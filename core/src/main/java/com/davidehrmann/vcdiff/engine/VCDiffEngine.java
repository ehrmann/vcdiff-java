// Copyright 2006-2016 Google Inc., David Ehrmann
// Authors: Sanjay Ghemawat, Jeff Dean, Chandra Chereddi, Lincoln Smith, David Ehrmann
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.CodeTableWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * All methods in this class are thread-safe.
 */
class VCDiffEngine {

    /**
     * The minimum size of a string match that is worth putting into a COPY
     * instruction.  Since this value is more than twice the block size, the
     * encoder will always discover a match of this size, no matter whether it is
     * aligned on block boundaries in the dictionary text.
     */
    public static final int kMinimumMatchSize = 32;

    /**
     * A copy of the dictionary contents
     */
    protected final byte[] dictionary_;

    /**
     * A hash that contains one element for every kBlockSize bytes of dictionary_.
     * This can be reused to encode many different target strings using the
     * same dictionary, without the need to compute the hash values each time.
     */
    protected final BlockHash hashed_dictionary_;

    public VCDiffEngine(byte[] dictionary) {
        dictionary_ = dictionary;
        hashed_dictionary_ = BlockHash.CreateDictionaryHash(dictionary_);
    }

    public int dictionary_size() {
        return dictionary_.length;
    }

    /**
     * Main worker function.  Finds the best matches between the dictionary
     * (source) and target data, and uses the coder to write a
     * delta file window into diff.
     *
     * @param targetData data to encoder
     * @param lookForTargetMatches whether to look for matches within the previously encoded target data, or just
     *                             within the source (dictionary) data.
     * @param diff writer to write diff to
     * @param coder CodeTableWriter to write encoded data to
     * @throws IOException if there's an encoding exception or an exception while writing to diff
     */
    public <OUT> void Encode(ByteBuffer targetData, boolean lookForTargetMatches, OUT diff, CodeTableWriter<OUT> coder) throws IOException {
        if (!targetData.hasRemaining()) {
            return;  // Do nothing for empty target
        }

        // Special case for really small input
        if (targetData.remaining() < BlockHash.kBlockSize) {
            int target_size = targetData.remaining();
            AddUnmatchedRemainder(targetData, coder);
            coder.output(diff);
            return;
        }

        final ByteBuffer local_target_data = targetData.slice();

        RollingHash hasher = new RollingHash(BlockHash.kBlockSize);
        final BlockHash target_hash;
        if (lookForTargetMatches) {
            target_hash = BlockHash.CreateTargetHash(local_target_data.slice(), dictionary_size());
        } else {
            target_hash = null;
        }

        //final int initial_position = local_target_data.position();

        final ByteBuffer candidate_pos = local_target_data.slice();

        int hash_value = (int)hasher.Hash(candidate_pos.array(), candidate_pos.arrayOffset() + candidate_pos.position(), candidate_pos.remaining());
        while (true) {
            if (EncodeCopyForBestMatch(lookForTargetMatches, hash_value, candidate_pos, local_target_data, target_hash, coder)) {
                candidate_pos.position(local_target_data.position());
                if (candidate_pos.remaining() < BlockHash.kBlockSize) {
                    break;  // Reached end of target data
                }
                // candidate_pos has jumped ahead by bytes_encoded bytes, so UpdateHash
                // can't be used to calculate the hash value at its new position.
                hash_value = (int)hasher.Hash(candidate_pos.array(),
                        candidate_pos.arrayOffset() + candidate_pos.position(),
                        candidate_pos.remaining());
                if (lookForTargetMatches) {
                    // Update the target hash for the ADDed and COPYed data
                    target_hash.AddAllBlocksThroughIndex(candidate_pos.position());
                }
            } else {
                // No match, or match is too small to be worth a COPY instruction.
                // Move to the next position in the target data.
                if (candidate_pos.remaining() - 1 < BlockHash.kBlockSize) {
                    break;  // Reached end of target data
                }

                if (lookForTargetMatches) {
                    target_hash.AddOneIndexHash(candidate_pos.position(), hash_value);
                }

                byte new_last_byte = candidate_pos.get(candidate_pos.position() + BlockHash.kBlockSize);
                byte old_first_byte = candidate_pos.get();

                hash_value = (int)hasher.UpdateHash(hash_value, old_first_byte, new_last_byte);
            }
        }

        AddUnmatchedRemainder(local_target_data, coder);
        coder.output(diff);

        targetData.position(targetData.position() + local_target_data.position());
    }

    protected static boolean ShouldGenerateCopyInstructionForMatchOfSize(int size) {
        return size >= kMinimumMatchSize;
    }

    /**
     * Once the encoder loop has finished checking for matches in the target data,
     * this function creates an ADD instruction to encode all target bytes
     * from the end of the last COPY match, if any, through the end of
     * the target data.  In the worst case, if no matches were found at all,
     * this function will create one big ADD instruction
     * for the entire buffer of target data.
     */
    protected void AddUnmatchedRemainder(ByteBuffer unencoded_target, CodeTableWriter<?> coder) {
        if (unencoded_target.hasRemaining()) {
            coder.add(unencoded_target.array(),
                    unencoded_target.arrayOffset() + unencoded_target.position(),
                    unencoded_target.remaining());

            unencoded_target.position(unencoded_target.limit());
        }
    }

    /**
     * This helper function tries to find an appropriate match within
     * hashed_dictionary_ for the block starting at the current target position.
     * If target_hash is not NULL, this function will also look for a match
     * within the previously encoded target data.
     *
     * If a match is found, this function will generate an ADD instruction
     * for all unencoded data that precedes the match,
     * and a COPY instruction for the match itself; then it returns
     * the number of bytes processed by both instructions,
     * which is guaranteed to be &gt; 0.
     * If no appropriate match is found, the function returns 0.
     *
     * The first four parameters are input parameters which are passed
     * directly to BlockHash::FindBestMatch; please see that function
     * for a description of their allowable values.
     */
    protected boolean EncodeCopyForBestMatch(boolean look_for_target_matches, int hash_value,
            ByteBuffer target_candidate, ByteBuffer unencoded_target,
            BlockHash target_hash, CodeTableWriter<?> coder) {

        // When FindBestMatch() comes up with a match for a candidate block,
        // it will populate best_match with the size, source offset,
        // and target offset of the match.
        BlockHash.Match best_match = new BlockHash.Match();

        ByteBuffer target = unencoded_target.slice();
        target.position((target_candidate.arrayOffset() + target_candidate.position()) -
                (unencoded_target.arrayOffset() + unencoded_target.position()));

        // First look for a match in the dictionary.
        hashed_dictionary_.FindBestMatch(hash_value, target, best_match);

        // If target matching is enabled, then see if there is a better match
        // within the target data that has been encoded so far.
        if (look_for_target_matches) {
            target_hash.FindBestMatch(hash_value, target, best_match);
        }

        if (!ShouldGenerateCopyInstructionForMatchOfSize(best_match.size())) {
            return false;
        }

        if (best_match.target_offset() > 0) {
            // Create an ADD instruction to encode all target bytes
            // from the end of the last COPY match, if any, up to
            // the beginning of this COPY match.
            coder.add(unencoded_target.array(),
                    unencoded_target.arrayOffset() + unencoded_target.position(),
                    best_match.target_offset());
        }

        coder.copy(best_match.source_offset(), best_match.size());
        unencoded_target.position(unencoded_target.position() + best_match.target_offset() + best_match.size());
        return best_match.target_offset() + best_match.size() > 0;
    }
}
