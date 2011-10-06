package com.googlecode.jvcdiff;

import static com.googlecode.jvcdiff.BlockHash.kBlockSize;
import static com.googlecode.jvcdiff.VCDiffAddressCache.VCD_SELF_MODE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;

public class VCDiffEngineTest {

	private static final Charset US_ASCII = Charset.forName("US-ASCII");

	private static final String dictionary_without_spaces_ = "The only thing we have to fear is fear itself";
	private static final String target_without_spaces_ = "What we hear is fearsome";

	private static final byte[] dictionary_;
	private static final byte[] target_;

	static {
		dictionary_ = MakeEachLetterABlock(dictionary_without_spaces_, kBlockSize, false);
		target_ = MakeEachLetterABlock(target_without_spaces_, kBlockSize, false);
	}

	private final VCDiffEngine engine_;
	private final VCDiffAddressCache default_cache_ = null;
	private boolean interleaved_;
	private int saved_total_size_position_ = 0;
	private int saved_delta_encoding_position_ = 0;
	private int saved_section_sizes_position_ = 0;
	private int instruction_bytes_ = 0;
	private int data_bytes_ = 0;
	private int address_bytes_ = 0;

	private ByteArrayOutputStream diff_ = new ByteArrayOutputStream();

	public VCDiffEngineTest() {
		engine_ = new VCDiffEngine(dictionary_);
	}

	@Test
	public void EngineEncodeNothing() throws IOException {
		EncodeNothing(/* interleaved = */ false, /* target matching = */ false);
	}

	@Test
	public void EngineEncodeNothingInterleaved() throws IOException {
		EncodeNothing(/* interleaved = */ true, /* target matching = */ false);
	}

	@Test
	public void EngineEncodeNothingTarget() throws IOException {
		EncodeNothing(/* interleaved = */ false, /* target matching = */ true);
	}

	@Test
	public void EngineEncodeNothingTargetInterleaved() throws IOException {
		EncodeNothing(/* interleaved = */ true, /* target matching = */ true);
	}

	@Test
	public void EngineEncodeSmallerThanOneBlock() throws IOException, VarIntParseException, VarIntEndOfBufferException {
		final byte[] small_text = "  ".getBytes(US_ASCII);
		EncodeText(small_text, /* interleaved = */ false, /* target matching = */ false);

		ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
		VerifyHeaderForDictionaryAndTargetText(dictionary_, small_text, actual);

		// Data for ADDs
		byte[] actual_small_text = new byte[small_text.length];
		actual.get(actual_small_text);
		Assert.assertArrayEquals(small_text, actual_small_text);

		// Instructions and sizes
		ExpectAddInstructionForStringLength(small_text, actual);
	}

	@Test
	public void EngineEncodeSmallerThanOneBlockInterleaved() throws IOException, VarIntParseException, VarIntEndOfBufferException {
		byte[] small_text = "  ".getBytes(US_ASCII);
		EncodeText(small_text, /* interleaved = */ true, /* target matching = */ false);

		ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
		VerifyHeaderForDictionaryAndTargetText(dictionary_, small_text, actual);

		// Interleaved section
		ExpectAddInstructionForStringLength(small_text, actual);
		ExpectDataString(small_text, actual);
	}

	@Test
	public void EngineEncodeSampleText() throws VarIntParseException, VarIntEndOfBufferException, IOException {
		Encode(/* interleaved = */ false, /* target matching = */ false);

		ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
		VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);
		
