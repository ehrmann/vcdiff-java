// Copyright 2006 Google Inc., David Ehrmann
// Authors: Sanjay Ghemawat, Jeff Dean, Chandra Chereddi, Lincoln Smith, David Ehrmann
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Implementation of the Bentley/McIlroy algorithm for finding differences.
// Bentley, McIlroy.  DCC 1999.  Data Compression Using Long Common Strings.
// http://citeseer.ist.psu.edu/555557.html

package com.googlecode.jvcdiff;

import java.nio.ByteBuffer;
import java.util.Arrays;

// A generic hash table which will be used to keep track of byte runs
// of size kBlockSize in both the incrementally processed target data
// and the preprocessed source dictionary.
//
// A custom hash table implementation is used instead of the standard
// hash_map template because we know that there will be exactly one
// entry in the BlockHash corresponding to each kBlockSize bytes
// in the source data, which makes certain optimizations possible:
// * The memory for the hash table and for all hash entries can be allocated
//   in one step rather than incrementally for each insert operation.
// * A single integer can be used to represent both
//   the index of the next hash entry in the chain
//   and the position of the entry within the source data
//   (== kBlockSize * block_number).  This greatly reduces the size
//   of a hash entry.
public class BlockHash {

	// Block size as per Bentley/McIlroy; must be a power of two.
	//
	// Using (for example) kBlockSize = 4 guarantees that no match smaller
	// than size 4 will be identified, that some matches having sizes
	// 4, 5, or 6 may be identified, and that all matches
	// having size 7 or greater will be identified (because any string of
	// 7 bytes must contain a complete aligned block of 4 bytes.)
	//
	// Increasing kBlockSize by a factor of two will halve the amount of
	// memory needed for the next block table, and will halve the setup time
	// for a new BlockHash.  However, it also doubles the minimum
	// match length that is guaranteed to be found in FindBestMatch(),
	// so that function will be less effective in finding matches.
	//
	// Computational effort in FindBestMatch (which is the inner loop of
	// the encoding algorithm) will be proportional to the number of
	// matches found, and a low value of kBlockSize will waste time
	// tracking down small matches.  On the other hand, if this value
	// is set too high, no matches will be found at all.
	//
	// It is suggested that different values of kBlockSize be tried against
	// a representative data set to find the best tradeoff between
	// memory/CPU and the effectiveness of FindBestMatch().
	//
	// If you change kBlockSize to a smaller value, please increase
	// kMaxMatchesToCheck accordingly.
	public static final int kBlockSize = 16;

	// FindBestMatch() will not process more than this number
	// of matching hash entries.
	//
	// It is necessary to have a limit on the maximum number of matches
	// that will be checked in order to avoid the worst-case performance
	// possible if, for example, all the blocks in the dictionary have
	// the same hash value.  See the unit test SearchStringFindsTooManyMatches
	// for an example of such a case.  The encoder uses a loop in
	// VCDiffEngine::Encode over each target byte, containing a loop in
	// BlockHash::FindBestMatch over the number of matches (up to a maximum
	// of the number of source blocks), containing two loops that extend
	// the match forwards and backwards up to the number of source bytes.
	// Total complexity in the worst case is
	//     O([target size] * source_size_ * source_size_)
	// Placing a limit on the possible number of matches checked changes this to
	//     O([target size] * source_size_ * kMaxMatchesToCheck)
	//
	// In empirical testing on real HTML text, using a block size of 4,
	// the number of true matches per call to FindBestMatch() did not exceed 78;
	// with a block size of 32, the number of matches did not exceed 3.
	//
	// The expected number of true matches scales super-linearly
	// with the inverse of kBlockSize, but here a linear scale is used
	// for block sizes smaller than 32.
	
	// kMaxMatchesToCheck = (kBlockSize >= 32) ? 32 : (32 * (32 / kBlockSize));
	protected static final int kMaxMatchesToCheck = 32 * (32 / kBlockSize);

	// Do not skip more than this number of non-matching hash collisions
	// to find the next matching entry in the hash chain.
	protected static final int kMaxProbes = 16;

	protected static final RollingHash rollingHash = new RollingHash(kBlockSize);
	
