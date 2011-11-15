package com.googlecode.jvcdiff;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.googlecode.jvcdiff.BlockHash.Match;

/**
 * All methods in this class are thread-safe.
 */
public class VCDiffEngine {

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
		dictionary_ = Arrays.copyOf(dictionary, dictionary.length);
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
	 * look_for_target_matches determines whether to look for matches
	 * within the previously encoded target data, or just within the source
	 * (dictionary) data.
	 * 
	 * @throws IOException 
	 */
	public <OUT> void Encode(ByteBuffer target_data, boolean look_for_target_matches, OUT diff, CodeTableWriterInterface<OUT> coder) throws IOException {
		if (!target_data.hasRemaining()) {
			return;  // Do nothing for empty target
		}

		// Special case for really small input
		if (target_data.remaining() < BlockHash.kBlockSize) {
			int target_size = target_data.remaining();
			AddUnmatchedRemainder(target_data, coder);
			FinishEncoding(target_size, diff, coder);
			return;
		}
		
		final ByteBuffer local_target_data = target_data.slice();
		
		RollingHash hasher = new RollingHash(BlockHash.kBlockSize);
		final BlockHash target_hash;
		if (look_for_target_matches) {
			target_hash = BlockHash.CreateTargetHash(local_target_data.slice(), dictionary_size());
		} else {
			target_hash = null;
		}
		
		//final int initial_position = local_target_data.position();
		
		final ByteBuffer candidate_pos = local_target_data.slice();
		
		int hash_value = (int)hasher.Hash(candidate_pos.array(), candidate_pos.arrayOffset() + candidate_pos.position(), candidate_pos.remaining());
		while (true) {
			System.out.printf("hash_value = %d\n", hash_value);
			System.out.printf("candidate_pos.remaining() = %d\n", candidate_pos.remaining());
			if (EncodeCopyForBestMatch(look_for_target_matches, hash_value, candidate_pos, local_target_data, target_hash, coder)) {
				candidate_pos.position(local_target_data.position());
				if (candidate_pos.remaining() < BlockHash.kBlockSize) {
					break;  // Reached end of target data
				}
				// candidate_pos has jumped ahead by bytes_encoded bytes, so UpdateHash
				// can't be used to calculate the hash value at its new position.
				hash_value = (int)hasher.Hash(candidate_pos.array(),
						candidate_pos.arrayOffset() + candidate_pos.position(),
						candidate_pos.remaining());
				if (look_for_target_matches) {
					// Update the target hash for the ADDed and COPYed data
					target_hash.AddAllBlocksThroughIndex(candidate_pos.position());
				}
			} else {
				// No match, or match is too small to be worth a COPY instruction.
				// Move to the next position in the target data.
				if (candidate_pos.remaining() - 1 < BlockHash.kBlockSize) {
					break;  // Reached end of target data
				}
				
				if (look_for_target_matches) {
					target_hash.AddOneIndexHash(candidate_pos.position(), hash_value);
				}
				
				byte new_last_byte = candidate_pos.get(candidate_pos.position() + BlockHash.kBlockSize);
				byte old_first_byte = candidate_pos.get();
				
				hash_value = (int)hasher.UpdateHash(hash_value, old_first_byte, new_last_byte);
			}
		}
		
		AddUnmatchedRemainder(local_target_data, coder);
		FinishEncoding(local_target_data.remaining(), diff, coder);
		
		target_data.position(target_data.position() + local_target_data.position());
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
	protected void AddUnmatchedRemainder(ByteBuffer unencoded_target, CodeTableWriterInterface<?> coder) {
		if (unencoded_target.hasRemaining()) {
			coder.Add(unencoded_target.array(),
					unencoded_target.arrayOffset() + unencoded_target.position(), 
					unencoded_target.remaining());

			unencoded_target.position(unencoded_target.limit());
		}
	}

	/**
	 * This helper function tells the coder to finish the encoding and write
	 * the results into the output string "diff".
	 * @throws IOException 
	 */
	protected <OUT> void FinishEncoding(int target_size, OUT diff, CodeTableWriterInterface<OUT> coder) throws IOException {
		if (target_size != coder.target_length()) {
			String.format("Internal error in VCDiffEngine::Encode: original target size (%d) does not match number of bytes processed (%d)",
					target_size, coder.target_length());
		}
		coder.Output(diff);
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
	 * which is guaranteed to be > 0.
	 * If no appropriate match is found, the function returns 0.
	 *
	 * The first four parameters are input parameters which are passed
	 * directly to BlockHash::FindBestMatch; please see that function
	 * for a description of their allowable values.
	 * 
	 * @param look_for_target_matches
	 * @param hash_value
	 * @param target_candidate
	 * @param unencoded_target
	 * @param target_hash
	 * @param coder
	 * @return
	 */
	protected boolean EncodeCopyForBestMatch(boolean look_for_target_matches, int hash_value,
			ByteBuffer target_candidate, ByteBuffer unencoded_target,
			BlockHash target_hash, CodeTableWriterInterface<?> coder) {

		// When FindBestMatch() comes up with a match for a candidate block,
		// it will populate best_match with the size, source offset,
		// and target offset of the match.
		BlockHash.Match best_match = new Match();

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
			coder.Add(unencoded_target.array(),
					unencoded_target.arrayOffset() + unencoded_target.position(),
					best_match.target_offset());
		}

		coder.Copy(best_match.source_offset(), best_match.size());
		unencoded_target.position(unencoded_target.position() + best_match.target_offset() + best_match.size());
		return best_match.target_offset() + best_match.size() > 0;
	}
}
