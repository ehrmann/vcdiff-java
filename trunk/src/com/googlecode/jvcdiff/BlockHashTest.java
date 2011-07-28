package com.googlecode.jvcdiff;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class BlockHashTest {
	private static final int kBlockSize = BlockHash.kBlockSize;

	private static final int kTimingTestSize = 1 << 21;  // 2M
	private static final int kTimingTestIterations = 32;

	// Each block in the sample text and search string is kBlockSize bytes long,
	// and consists of (kBlockSize - 1) space characters
	// followed by a single letter of text.

	// Block numbers of certain characters within the sample text:
	// All six occurrences of "e", in order.
	static final int block_of_first_e = 2;
	static final int block_of_second_e = 16;
	static final int block_of_third_e = 21;
	static final int block_of_fourth_e = 27;
	static final int block_of_fifth_e = 35;
	static final int block_of_sixth_e = 42;

	static final int block_of_y_in_only = 7;
	// The block number is multiplied by kBlockSize to arrive at the
	// index, which points to the (kBlockSize - 1) space characters before
	// the letter specified.
	// Indices of certain characters within the sample text.
	static final int index_of_first_e = block_of_first_e * kBlockSize;
	static final int index_of_fourth_e = block_of_fourth_e * kBlockSize;
	static final int index_of_sixth_e = block_of_sixth_e * kBlockSize;
	static final int index_of_y_in_only = block_of_y_in_only * kBlockSize;
	static final int index_of_space_before_fear_is_fear = 25 * kBlockSize;
	static final int index_of_longest_match_ear_is_fear = 27 * kBlockSize;
	static final int index_of_i_in_fear_is_fear = 31 * kBlockSize;
	static final int index_of_space_before_fear_itself = 33 * kBlockSize;
	static final int index_of_space_before_itself = 38 * kBlockSize;
	static final int index_of_ababc = 4 * kBlockSize;

	// Indices of certain characters within the search strings.
	static final int index_of_second_w_in_what_we = 5 * kBlockSize;
	static final int index_of_second_e_in_what_we_hear = 9 * kBlockSize;
	static final int index_of_f_in_fearsome = 16 * kBlockSize;
	static final int index_of_space_in_eat_itself =  12 * kBlockSize;
	static final int index_of_i_in_itself = 13 * kBlockSize;
	static final int index_of_t_in_use_the = 4 * kBlockSize;
	static final int index_of_o_in_online = 8 * kBlockSize;

	static final String sample_text_without_spaces = "The only thing we have to fear is fear itself";
	static final String search_string_without_spaces = "What we hear is fearsome";
	static final String search_string_altered_without_spaces = "Vhat ve hear is fearsomm";
	static final String search_to_end_without_spaces = "Pop will eat itself, eventually";
	static final String search_to_beginning_without_spaces = "Use The online dictionary";
	static final String sample_text_many_matches_without_spaces = "ababababcab";
	static final String search_string_many_matches_without_spaces = "ababc";

	static final byte[] sample_text = MakeEachLetterABlock(sample_text_without_spaces);
	static final byte[] search_string = MakeEachLetterABlock(search_string_without_spaces);
	static final byte[] search_string_altered = MakeEachLetterABlock(search_string_altered_without_spaces);
	static final byte[] search_to_end_string = MakeEachLetterABlock(search_to_end_without_spaces);
	static final byte[] search_to_beginning_string = MakeEachLetterABlock(search_to_beginning_without_spaces);
	static final byte[] sample_text_many_matches = MakeEachLetterABlock(sample_text_many_matches_without_spaces);
	static final byte[] search_string_many_matches = MakeEachLetterABlock(search_string_many_matches_without_spaces);

	static final byte[] test_string_y = MakeEachLetterABlock("y");
	static final byte[] test_string_e = MakeEachLetterABlock("e");
	static final byte[] test_string_all_Qs;
	static final byte[] test_string_unaligned_e;
	static {
		test_string_unaligned_e = new byte[kBlockSize];
		Arrays.fill(test_string_unaligned_e, (byte)' ');
		test_string_unaligned_e[kBlockSize - 2] = (byte)'e';

		test_string_all_Qs = new byte[kBlockSize];
		Arrays.fill(test_string_all_Qs, (byte)'Q');	
	}

	static long hashed_y = new RollingHash(kBlockSize).Hash(test_string_y, 0, test_string_y.length);
	static long hashed_e = new RollingHash(kBlockSize).Hash(test_string_e, 0, test_string_e.length);
	static long hashed_f = new RollingHash(kBlockSize).Hash(search_string, index_of_f_in_fearsome, search_string.length - index_of_f_in_fearsome);
	static long hashed_unaligned_e = new RollingHash(kBlockSize).Hash(test_string_unaligned_e, 0, test_string_unaligned_e.length);
	static long hashed_all_Qs = new RollingHash(kBlockSize).Hash(test_string_all_Qs, 0, test_string_all_Qs.length);

	// The two strings passed to BlockHash::MatchingBytesToLeft do have matching
	// characters -- in fact, they're the same string -- but since max_bytes is zero
	// or negative, BlockHash::MatchingBytesToLeft should not read from the strings
	// and should return 0.
	@Test
	public void MaxBytesZeroDoesNothing() {
		Assert.assertEquals(0, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				0));
		Assert.assertEquals(0, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				0));
	}

	@Test
	public void MaxBytesNegativeDoesNothing() {
		Assert.assertEquals(0, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				-1));
		Assert.assertEquals(0, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				Integer.MIN_VALUE));
		Assert.assertEquals(0, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				-1));
		Assert.assertEquals(0, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				Integer.MIN_VALUE));
	}

	@Test
	public void MaxBytesOneMatch() {
		Assert.assertEquals(1, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				1));
		Assert.assertEquals(1, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_f_in_fearsome,
				1));
	}

	@Test
	public void MaxBytesOneNoMatch() {
		Assert.assertEquals(0, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_second_e_in_what_we_hear,
				1));
		Assert.assertEquals(0, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string, index_of_second_e_in_what_we_hear - 1,
				1));
	}

	@Test
	public void LeftLimitedByMaxBytes() throws UnsupportedEncodingException {
		// The number of bytes that match between the original "we hear is fearsome"
		// and the altered "ve hear is fearsome".
		final int expected_length = kBlockSize * "e hear is ".getBytes("US-ASCII").length;
		final int max_bytes = expected_length - 1;
		Assert.assertEquals(max_bytes, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string_altered, index_of_f_in_fearsome,
				max_bytes));
	}

	@Test
	public void LeftNotLimited() throws UnsupportedEncodingException {
		// The number of bytes that match between the original "we hear is fearsome"
		// and the altered "ve hear is fearsome".
		final int expected_length = kBlockSize * "e hear is ".getBytes("US-ASCII").length;
		final int max_bytes = expected_length + 1;
		Assert.assertEquals(expected_length, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string_altered, index_of_f_in_fearsome,
				max_bytes));
		Assert.assertEquals(expected_length, BlockHash.MatchingBytesToLeft(
				search_string, index_of_f_in_fearsome,
				search_string_altered, index_of_f_in_fearsome,
				Integer.MAX_VALUE));
	}

	@Test
	public void RightLimitedByMaxBytes() throws UnsupportedEncodingException {
		// The number of bytes that match between the original "fearsome"
		// and the altered "fearsomm".
		final int expected_length = (kBlockSize * "fearsom".getBytes("US-ASCII").length) + (kBlockSize - 1);  // spacing between letters
		final int max_bytes = expected_length - 1;
		Assert.assertEquals(max_bytes, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string_altered, index_of_f_in_fearsome,
				max_bytes));
	}

	@Test
	public void RightNotLimited() throws UnsupportedEncodingException {
		// The number of bytes that match between the original "we hear is fearsome"
		// and the altered "ve hear is fearsome".
		final int expected_length = (kBlockSize * "fearsom".getBytes("US-ASCII").length) + (kBlockSize - 1);  // spacing between letters
		final int max_bytes = expected_length + 1;
		Assert.assertEquals(expected_length, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string_altered, index_of_f_in_fearsome,
				max_bytes));
		Assert.assertEquals(expected_length, BlockHash.MatchingBytesToRight(
				search_string, index_of_f_in_fearsome,
				search_string_altered, index_of_f_in_fearsome,
				Integer.MAX_VALUE));
	}

	@Test
	public void BlockContentsMatchIsAsFastAsBlockCompareWords() {
		byte[] compare_buffer_1_ = new byte[kTimingTestSize];
		byte[] compare_buffer_2_ = new byte[kTimingTestSize];

		// The value 0xBE is arbitrarily chosen.  First test with identical contents
		// in the buffers, so that the comparison functions cannot short-circuit
		// and will return true.
		Arrays.fill(compare_buffer_1_, (byte)0xBE);
		Arrays.fill(compare_buffer_2_, (byte)0xBE);

		System.out.printf("Comparing %d identical values:\n", (kTimingTestSize / kBlockSize));

		TestAndPrintTimesForCompareFunctions(true, compare_buffer_1_, compare_buffer_2_);

		// Now change one value in the middle of one buffer, so that the contents
		// are no longer the same.
		compare_buffer_1_[kTimingTestSize / 2] = 0x00;
		System.out.printf("Comparing %d identical values and one mismatch\n", ((kTimingTestSize / kBlockSize) - 1));

		TestAndPrintTimesForCompareFunctions(false, compare_buffer_1_, compare_buffer_2_);

		// Set one of the bytes of each block to differ so that
		// none of the compare operations will return true, and run timing tests.
		// In practice, BlockHash::BlockContentsMatch will only be called
		// for two blocks whose hash values match, and the two most important
		// cases are: (1) the blocks are identical, or (2) none of their bytes match.
		TimingTestForBlocksThatDifferAtByte(0);
		TimingTestForBlocksThatDifferAtByte(1);
		TimingTestForBlocksThatDifferAtByte(kBlockSize / 2);
		TimingTestForBlocksThatDifferAtByte(kBlockSize - 1);
	}

	@Test
	public void FindFailsBeforeHashing() {
		BlockHash th_ = BlockHash.CreateTargetHash(sample_text, 0);
		Assert.assertEquals(-1, th_.FirstMatchingBlock((int)hashed_y, test_string_y, 0));
	}

	@Test
	public void HashOneFindOne() {
		RollingHash rollingHash = new RollingHash(kBlockSize);
		BlockHash th_ = BlockHash.CreateTargetHash(sample_text, 0);
		for (int i = 0; i <= index_of_y_in_only; ++i) {
			th_.AddOneIndexHash(i, (int)rollingHash.Hash(sample_text, i, sample_text.length - i));
		}
		Assert.assertEquals(block_of_y_in_only, th_.FirstMatchingBlock((int)hashed_y, test_string_y, 0));
		Assert.assertEquals(-1, th_.NextMatchingBlock(block_of_y_in_only, test_string_y, 0));
	}

	@Test
	public void HashAllFindOne() {
		BlockHash dh_ = BlockHash.CreateDictionaryHash(sample_text);
		Assert.assertEquals(block_of_y_in_only, dh_.FirstMatchingBlock((int)hashed_y, test_string_y, 0));
		Assert.assertEquals(-1, dh_.NextMatchingBlock(block_of_y_in_only, test_string_y, 0));
	}

	@Test
	public void NonMatchingTextNotFound() {
		BlockHash dh_ = BlockHash.CreateDictionaryHash(sample_text);
		Assert.assertEquals(-1, dh_.FirstMatchingBlock((int)hashed_all_Qs, test_string_all_Qs, 0));
	}

	// Search for unaligned text.  The test string is contained in the
	// sample text (unlike the non-matching string in NonMatchingTextNotFound,
	// above), but it is not aligned on a block boundary.  FindMatchingBlock
	// will only work if the test string is aligned on a block boundary.
	//
	//	    "   T   h   e       o   n   l   y"
	//	              ^^^^ Here is the test string
	@Test
	public void UnalignedTextNotFound() {
		BlockHash dh_ = BlockHash.CreateDictionaryHash(sample_text);
		Assert.assertEquals(-1, dh_.FirstMatchingBlock((int)hashed_unaligned_e, test_string_unaligned_e, 0));
	}

	@Test
	public void FindSixMatches() {
		BlockHash dh_ = BlockHash.CreateDictionaryHash(sample_text);

		Assert.assertEquals(block_of_first_e, dh_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
		Assert.assertEquals(block_of_second_e, dh_.NextMatchingBlock(block_of_first_e, test_string_e, 0));
		Assert.assertEquals(block_of_third_e, dh_.NextMatchingBlock(block_of_second_e, test_string_e, 0));
		Assert.assertEquals(block_of_fourth_e, dh_.NextMatchingBlock(block_of_third_e, test_string_e, 0));
		Assert.assertEquals(block_of_fifth_e, dh_.NextMatchingBlock(block_of_fourth_e, test_string_e, 0));
		Assert.assertEquals(block_of_sixth_e, dh_.NextMatchingBlock(block_of_fifth_e, test_string_e, 0));
		Assert.assertEquals(-1, dh_.NextMatchingBlock(block_of_sixth_e, test_string_e, 0));

		// Starting over gives same result
		Assert.assertEquals(block_of_first_e, dh_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
	}

	@Test
	public void AddRangeFindThreeMatches() {
		BlockHash th_ = BlockHash.CreateTargetHash(sample_text, 0);

		// Add hash values only for those characters before the fourth instance
		// of "e" in the sample text.  Tests that the ending index
		// of AddAllBlocksThroughIndex() is not inclusive: only three matches
		// for "e" should be found.
		th_.AddAllBlocksThroughIndex(index_of_fourth_e);
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
		Assert.assertEquals(block_of_second_e, th_.NextMatchingBlock(block_of_first_e, test_string_e, 0));
		Assert.assertEquals(block_of_third_e, th_.NextMatchingBlock(block_of_second_e, test_string_e, 0));
		Assert.assertEquals(-1, th_.NextMatchingBlock(block_of_third_e, test_string_e, 0));

		// Starting over gives same result
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
	}

	// Try indices that are not even multiples of the block size.
	// Add three ranges and verify the results after each
	// call to AddAllBlocksThroughIndex().
	@Test
	public void AddRangeWithUnalignedIndices() {
		BlockHash th_ = BlockHash.CreateTargetHash(sample_text, 0);

		th_.AddAllBlocksThroughIndex(index_of_first_e + 1);
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
		Assert.assertEquals(-1, th_.NextMatchingBlock(block_of_first_e, test_string_e, 0));

		// Starting over gives same result
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));

		// Add the second range to expand the result set
		th_.AddAllBlocksThroughIndex(index_of_fourth_e - 3);
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
		Assert.assertEquals(block_of_second_e, th_.NextMatchingBlock(block_of_first_e, test_string_e, 0));
		Assert.assertEquals(block_of_third_e, th_.NextMatchingBlock(block_of_second_e, test_string_e, 0));
		Assert.assertEquals(-1, th_.NextMatchingBlock(block_of_third_e, test_string_e, 0));

		// Starting over gives same result
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));

		// Add the third range to expand the result set
		th_.AddAllBlocksThroughIndex(index_of_fourth_e + 1);

		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
		Assert.assertEquals(block_of_second_e, th_.NextMatchingBlock(block_of_first_e, test_string_e, 0));
		Assert.assertEquals(block_of_third_e, th_.NextMatchingBlock(block_of_second_e, test_string_e, 0));
		Assert.assertEquals(block_of_fourth_e, th_.NextMatchingBlock(block_of_third_e, test_string_e, 0));
		Assert.assertEquals(-1, th_.NextMatchingBlock(block_of_fourth_e, test_string_e, 0));

		// Starting over gives same result
		Assert.assertEquals(block_of_first_e, th_.FirstMatchingBlock((int)hashed_e, test_string_e, 0));
	}

	private static void TestAndPrintTimesForCompareFunctions(boolean should_be_identical, byte[] compare_buffer_1_, byte[] compare_buffer_2_) {
		// Prime the memory cache.
		int prime_result_ = 0;
		for (int i = 0; i < kTimingTestSize && prime_result_ == 0; i++) {
			prime_result_ = (compare_buffer_1_[i] & 0xff) - (compare_buffer_2_[i] & 0xff);
		}

		final int block1_limit = kTimingTestSize - kBlockSize;

		int block_contents_match_result = 0;

		long block_contents_time = System.nanoTime();

		for (int i = 0; i < kTimingTestIterations; ++i) {
			int block1 = 0;
			int block2 = 0;
			while (block1 < block1_limit) {
				if (!BlockHash.BlockContentsMatch(compare_buffer_1_, block1, compare_buffer_2_, block2)) {
					++block_contents_match_result;
				}
				block1 += kBlockSize;
				block2 += kBlockSize;
			}
		}

		block_contents_time = System.nanoTime() - block_contents_time;

		double time_for_block_contents_match = (double)block_contents_time / 1000.0 / ((kTimingTestSize / kBlockSize) * kTimingTestIterations);

		if (should_be_identical) {
			Assert.assertEquals(0, block_contents_match_result);
		} else {
			Assert.assertTrue(block_contents_match_result > 0);
		}

		System.out.println( "BlockHash.BlockContentsMatch: " + time_for_block_contents_match + " us per operation");
	}

	void TimingTestForBlocksThatDifferAtByte(int n) {
		byte[] compare_buffer_1_ = new byte[kTimingTestSize];
		byte[] compare_buffer_2_ = new byte[kTimingTestSize];

		Arrays.fill(compare_buffer_1_, (byte)0xBE);
		Arrays.fill(compare_buffer_2_, (byte)0xBE);

		for (int index = n; index < kTimingTestSize; index += kBlockSize) {
			compare_buffer_1_[index] = 0x00;
			compare_buffer_2_[index] = 0x01;
		}

		System.out.printf("Comparing blocks that differ at byte %d\n", n);
		TestAndPrintTimesForCompareFunctions(false, compare_buffer_1_, compare_buffer_2_);
	}

	// Copy sample_text_without_spaces and search_string_without_spaces
	// into newly allocated sample_text and search_string buffers,
	// but pad them with space characters so that every character
	// in sample_text_without_spaces matches (kBlockSize - 1)
	// space characters in sample_text, followed by that character.
	// For example:
	// Since sample_text_without_spaces begins "The only thing"...,
	// if kBlockSize is 4, then 3 space characters will be inserted
	// between each letter of sample_text, as follows:
	// "   T   h   e       o   n   l   y       t   h   i   n   g"...
	// This makes testing simpler, because finding a kBlockSize-byte match
	// between the sample text and search string only depends on the
	// trailing letter in each block.
	private static byte[] MakeEachLetterABlock(String string_without_spaces) {
		byte[] bytes;
		try {
			bytes = string_without_spaces.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			return null;
		}

		byte[] result = new byte[bytes.length * kBlockSize];
		Arrays.fill(result, (byte)' ');

		for (int i = 0, j = kBlockSize - 1; i < bytes.length; i++, j+=kBlockSize) {
			result[j] = bytes[i];
		}

		return result;
	}


}