	private final ByteBuffer source_data;

	// The size of this array is determined using CalcTableSize().  It has at
	// least one element for each kBlockSize-byte block in the source data.
	// GetHashTableIndex() returns an index into this table for a given hash
	// value.  The value of each element of hash_table_ is the lowest block
	// number in the source data whose hash value would return the same value from
	// GetHashTableIndex(), or -1 if there is no matching block.  This value can
	// then be used as an index into next_block_table_ to retrieve the entire set
	// of matching block numbers.
	private final int[] hash_table;

	// An array containing one element for each source block.  Each element is
	// either -1 (== not found) or the index of the next block whose hash value
	// would produce a matching result from GetHashTableIndex().
	private final int[] next_block_table;

	// This vector has the same size as next_block_table_.  For every block number
	// B that is referenced in hash_table_, last_block_table_[B] will contain
	// the maximum block number that has the same GetHashTableIndex() value
	// as block B.  This number may be B itself.  For a block number B' that
	// is not referenced in hash_table_, the value of last_block_table_[B'] is -1.
	// This table is used only while populating the hash table, not while looking
	// up hash values in the table.  Keeping track of the last block number in the
	// chain allows us to construct the block chains as FIFO rather than LIFO
	// lists, so that the match with the lowest index is returned first.  This
	// should result in a more compact encoding because the VCDIFF format favors
	// smaller index values and repeated index values.
	private final int[] last_block_table;

	// Performing a bitwise AND with hash_table_mask_ will produce a value ranging
	// from 0 to the number of elements in hash_table_.
	private final int hash_table_mask;

	// The offset of the first byte of source data (the data at source_data_[0]).
	// For the purpose of computing offsets, the source data and target data
	// are considered to be concatenated -- not literally in a single memory
	// buffer, but conceptually as described in the RFC.
	// The first byte of the previously encoded target data
	// has an offset that is equal to dictionary_size, i.e., just after
	// the last byte of source data.
	// For a hash of source (dictionary) data, starting_offset_ will be zero;
	// for a hash of previously encoded target data, starting_offset_ will be
	// equal to the dictionary size.
	private final int starting_offset;

	// The last index added by AddBlock().  This determines the block number
	// for successive calls to AddBlock(), and is also
	// used to determine the starting block for AddAllBlocksThroughIndex().
	private int last_block_added = -1;
	
	// This class is used to store the best match found by FindBestMatch()
	// and return it to the caller.
	public static class Match {
		// The size of the best (longest) match passed to ReplaceIfBetterMatch().
		private int size = 0;

		// The source offset of the match, including the starting_offset_
		// of the BlockHash for which the match was found.
		private int source_offset = -1;

		// The target offset of the match.  An offset of 0 corresponds to the
		// data at target_start, which is an argument of FindBestMatch().
		private int target_offset = -1;

		public Match() {
		}


		public void ReplaceIfBetterMatch(int candidate_size,
				int candidate_source_offset,
				int candidate_target_offset) {
			if (candidate_size > size) {
				size = candidate_size;
				source_offset = candidate_source_offset;
				target_offset = candidate_target_offset;
			}
		}

		public int size() { return size; }
		public int source_offset() { return source_offset; }
		public int target_offset() { return target_offset; }
	}

	// A BlockHash is created using a buffer of source data.  The hash table
	// will contain one entry for each kBlockSize-byte block in the
	// source data.
	//
	// See the comments for starting_offset_, below, for a description of
	// the starting_offset argument.  For a hash of source (dictionary) data,
	// starting_offset_ will be zero; for a hash of previously encoded
	// target data, starting_offset_ will be equal to the dictionary size.
	public BlockHash(byte[] source_data, int starting_offset, boolean populate_hash_table) {
		this(ByteBuffer.wrap(source_data), starting_offset, populate_hash_table);
	}
	
