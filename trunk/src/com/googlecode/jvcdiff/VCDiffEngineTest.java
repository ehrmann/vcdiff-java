package com.googlecode.jvcdiff;

import static com.googlecode.jvcdiff.BlockHash.kBlockSize;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Test;

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
	private boolean interleaved_;
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

	private void EncodeNothing(boolean interleaved, boolean target_matching) throws IOException {
		interleaved_ = interleaved;
		VCDiffCodeTableWriter coder = new VCDiffCodeTableWriter(interleaved);
		coder.Init(engine_.dictionary_size());
		
		engine_.Encode(ByteBuffer.allocate(0), target_matching, null, coder);
		assertEquals(0, diff_.size());
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
