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