	public BlockHash(ByteBuffer source_data, int starting_offset, boolean populate_hash_table) {
		final int table_size = CalcTableSize(source_data.remaining());
		if (table_size == 0) {
			throw new IllegalArgumentException("Error finding table size for source size " + source_data.remaining());
		}
		
		this.source_data = source_data;
		this.starting_offset = starting_offset;
		
		// Since table_size is a power of 2, (table_size - 1) is a bit mask
		// containing all the bits below table_size.
		hash_table_mask = table_size - 1;
		hash_table = new int[table_size];
		Arrays.fill(hash_table, -1);

		next_block_table = new int[GetNumberOfBlocks()];
		last_block_table = new int[GetNumberOfBlocks()];

		Arrays.fill(next_block_table, -1);
		Arrays.fill(last_block_table, -1);

		if (populate_hash_table) {
			AddAllBlocks();
		}
	}

	// In the context of the open-vcdiff encoder, BlockHash is used for two
	// purposes: to hash the source (dictionary) data, and to hash
	// the previously encoded target data.  The main differences between
	// a dictionary BlockHash and a target BlockHash are as follows:
	//
	//   1. The best_match->source_offset() returned from FindBestMatch()
	//      for a target BlockHash is computed in the following manner:
	//      the starting offset of the first byte in the target data
	//      is equal to the dictionary size.  FindBestMatch() will add
	//      starting_offset_ to any best_match->source_offset() value it returns,
	//      in order to produce the correct offset value for a target BlockHash.
	//   2. For a dictionary BlockHash, the entire data set is hashed at once
	//      when Init() is called with the parameter populate_hash_table = true.
	//      For a target BlockHash, because the previously encoded target data
	//      includes only the data seen up to the current encoding position,
	//      the data blocks are hashed incrementally as the encoding position
	//      advances, using AddOneIndexHash() and AddAllBlocksThroughIndex().
	//
	// The following two factory functions can be used to create BlockHash
	// objects for each of these two purposes.  Each factory function calls
	// the object constructor and also calls Init().  If an error occurs,
	// NULL is returned; otherwise a valid BlockHash object is returned.
	// Since a dictionary BlockHash is not expected to be modified after
	// initialization, a const object is returned.
	// The caller is responsible for deleting the returned object
	// (using the C++ delete operator) once it is no longer needed.
	public static BlockHash CreateDictionaryHash(byte[] dictionary_data) {
		return new BlockHash(dictionary_data, 0, true);
	}

	public static BlockHash CreateTargetHash(byte[] target_data, int dictionary_size) {
		return new BlockHash(target_data, dictionary_size, false);
	}
	
	public static BlockHash CreateTargetHash(ByteBuffer target_data, int dictionary_size) {
		return new BlockHash(target_data, dictionary_size, false);
	}

	// This function will be called to add blocks incrementally to the target hash
	// as the encoding position advances through the target data.  It will be
	// called for every kBlockSize-byte block in the target data, regardless
	// of whether the block is aligned evenly on a block boundary.  The
	// BlockHash will only store hash entries for the evenly-aligned blocks.
	public void AddOneIndexHash(int index, int hash_value) {
		if (index == NextIndexToAdd()) {
			AddBlock(hash_value);
		}
	}

	// Calls AddBlock() for each kBlockSize-byte block in the range
	// (last_block_added_ * kBlockSize, end_index), exclusive of the endpoints.
	// If end_index <= the last index added (last_block_added_ * kBlockSize),
	// this function does nothing.
	//
	// A partial block beginning anywhere up to (end_index - 1) is also added,
	// unless it extends outside the end of the source data.  Like AddAllBlocks(),
	// this function computes the hash value for each of the blocks in question
	// from scratch, so it is not a good option if the hash values have already
	// been computed for some other purpose.
	//
	// Example: assume kBlockSize = 4, last_block_added_ = 1, and there are
	// 14 bytes of source data.
	// If AddAllBlocksThroughIndex(9) is invoked, then it will call AddBlock()
	// only for block number 2 (at index 8).
	// If, after that, AddAllBlocksThroughIndex(14) is invoked, it will not call
	// AddBlock() at all, because block 3 (beginning at index 12) would
	// fall outside the range of source data.
	//
	// VCDiffEngine::Encode (in vcdiffengine.cc) uses this function to
	// add a whole range of data to a target hash when a COPY instruction
	// is generated.
	public void AddAllBlocksThroughIndex(int end_index) {
		if (end_index > source_data.limit()) {
			throw new IllegalArgumentException("AddAllBlocksThroughIndex() called with index " + end_index + " higher than end index " + source_data.limit());
		}
		final int last_index_added = last_block_added * kBlockSize;
		if (end_index <= last_index_added) {
			throw new IllegalArgumentException("AddAllBlocksThroughIndex() called with index " + end_index + " <= last index added ( " + last_index_added + ")");
		}
		int end_limit = end_index;
		// Don't allow reading any indices at or past source_size_.
		// The Hash function extends (kBlockSize - 1) bytes past the index,
		// so leave a margin of that size.
		int last_legal_hash_index = source_data.limit() - kBlockSize;
		if (end_limit > last_legal_hash_index) {
			end_limit = last_legal_hash_index + 1;
		}

		ByteBuffer temp = source_data.duplicate();
		temp.position(NextIndexToAdd());
		// temp.limit(end_limit);

		while (temp.position() < end_limit) {
			AddBlock((int)rollingHash.Hash(temp));
		}
	}