		// Data for ADDs
		ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), false, actual);
		ExpectDataByte((byte)'t', actual);
		ExpectDataByte((byte)'s', actual);
		ExpectDataByte((byte)'m', actual);
		// Instructions and sizes
		if (!ExpectAddCopyInstruction(kBlockSize, (3 * kBlockSize) - 1, VCD_SELF_MODE, actual)) {
			ExpectCopyInstruction((3 * kBlockSize) - 1, VCD_SELF_MODE, actual);
		}
		ExpectAddInstruction(1, actual);
		ExpectCopyInstruction((6 * kBlockSize) - 1, VCD_SELF_MODE, actual);
		ExpectCopyInstruction(11 * kBlockSize, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, actual);
		if (!ExpectAddCopyInstruction(1, (2 * kBlockSize) - 1, VCD_SELF_MODE, actual)) {
			ExpectCopyInstruction((2 * kBlockSize) - 1, VCD_SELF_MODE, actual);
		}
		if (!ExpectAddCopyInstruction(1, kBlockSize, VCD_SELF_MODE, actual)) {
			ExpectCopyInstruction(kBlockSize, VCD_SELF_MODE, actual);
		}
		// Addresses for COPY
		ExpectAddressVarint(18 * kBlockSize, actual);  // "ha"
		ExpectAddressVarint(14 * kBlockSize, actual);  // " we h"
		ExpectAddressVarint((9 * kBlockSize) + (kBlockSize - 1), actual);  // "ear is fear"
		ExpectAddressVarint(4 * kBlockSize, actual);  // "o" from "The only"
		ExpectAddressVarint(2 * kBlockSize, actual);  // "e" from "The only"

		VerifySizes(actual);
	}

	private void EncodeNothing(boolean interleaved, boolean target_matching) throws IOException {
		interleaved_ = interleaved;
		VCDiffCodeTableWriter coder = new VCDiffCodeTableWriter(interleaved);
		coder.Init(engine_.dictionary_size());

		engine_.Encode(ByteBuffer.allocate(0), target_matching, null, coder);
		assertEquals(0, diff_.size());
	}

	private void EncodeText(byte[] bytes, boolean interleaved, boolean target_matching) throws IOException {
		interleaved_ = interleaved;
		VCDiffCodeTableWriter coder = new VCDiffCodeTableWriter(interleaved);
		coder.Init(engine_.dictionary_size());
		engine_.Encode(ByteBuffer.wrap(bytes), target_matching, diff_, coder);
	}

	protected void Encode(boolean interleaved, boolean target_matching) throws IOException, VarIntParseException, VarIntEndOfBufferException {
		EncodeText(target_, interleaved, target_matching);
		VerifyHeader();
	}

	protected void VerifyHeader() throws VarIntParseException, VarIntEndOfBufferException {
		VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, ByteBuffer.wrap(diff_.toByteArray()));
	}

	// Call this function before beginning to iterate through the diff string
	// using the Expect... functions.
	protected void VerifyHeaderForDictionaryAndTargetText(byte[] dictionary, byte[] target_text, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		ExpectByte((byte)0x01, actual);		// Win_Indicator: VCD_SOURCE (dictionary)
		ExpectStringLength(dictionary, actual);
		ExpectByte((byte)0x00, actual);		// Source segment position: start of dictionary
		saved_total_size_position_ = actual.position();
		SkipVarint(actual);					// Length of the delta encoding
		saved_delta_encoding_position_ = actual.position();
		ExpectStringLength(target_text, actual);
		ExpectByte((byte)0x00, actual);		// Delta_indicator (no compression)
		saved_section_sizes_position_ = actual.position();
		SkipVarint(actual);					// length of data for ADDs and RUNs
		SkipVarint(actual);					// length of instructions section
		SkipVarint(actual);					// length of addresses for COPYs
	}

	// These functions iterate through the decoded output and expect
	// simple elements: bytes or variable-length integers.
	private void ExpectByte(byte b, ByteBuffer actual) {
		assertEquals(b, actual.get());
	}

	private int ExpectVarint(int expected_value, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		int original_position = actual.position();
		int expected_length = VarInt.calculateIntLength(expected_value);
		int parsed_value = VarInt.getInt(actual);

		assertEquals(expected_length, actual.position() - original_position);
		assertEquals(expected_value, parsed_value);

		return expected_length;
	}

	void SkipVarint(ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		VarInt.getInt(actual);
	}

	void ExpectDataByte(byte b, ByteBuffer actual) {
		ExpectByte(b, actual);
		if (interleaved_) {
			++instruction_bytes_;
		} else {
			++data_bytes_;
		}
	}

	void ExpectDataString(byte[] expected_string, ByteBuffer actual) {
		byte[] actual_string = new byte[expected_string.length];
		actual.get(actual_string);
		assertArrayEquals(expected_string, actual_string);
	}

	void ExpectDataStringWithBlockSpacing(byte[] expected_string, boolean trailing_spaces, ByteBuffer actual) {
		byte[] exploded;
		if (trailing_spaces) {
			exploded = new byte[expected_string.length * kBlockSize + kBlockSize - 1];
		} else {
			exploded = new byte[expected_string.length * kBlockSize];
		}

		Arrays.fill(exploded, (byte)' ');

		for (int i = 0; i < expected_string.length; i++) {
			exploded[(i + 1) * kBlockSize - 1] = expected_string[i];
		}

		byte[] actual_string = new byte[exploded.length];
		actual.get(actual_string);
		assertArrayEquals(exploded, actual_string);
	}

	void ExpectInstructionByte(byte b, ByteBuffer actual) {
		ExpectByte(b, actual);
		++instruction_bytes_;
	}

	void ExpectInstructionVarint(int value, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		instruction_bytes_ += ExpectVarint(value, actual);
	}

	void ExpectAddressByte(byte b, ByteBuffer actual) {
		ExpectByte(b, actual);
		if (interleaved_) {
			++instruction_bytes_;
		} else {
			++address_bytes_;
		}
	}

	void ExpectAddressVarint(int value, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		if (interleaved_) {
			instruction_bytes_ += ExpectVarint(value, actual);
		} else {
			address_bytes_ += ExpectVarint(value, actual);
		}
	}

	// The following functions leverage the fact that the encoder uses
	// the default code table and cache sizes.  They are able to search for
	// instructions of a particular size.  The logic for mapping from
	// instruction type, mode, and size to opcode value is very different here
	// from the logic used in encodetable.{h,cc}, so hopefully
	// this version will help validate that the other is correct.
	// This version uses conditional statements, while encodetable.h
	// looks up values in a mapping table.
	private void ExpectAddress(byte address, short copy_mode, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		if (default_cache_.WriteAddressAsVarintForMode(copy_mode)) {
			ExpectAddressVarint(address, actual);
		} else {
			ExpectAddressByte(address, actual);
		}
	}

	private void ExpectAddInstruction(int size, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		if (size <= 18) {
			ExpectInstructionByte((byte)(0x01 + size), actual);
		} else {
			ExpectInstructionByte((byte)0x01, actual);
			ExpectInstructionVarint(size, actual);
		}
	}

	private void ExpectCopyInstruction(int size, int mode, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		if ((size >= 4) && (size <= 16)) {
			ExpectInstructionByte((byte)(0x10 + (0x10 * mode) + size), actual);
		} else {
			ExpectInstructionByte((byte)(0x13 + (0x10 * mode)), actual);
			ExpectInstructionVarint(size, actual);
		}
	}

	private boolean ExpectAddCopyInstruction(int add_size, int copy_size, short copy_mode, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		if (!default_cache_.IsSameMode(copy_mode) &&
				(add_size <= 4) &&
				(copy_size >= 4) &&
				(copy_size <= 6)) {
			ExpectInstructionByte((byte)(0x9C +
					(0x0C * copy_mode) +
					(0x03 * add_size) +
					copy_size), actual);
			return true;
		} else if (default_cache_.IsSameMode(copy_mode) &&
				(add_size <= 4) &&
				(copy_size == 4)) {
			ExpectInstructionByte((byte)(0xD2 + (0x04 * copy_mode) + add_size), actual);
			return true;
		} else {
			ExpectAddInstruction((byte)add_size, actual);
			return false;
		}
	}

	private void ExpectAddInstructionForStringLength(byte[] s, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		ExpectAddInstruction(s.length, actual);
	}

	private int ExpectStringLength(byte[] s, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		return ExpectSize(s.length, actual);
	}

	int ExpectSize(int size, ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		return ExpectVarint(size, actual);
	}

	// Call this function before beginning to iterating through the entire
	// diff string using the Expect... functions.  It makes sure that the
	// size totals in the window header match the number of bytes that
	// were parsed.
	void VerifySizes(ByteBuffer actual) throws VarIntParseException, VarIntEndOfBufferException {
		Assert.assertFalse(actual.hasRemaining());

		final int delta_encoding_size = actual.position() - saved_delta_encoding_position_;

		actual.position(saved_total_size_position_);
		ExpectSize(delta_encoding_size, actual);

		actual.position(saved_section_sizes_position_);
		ExpectSize(data_bytes_, actual);
		ExpectSize(instruction_bytes_, actual);
		ExpectSize(address_bytes_, actual);
	}


	// Copy string_without_spaces into newly allocated result buffer,
	// but pad its contents with space characters so that every character
	// in string_without_spaces corresponds to (block_size - 1)
	// spaces in the result, followed by that character.
	// For example:
	// If string_without_spaces begins "The only thing"... and block_size is 4,
	// then 3 space characters will be inserted
	// between each letter in the result, as follows:
	// "   T   h   e       o   n   l   y       t   h   i   n   g"...
	// This makes testing simpler, because finding a block_size-byte match
	// between the dictionary and target only depends on the
	// trailing letter in each block.
	// If no_initial_padding is true, then the first letter will not have
	// spaces added in front of it.
	private static byte[] MakeEachLetterABlock(String string_without_spaces, int block_size, boolean no_initial_padding) {
		byte[] bytes_without_spaces = string_without_spaces.getBytes(US_ASCII);
		byte[] padded_text = new byte[(block_size * bytes_without_spaces.length)];
		Arrays.fill(padded_text, (byte)' ');

		int padded_text_index = 0;
		if (!no_initial_padding) {
			padded_text_index = block_size - 1;
		}

		for (int i = 0; i < bytes_without_spaces.length; ++i) {
			padded_text[padded_text_index] = bytes_without_spaces[i];
			padded_text_index += block_size;
		}

		return padded_text;
	}
}