	// FindBestMatch takes a position within the unencoded target data
	// (target_candidate_start) and the hash value of the kBlockSize bytes
	// beginning at that position (hash_value).  It attempts to find a matching
	// set of bytes within the source (== dictionary) data, expanding
	// the match both below and above the target block.  It cannot expand
	// the match outside the bounds of the source data, or below
	// target_start within the target data, or past
	// the end limit of (target_start + target_length).
	//
	// target_candidate_start is the start of the candidate block within the
	// target data for which a match will be sought, while
	// target_start (which is <= target_candidate_start)
	// is the start of the target data that has yet to be encoded.
	//
	// If a match is found whose size is greater than the size
	// of best_match, this function populates *best_match with the
	// size, source_offset, and target_offset of the match found.
	// best_match->source_offset() will contain the index of the start of the
	// matching source data, plus starting_offset_
	// (see description of starting_offset_ for details);
	// best_match->target_offset() will contain the offset of the match
	// beginning with target_start = offset 0, such that
	//     0 <= best_match->target_offset()
	//              <= (target_candidate_start - target_start);
	// and best_match->size() will contain the size of the match.
	// If no such match is found, this function leaves *best_match unmodified.
	//
	// On calling FindBestMatch(), best_match must
	// point to a valid Match object, and cannot be NULL.
	// The same Match object can be passed
	// when calling FindBestMatch() on a different BlockHash object
	// for the same candidate data block, in order to find
	// the best match possible across both objects.  For example:
	//
	//     open_vcdiff::BlockHash::Match best_match;
	//     uint32_t hash_value =
	//         RollingHash<BlockHash::kBlockSize>::Hash(target_candidate_start);
	//     bh1.FindBestMatch(hash_value,
	//                       target_candidate_start,
	//                       target_start,
	//                       target_length,
	//                       &best_match);
	//     bh2.FindBestMatch(hash_value,
	//                       target_candidate_start,
	//                       target_start,
	//                       target_length,
	//                       &best_match);
	//     if (best_size >= 0) {
	//       // a match was found; its size, source offset, and target offset
	//       // can be found in best_match
	//     }
	//
	// hash_value is passed as a separate parameter from target_candidate_start,
	// (rather than calculated within FindBestMatch) in order to take
	// advantage of the rolling hash, which quickly calculates the hash value
	// of the block starting at target_candidate_start based on
	// the known hash value of the block starting at (target_candidate_start - 1).
	// See vcdiffengine.cc for more details.
	//
	// Example:
	//    kBlockSize: 4
	//    target text: "ANDREW LLOYD WEBBER"
	//                 1^    5^2^         3^
	//    dictionary: "INSURANCE : LLOYDS OF LONDON"
	//                           4^
	//    hashed dictionary blocks:
	//        "INSU", "RANC", "E : ", "LLOY", "DS O", "F LON"
	//
	//    1: target_start (beginning of unencoded data)
	//    2: target_candidate_start (for the block "LLOY")
	//    3: target_length (points one byte beyond the last byte of data.)
	//    4: best_match->source_offset() (after calling FindBestMatch)
	//    5: best_match->target_offset() (after calling FindBestMatch)
	//
	//    Under these conditions, FindBestMatch will find a matching
	//    hashed dictionary block for "LLOY", and will extend the beginning of
	//    this match backwards by one byte, and the end of the match forwards
	//    by one byte, finding that the best match is " LLOYD"
	//    with best_match->source_offset() = 10
	//                                  (offset of " LLOYD" in the source string),
	//         best_match->target_offset() = 6
	//                                  (offset of " LLOYD" in the target string),
	//     and best_match->size() = 6.

	public void FindBestMatch(int hash_value, ByteBuffer target, Match best_match) {
		// Keep a count of the number of matches found.  This will throttle the
		// number of iterations in FindBestMatch.  For example, if the entire
		// dictionary is made up of spaces (' ') and the search string is also
		// made up of spaces, there will be one match for each block in the
		// dictionary.
		int match_counter = 0;
		// TODO
		for (int block_number = FirstMatchingBlock(hash_value, target.array(), target.arrayOffset() + target.position());
		(block_number >= 0) && !(++match_counter > kMaxMatchesToCheck);
		block_number = NextMatchingBlock(block_number, target.array(), target.arrayOffset() + target.position())) {
			int source_match_offset = block_number * kBlockSize;
			final int source_match_end = source_match_offset + kBlockSize;

			int target_match_offset = target.position();
			final int target_match_end = target_match_offset + kBlockSize;

			int match_size = kBlockSize;
			{
				// Extend match start towards beginning of unencoded data
				final int limit_bytes_to_left = Math.min(source_match_offset, target_match_offset);
				final int matching_bytes_to_left =
					MatchingBytesToLeft(
							source_data, source_match_offset,
							target.array(), target.arrayOffset() + target_match_offset,
							limit_bytes_to_left);
				source_match_offset -= matching_bytes_to_left;
				target_match_offset -= matching_bytes_to_left;
				match_size += matching_bytes_to_left;
			}
			{
				// Extend match end towards end of unencoded data
				final int source_bytes_to_right = source_data.limit() - source_match_end;
				final int target_bytes_to_right = target.limit() - target_match_end;
				final int limit_bytes_to_right = Math.min(source_bytes_to_right, target_bytes_to_right);
				match_size +=
					MatchingBytesToRight(
							source_data, source_match_end,
							target.array(), target.arrayOffset() + target_match_end,
							limit_bytes_to_right);
			}
			// Update in/out parameter if the best match found was better
			// than any match already stored in *best_match.
			best_match.ReplaceIfBetterMatch(match_size, source_match_offset + starting_offset, target_match_offset);
		}
	}
	
	public void FindBestMatch(int hash_value, byte[] target_candidate, int target_candidate_start, byte[] target, int target_start, Match best_match) {
		if (target_candidate != target) {
			throw new IllegalArgumentException("target_candidate != target");
		}
		if (target_candidate_start < target_start) {
			throw new IllegalArgumentException("target_candidate_start < target_start");
		}
		
		ByteBuffer targetBuffer = ByteBuffer.wrap(target, target_start, target.length - target_start);
		targetBuffer.position(target_candidate_start);
		
		FindBestMatch(hash_value, targetBuffer, best_match);
	}

	// Internal routine which calculates a hash table size based on kBlockSize and
	// the dictionary_size.  Will return a power of two if successful, or 0 if an
	// internal error occurs.  Some calculations (such as GetHashTableIndex())
	// depend on the table size being a power of two.
	protected static int CalcTableSize(final int dictionary_size) {
		// Overallocate the hash table by making it the same size (in bytes)
		// as the source data.  This is a trade-off between space and time:
		// the empty entries in the hash table will reduce the
		// probability of a hash collision to (sizeof(int) / kblockSize),
		// and so save time comparing false matches.
		final int min_size = (dictionary_size / 4) + 1;
		int table_size = 1;
		// Find the smallest power of 2 that is >= min_size, and assign
		// that value to table_size.
		while (table_size < min_size) {
			table_size <<= 1;
			// Guard against an infinite loop
			if (table_size <= 0) {
				String.format("Internal error: CalcTableSize(dictionary_size = %d): resulting table_size %d is zero or negative", dictionary_size, table_size);
				return 0;
			}
		}
		// Check size sanity
		if ((table_size & (table_size - 1)) != 0) {
			String.format("Internal error: CalcTableSize(dictionary_size = %d): resulting table_size %d is not a power of 2", dictionary_size, table_size);
			return 0;
		}
		// The loop above tries to find the smallest power of 2 that is >= min_size.
		// That value must lie somewhere between min_size and (min_size * 2),
		// except for the case (dictionary_size == 0, table_size == 1).
		if ((dictionary_size > 0) && (table_size > (min_size * 2))) {
			String.format("Internal error: CalcTableSize(dictionary_size = %d): resulting table_size %d is too large", dictionary_size, table_size);
			return 0;
		}
		return table_size;
	}

	protected int GetNumberOfBlocks() {
		return source_data.limit() / kBlockSize;
	}

	// Use the lowest-order bits of the hash value
	// as the index into the hash table.
	protected int GetHashTableIndex(int hash_value) {
		return hash_value & hash_table_mask;
	}

	// The index within source_data_ of the next block
	// for which AddBlock() should be called.
	protected int NextIndexToAdd() {
		return (last_block_added + 1) * kBlockSize;
	}

	// Adds an entry to the hash table for one block of source data of length
	// kBlockSize, starting at source_data_[block_number * kBlockSize],
	// where block_number is always (last_block_added_ + 1).  That is,
	// AddBlock() must be called once for each block in source_data_
	// in increasing order.
	protected void AddBlock(int hash_value) {
		// The initial value of last_block_added_ is -1.
		int block_number = last_block_added + 1;
		final int total_blocks = (source_data.limit() / kBlockSize);  // round down
		if (block_number >= total_blocks) {
			String.format("BlockHash.AddBlock() called with block number %d this is past last block %d", block_number, total_blocks - 1);
			return;
		}
		if (next_block_table[block_number] != -1) {
			String.format("Internal error in BlockHash::AddBlock(): block number = %d, next block should be -1 but is %d", block_number, next_block_table[block_number]);
			return;
		}
		final int hash_table_index = GetHashTableIndex(hash_value);
		final int first_matching_block = hash_table[hash_table_index];
		if (first_matching_block < 0) {
			// This is the first entry with this hash value
			hash_table[hash_table_index] = block_number;
			last_block_table[block_number] = block_number;
		} else {
			// Add this entry at the end of the chain of matching blocks
			final int last_matching_block = last_block_table[first_matching_block];
			if (next_block_table[last_matching_block] != -1) {
				String.format("Internal error in BlockHash::AddBlock(): first matching block = %d, last matching block = %d, next block should be -1 but is %d", first_matching_block, last_matching_block, next_block_table[last_matching_block]);
				return;
			}
			next_block_table[last_matching_block] = block_number;
			last_block_table[first_matching_block] = block_number;
		}
		last_block_added = block_number;
	}

	// Calls AddBlock() for each complete kBlockSize-byte block between
	// source_data_ and (source_data_ + source_size_).  It is equivalent
	// to calling AddAllBlocksThroughIndex(source_data + source_size).
	// This function is called when Init(true) is invoked.
	protected void AddAllBlocks() {
		AddAllBlocksThroughIndex(source_data.limit());
	}

	// Returns true if the contents of the kBlockSize-byte block
	// beginning at block1 are identical to the contents of
	// the block beginning at block2; false otherwise.
	protected static boolean BlockContentsMatch(byte[] block1, int block1_ofset, ByteBuffer block2, int block2_offset) {
		for (int i = 0; i < kBlockSize; i++) {
			if (block1[block1_ofset + i] != block2.get(block2_offset + i)) {
				return false;
			}
		}

		return true;
	}
	
	protected static boolean BlockContentsMatch(byte[] block1, int block1_ofset, byte[] block2, int block2_offset) {
		for (int i = 0; i < kBlockSize; i++) {
			if (block1[block1_ofset + i] != block2[block2_offset + i]) {
				return false;
			}
		}

		return true;
	}

	// Finds the first block number within the hashed data
	// that represents a match for the given hash value.
	// Returns -1 if no match was found.
	//
	// Init() must have been called and returned true before using
	// FirstMatchingBlock or NextMatchingBlock.  No check is performed
	// for this condition; the code will crash if this condition is violated.
	//
	// The hash table is initially populated with -1 (not found) values,
	// so if this function is called before the hash table has been populated
	// using AddAllBlocks() or AddBlock(), it will simply return -1
	// for any value of hash_value.
	protected int FirstMatchingBlock(int hash_value, byte[] block_ptr, int offset) {
		return SkipNonMatchingBlocks(hash_table[GetHashTableIndex(hash_value)], block_ptr, offset);
	}

	// Given a block number returned by FirstMatchingBlock()
	// or by a previous call to NextMatchingBlock(), returns
	// the next block number that matches the same hash value.
	// Returns -1 if no match was found.
	protected int NextMatchingBlock(int block_number, byte[] block_ptr, int offset) {
		if (block_number >= GetNumberOfBlocks()) {
			throw new IllegalArgumentException("NextMatchingBlock called for invalid block number " + block_number);
		}
		return SkipNonMatchingBlocks(next_block_table[block_number], block_ptr, offset);
	}

	// Walk through the hash entry chain, skipping over any false matches
	// (for which the lowest bits of the fingerprints match,
	// but the actual block data does not.)  Returns the block number of
	// the first true match found, or -1 if no true match was found.
	// If block_number is a matching block, the function will return block_number
	// without skipping to the next block.
	protected int SkipNonMatchingBlocks(int block_number, byte[] block_ptr, int offset) {
		int probes = 0;
		while (block_number >= 0 && !BlockContentsMatch(block_ptr, offset, source_data, block_number * kBlockSize)) {
			if (++probes > kMaxProbes) {
				return -1;  // Avoid too much chaining
			}
			block_number = next_block_table[block_number];
		}
		return block_number;
	}

	// Returns the number of bytes to the left of source_match_start
	// that match the corresponding bytes to the left of target_match_start.
	// Will not examine more than max_bytes bytes, which is to say that
	// the return value will be in the range [0, max_bytes] inclusive.
	protected static int MatchingBytesToLeft(ByteBuffer source_match_start, int source_match_offset, byte[] target_match_start, int target_match_start_offset, int max_bytes) {
		int bytes_found = 0;
		while (bytes_found < max_bytes) {
			--source_match_offset;
			--target_match_start_offset;

			if (source_match_start.get(source_match_offset) != target_match_start[target_match_start_offset]) {
				break;
			}
			++bytes_found;
		}
		return bytes_found;
	}
	
	protected static int MatchingBytesToLeft(byte[] source_match_start, int source_match_offset, byte[] target_match_start, int target_match_start_offset, int max_bytes) {
		int bytes_found = 0;
		while (bytes_found < max_bytes) {
			--source_match_offset;
			--target_match_start_offset;

			if (source_match_start[source_match_offset] != target_match_start[target_match_start_offset]) {
				break;
			}
			++bytes_found;
		}
		return bytes_found;
	}

	// Returns the number of bytes starting at source_match_end
	// that match the corresponding bytes starting at target_match_end.
	// Will not examine more than max_bytes bytes, which is to say that
	// the return value will be in the range [0, max_bytes] inclusive.
	protected static int MatchingBytesToRight(ByteBuffer source_match_end, int source_match_end_offset, byte[] target_match_end, int target_match_end_offset, int max_bytes) {
		int bytes_found = 0;
		while ((bytes_found < max_bytes) && (source_match_end.get(source_match_end_offset) == target_match_end[target_match_end_offset])) {
			++bytes_found;
			++source_match_end_offset;
			++target_match_end_offset;
		}
		return bytes_found;
	}
	
	protected static int MatchingBytesToRight(byte[] source_match_end, int source_match_end_offset, byte[] target_match_end, int target_match_end_offset, int max_bytes) {
		int bytes_found = 0;
		while ((bytes_found < max_bytes) && (source_match_end[source_match_end_offset] == target_match_end[target_match_end_offset])) {
			++bytes_found;
			++source_match_end_offset;
			++target_match_end_offset;
		}
		return bytes_found;
	}
}
