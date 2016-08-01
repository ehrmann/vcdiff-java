package com.davidehrmann.vcdiff;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Suite.SuiteClasses;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

@SuiteClasses({VCDiffEngineTest.VCDiffEngineTestImpl.class, VCDiffEngineTest.WeaselsToMoonpiesTest.class})
public abstract class VCDiffEngineTest {

    // Some common definitions and helper functions used in the various tests
    // for VCDiffEngine.


    protected static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected final VCDiffAddressCache default_cache_ = new VCDiffAddressCacheImpl();
    protected final byte[] dictionary_;
    protected final byte[] target_;
    private final VCDiffEngine engine_;
    protected ByteArrayOutputStream diff_ = new ByteArrayOutputStream();
    private boolean interleaved_;
    private int saved_total_size_position_ = 0;
    private int saved_delta_encoding_position_ = 0;
    private int saved_section_sizes_position_ = 0;
    private int instruction_bytes_ = 0;
    private int data_bytes_ = 0;
    private int address_bytes_ = 0;

    public VCDiffEngineTest(byte[] dictionary, byte[] target) {
        if (target == null || dictionary == null) {
            throw new NullPointerException();
        }

        target_ = target;
        dictionary_ = dictionary;

        engine_ = new VCDiffEngine(dictionary_);
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
    protected static byte[] MakeEachLetterABlock(String string_without_spaces, int block_size, boolean no_initial_padding) {
        byte[] bytes_without_spaces = string_without_spaces.getBytes(US_ASCII);
        byte[] padded_text = new byte[block_size * bytes_without_spaces.length - (no_initial_padding ? block_size - 1 : 0)];
        Arrays.fill(padded_text, (byte) ' ');

        int padded_text_index = 0;
        if (!no_initial_padding) {
            padded_text_index = block_size - 1;
        }

        for (byte bytes_without_space : bytes_without_spaces) {
            padded_text[padded_text_index] = bytes_without_space;
            padded_text_index += block_size;
        }

        return padded_text;
    }

    protected void EncodeNothing(boolean interleaved, boolean target_matching) throws IOException {
        interleaved_ = interleaved;
        VCDiffCodeTableWriter coder = new VCDiffCodeTableWriter(interleaved);
        coder.Init(engine_.dictionary_size());

        engine_.Encode(ByteBuffer.allocate(0), target_matching, null, coder);
        assertEquals(0, diff_.size());
    }

    protected void EncodeText(byte[] bytes, boolean interleaved, boolean target_matching) throws IOException {
        interleaved_ = interleaved;
        VCDiffCodeTableWriter coder = new VCDiffCodeTableWriter(interleaved);
        coder.Init(engine_.dictionary_size());
        engine_.Encode(ByteBuffer.wrap(bytes), target_matching, diff_, coder);
    }

    protected void Encode(boolean interleaved, boolean target_matching) throws IOException, VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        EncodeText(target_, interleaved, target_matching);
        VerifyHeader();
    }

    protected void VerifyHeader() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, ByteBuffer.wrap(diff_.toByteArray()));
    }

    // Call this function before beginning to iterate through the diff string
    // using the Expect... functions.
    protected void VerifyHeaderForDictionaryAndTargetText(byte[] dictionary, byte[] target_text, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        ExpectByte((byte) 0x01, actual);        // Win_Indicator: VCD_SOURCE (dictionary)
        ExpectStringLength(dictionary, actual);
        ExpectByte((byte) 0x00, actual);        // Source segment position: start of dictionary
        saved_total_size_position_ = actual.position();
        SkipVarint(actual);                    // Length of the delta encoding
        saved_delta_encoding_position_ = actual.position();
        ExpectStringLength(target_text, actual);
        ExpectByte((byte) 0x00, actual);        // Delta_indicator (no compression)
        saved_section_sizes_position_ = actual.position();
        SkipVarint(actual);                    // length of data for ADDs and RUNs
        SkipVarint(actual);                    // length of instructions section
        SkipVarint(actual);                    // length of addresses for COPYs
    }

    // These functions iterate through the decoded output and expect
    // simple elements: bytes or variable-length integers.
    protected void ExpectByte(byte b, ByteBuffer actual) {
        assertEquals(b, actual.get());
    }

    protected int ExpectVarint(int expected_value, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        int original_position = actual.position();
        int expected_length = VarInt.calculateIntLength(expected_value);
        int parsed_value = VarInt.getInt(actual);

        assertEquals(expected_value, parsed_value);
        assertEquals(expected_length, actual.position() - original_position);

        return expected_length;
    }

    protected void SkipVarint(ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        VarInt.getInt(actual);
    }

    protected void ExpectDataByte(byte b, ByteBuffer actual) {
        ExpectByte(b, actual);
        if (interleaved_) {
            ++instruction_bytes_;
        } else {
            ++data_bytes_;
        }
    }

    protected void ExpectDataString(byte[] expected_string, ByteBuffer actual) {
        for (byte b : expected_string) {
            ExpectDataByte(b, actual);
        }
    }

    protected void ExpectDataStringWithBlockSpacing(byte[] expected_string, int blockSize, boolean trailing_spaces, ByteBuffer actual) {
        for (byte b : expected_string) {
            for (int i = 0; i < (blockSize - 1); ++i) {
                ExpectDataByte((byte) ' ', actual);
            }
            ExpectDataByte(b, actual);
        }

        if (trailing_spaces) {
            for (int i = 0; i < (blockSize - 1); ++i) {
                ExpectDataByte((byte) ' ', actual);
            }
        }
    }

    protected void ExpectInstructionByte(byte b, ByteBuffer actual) {
        ExpectByte(b, actual);
        ++instruction_bytes_;
    }

    protected void ExpectInstructionVarint(int value, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        instruction_bytes_ += ExpectVarint(value, actual);
    }

    protected void ExpectAddressByte(byte b, ByteBuffer actual) {
        ExpectByte(b, actual);
        if (interleaved_) {
            ++instruction_bytes_;
        } else {
            ++address_bytes_;
        }
    }

    protected void ExpectAddressVarint(int value, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        if (interleaved_) {
            instruction_bytes_ += ExpectVarint(value, actual);
        } else {
            address_bytes_ += ExpectVarint(value, actual);
        }
    }

    protected void ExpectCopyForSize(int size, int mode, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        ExpectCopyInstruction(size, mode, actual);
    }

    protected void ExpectAddressVarintForSize(int value, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        ExpectAddressVarint(value, actual);
    }

    // The following functions leverage the fact that the encoder uses
    // the default code table and cache sizes.  They are able to search for
    // instructions of a particular size.  The logic for mapping from
    // instruction type, mode, and size to opcode value is very different here
    // from the logic used in encodetable.{h,cc}, so hopefully
    // this version will help validate that the other is correct.
    // This version uses conditional statements, while encodetable.h
    // looks up values in a mapping table.
    protected void ExpectAddress(int address, short copy_mode, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        if (default_cache_.WriteAddressAsVarintForMode(copy_mode)) {
            ExpectAddressVarint(address, actual);
        } else {
            ExpectAddressByte((byte) address, actual);
        }
    }

    protected void ExpectAddInstruction(int size, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        if (size <= 18) {
            ExpectInstructionByte((byte) (0x01 + size), actual);
        } else {
            ExpectInstructionByte((byte) 0x01, actual);
            ExpectInstructionVarint(size, actual);
        }
    }

    protected void ExpectCopyInstruction(int size, int mode, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        if ((size >= 4) && (size <= 16)) {
            ExpectInstructionByte((byte) (0x10 + (0x10 * mode) + size), actual);
        } else {
            ExpectInstructionByte((byte) (0x13 + (0x10 * mode)), actual);
            ExpectInstructionVarint(size, actual);
        }
    }

    protected boolean ExpectAddCopyInstruction(int add_size, int copy_size, short copy_mode, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        if (!default_cache_.IsSameMode(copy_mode) &&
                (add_size <= 4) &&
                (copy_size >= 4) &&
                (copy_size <= 6)) {
            ExpectInstructionByte((byte) (0x9C +
                    (0x0C * copy_mode) +
                    (0x03 * add_size) +
                    copy_size), actual);
            return true;
        } else if (default_cache_.IsSameMode(copy_mode) &&
                (add_size <= 4) &&
                (copy_size == 4)) {
            ExpectInstructionByte((byte) (0xD2 + (0x04 * copy_mode) + add_size), actual);
            return true;
        } else {
            ExpectAddInstruction((byte) add_size, actual);
            return false;
        }
    }

    protected void ExpectAddInstructionForStringLength(byte[] s, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        ExpectAddInstruction(s.length, actual);
    }

    protected int ExpectStringLength(byte[] s, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        return ExpectSize(s.length, actual);
    }

    protected int ExpectSize(int size, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        return ExpectVarint(size, actual);
    }

    // Call this function before beginning to iterating through the entire
    // diff string using the Expect... functions.  It makes sure that the
    // size totals in the window header match the number of bytes that
    // were parsed.
    protected void VerifySizes(ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        Assert.assertFalse(actual.hasRemaining());

        final int delta_encoding_size = actual.position() - saved_delta_encoding_position_;

        actual.position(saved_total_size_position_);
        ExpectSize(delta_encoding_size, actual);

        actual.position(saved_section_sizes_position_);
        ExpectSize(data_bytes_, actual);
        ExpectSize(instruction_bytes_, actual);
        ExpectSize(address_bytes_, actual);
    }

    public static class VCDiffEngineTestImpl extends VCDiffEngineTest {

        protected static final int kBlockSize = VCDiffEngine.kMinimumMatchSize;

        private static final String dictionary_without_spaces_ = "The only thing we have to fear is fear itself";
        private static final String target_without_spaces_ = "What we hear is fearsome";

        public VCDiffEngineTestImpl() {
            super(
                    MakeEachLetterABlock(dictionary_without_spaces_, kBlockSize, false),
                    MakeEachLetterABlock(target_without_spaces_, kBlockSize, false));
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
        public void EngineEncodeSmallerThanOneBlock() throws IOException, VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
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
        public void EngineEncodeSmallerThanOneBlockInterleaved() throws IOException, VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
            byte[] small_text = "  ".getBytes(US_ASCII);
            EncodeText(small_text, /* interleaved = */ true, /* target matching = */ false);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, small_text, actual);

            // Interleaved section
            ExpectAddInstructionForStringLength(small_text, actual);
            ExpectDataString(small_text, actual);
        }

        @Test
        public void EngineEncodeSampleText() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
            Encode(/* interleaved = */ false, /* target matching = */ false);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);

            // Data for ADDs
            ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), kBlockSize, false, actual);
            ExpectDataByte((byte) 't', actual);
            ExpectDataByte((byte) 's', actual);
            ExpectDataByte((byte) 'm', actual);
            // Instructions and sizes
            if (!ExpectAddCopyInstruction(kBlockSize, (3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectCopyInstruction((3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            }
            ExpectAddInstruction(1, actual);
            ExpectCopyInstruction((6 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            ExpectCopyInstruction(11 * kBlockSize, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, actual);
            if (!ExpectAddCopyInstruction(1, (2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectCopyInstruction((2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            }
            if (!ExpectAddCopyInstruction(1, kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectCopyInstruction(kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual);
            }
            // Addresses for COPY
            ExpectAddressVarint(18 * kBlockSize, actual);  // "ha"
            ExpectAddressVarint(14 * kBlockSize, actual);  // " we h"
            ExpectAddressVarint((9 * kBlockSize) + (kBlockSize - 1), actual);  // "ear is fear"
            ExpectAddressVarint(4 * kBlockSize, actual);  // "o" from "The only"
            ExpectAddressVarint(2 * kBlockSize, actual);  // "e" from "The only"

            VerifySizes(actual);
        }

        @Test
        public void EngineEncodeSampleTextInterleaved() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
            Encode(/* interleaved = */ true, /* target matching = */ false);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);

            // Interleaved section
            if (!ExpectAddCopyInstruction(kBlockSize, (3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), kBlockSize, false, actual);
                ExpectCopyInstruction((3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            } else {
                ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), kBlockSize, false, actual);
            }

            ExpectAddressVarint(18 * kBlockSize, actual);  // "ha"
            ExpectAddInstruction(1, actual);
            ExpectDataByte((byte) 't', actual);
            ExpectCopyInstruction((6 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            ExpectAddressVarint(14 * kBlockSize, actual);  // " we h"
            ExpectCopyInstruction(11 * kBlockSize, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, actual);
            ExpectAddressVarint((9 * kBlockSize) + (kBlockSize - 1), actual);  // "ear is fear"

            if (!ExpectAddCopyInstruction(1, (2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectDataByte((byte) 's', actual);
                ExpectCopyInstruction((2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            } else {
                ExpectDataByte((byte) 's', actual);
            }

            ExpectAddressVarint(4 * kBlockSize, actual);  // "o" from "The only"

            if (!ExpectAddCopyInstruction(1, kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectDataByte((byte) 'm', actual);
                ExpectCopyInstruction(kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual);
            } else {
                ExpectDataByte((byte) 'm', actual);
            }

            ExpectAddressVarint(2 * kBlockSize, actual);  // "e" from "The only"
            VerifySizes(actual);
        }

        @Test
        public void EngineEncodeSampleTextWithTargetMatching() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
            Encode(/* interleaved = */ false, /* target matching = */ true);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);

            // Data for ADDs
            ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), kBlockSize, false, actual);
            ExpectDataByte((byte) 't', actual);
            ExpectDataByte((byte) 's', actual);
            ExpectDataByte((byte) 'm', actual);

            // Instructions and sizes
            if (!ExpectAddCopyInstruction(kBlockSize, (3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectCopyInstruction((3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            }

            ExpectAddInstruction(1, actual);
            ExpectCopyInstruction((6 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            ExpectCopyInstruction(11 * kBlockSize, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, actual);

            if (!ExpectAddCopyInstruction(1, (2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectCopyInstruction((2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            }

            if (!ExpectAddCopyInstruction(1, kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectCopyInstruction(kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual);
            }

            // Addresses for COPY
            ExpectAddressVarint(18 * kBlockSize, actual);  // "ha"
            ExpectAddressVarint(14 * kBlockSize, actual);  // " we h"
            ExpectAddressVarint((9 * kBlockSize) + (kBlockSize - 1), actual);  // "ear is fear"
            ExpectAddressVarint(4 * kBlockSize, actual);  // "o" from "The only"
            ExpectAddressVarint(2 * kBlockSize, actual);  // "e" from "The only"

            VerifySizes(actual);
        }

        // FIXME: This test case matches EngineEncodeSampleTextInterleaved().
        // See http://code.google.com/p/open-vcdiff/issues/detail?id=32
        @Test
        public void EngineEncodeSampleTextInterleavedWithTargetMatching() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
            Encode(/* interleaved = */ true, /* target matching = */ false);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);

            // Interleaved section
            if (!ExpectAddCopyInstruction(kBlockSize, (3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), kBlockSize, false, actual);
                ExpectCopyInstruction((3 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            } else {
                ExpectDataStringWithBlockSpacing("W".getBytes(US_ASCII), kBlockSize, false, actual);
            }
            ExpectAddressVarint(18 * kBlockSize, actual);  // "ha"
            ExpectAddInstruction(1, actual);
            ExpectDataByte((byte) 't', actual);
            ExpectCopyInstruction((6 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            ExpectAddressVarint(14 * kBlockSize, actual);  // " we h"
            ExpectCopyInstruction(11 * kBlockSize, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, actual);
            ExpectAddressVarint((9 * kBlockSize) + (kBlockSize - 1), actual);  // "ear is fear"
            if (!ExpectAddCopyInstruction(1, (2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectDataByte((byte) 's', actual);
                ExpectCopyInstruction((2 * kBlockSize) - 1, VCDiffAddressCache.VCD_SELF_MODE, actual);
            } else {
                ExpectDataByte((byte) 's', actual);
            }
            ExpectAddressVarint(4 * kBlockSize, actual);  // "o" from "The only"
            if (!ExpectAddCopyInstruction(1, kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual)) {
                ExpectDataByte((byte) 'm', actual);
                ExpectCopyInstruction(kBlockSize, VCDiffAddressCache.VCD_SELF_MODE, actual);
            } else {
                ExpectDataByte((byte) 'm', actual);
            }
            ExpectAddressVarint(2 * kBlockSize, actual);  // "e" from "The only"
            VerifySizes(actual);
        }
    }

    // This test case takes a dictionary containing several instances of the string
    // "weasel", and a target string which is identical to the dictionary
    // except that all instances of "weasel" have been replaced with the string
    // "moon-pie".  It tests that COPY instructions are generated for all
    // boilerplate text (that is, the text between the "moon-pie" instances in
    // the target) and, if target matching is enabled, that each instance of
    // "moon-pie" (except the first one) is encoded using a COPY instruction
    // rather than an ADD.
    public static class WeaselsToMoonpiesTest extends VCDiffEngineTest {

        // kCompressibleTestBlockSize:
        // The size of the block to create for each letter in the
        // dictionary and search string for the "compressible text" test.
        // See MakeEachLetterABlock, below.
        // If we use kCompressibleTestBlockSize = kBlockSize, then the
        // encoder will find one match per unique letter in the HTML text.
        // There are too many examples of "<" in the text for the encoder
        // to iterate through them all, and some matches are not found.
        // If we use kCompressibleTextBlockSize = 1, then the boilerplate
        // text between "weasel" strings in the dictionary and "moon-pie"
        // strings in the target may not be long enough to be found by
        // the encoder's block-hash algorithm.  A good value, that will give
        // reproducible results across all block sizes, will be somewhere
        // in between these extremes.
        protected static final int kCompressibleTestBlockSize = VCDiffEngine.kMinimumMatchSize / 4;
        protected static final int kTrailingSpaces = kCompressibleTestBlockSize - 1;

        // Care is taken in the formulation of the dictionary
        // to ensure that the surrounding letters do not match; for example,
        // there are not two instances of the string "weasels".  Otherwise,
        // the matching behavior would not be as predictable.
        protected static final String dictionary_without_spaces_ =
                "<html>\n" +
                        "<head>\n" +
                        "<meta content=\"text/html; charset=ISO-8859-1\"\n" +
                        "http-equiv=\"content-type\">\n" +
                        "<title>All about weasels</title>\n" +
                        "</head>\n" +
                        "<!-- You will notice that the word \"weasel\" may be replaced" +
                        " with something else -->\n" +
                        "<body>\n" +
                        "<h1>All about the weasel: highly compressible HTML text</h1>" +
                        "<ul>\n" +
                        "<li>Don't look a gift weasel in its mouth.</li>\n" +
                        "<li>This item makes sure the next occurrence is found.</li>\n" +
                        "<li>Don't count your weasel, before it's hatched.</li>\n" +
                        "</ul>\n" +
                        "<br>\n" +
                        "</body>\n" +
                        "</html>\n";

        protected static final String target_without_spaces_ =
                "<html>\n" +
                        "<head>\n" +
                        "<meta content=\"text/html; charset=ISO-8859-1\"\n" +
                        "http-equiv=\"content-type\">\n" +
                        "<title>All about moon-pies</title>\n" +
                        "</head>\n" +
                        "<!-- You will notice that the word \"moon-pie\" may be replaced" +
                        " with something else -->\n" +
                        "<body>\n" +
                        "<h1>All about the moon-pie: highly compressible HTML text</h1>" +
                        "<ul>\n" +
                        "<li>Don't look a gift moon-pie in its mouth.</li>\n" +
                        "<li>This item makes sure the next occurrence is found.</li>\n" +
                        "<li>Don't count your moon-pie, before it's hatched.</li>\n" +
                        "</ul>\n" +
                        "<br>\n" +
                        "</body>\n" +
                        "</html>\n";

        protected static final String weasel_text_without_spaces_ = "weasel";
        protected static final String moonpie_text_without_spaces_ = "moon-pie";

        //protected final byte[] dictionary_;
        //protected final byte[] target_;

        protected final byte[] weasel_text_;
        protected final byte[] moonpie_text_;

        protected final int[] weasel_positions_ = new int[128];
        protected final int[] after_weasel_ = new int[128];
        protected final int[] moonpie_positions_ = new int[128];
        protected final int[] after_moonpie_ = new int[128];
        protected int match_index_ = 0;
        protected int copied_moonpie_address_ = 0;

        public WeaselsToMoonpiesTest() {
            super(
                    MakeEachLetterABlock(dictionary_without_spaces_, kCompressibleTestBlockSize, false),
                    MakeEachLetterABlock(target_without_spaces_, kCompressibleTestBlockSize, false));

            weasel_text_ = MakeEachLetterABlock(weasel_text_without_spaces_, kCompressibleTestBlockSize, true);
            moonpie_text_ = MakeEachLetterABlock(moonpie_text_without_spaces_, kCompressibleTestBlockSize, true);
        }

        private static int byteArraySubstring(byte[] source, byte[] target, int start) {
            for (int i = start; i < source.length; i++) {
                int j;
                for (j = 0; j < target.length && i + j < source.length; j++) {
                    if (source[i + j] != target[j]) {
                        break;
                    }
                }

                if (j == target.length) {
                    return i;
                }
            }
            return -1;
        }

        void FindNextMoonpie(boolean include_trailing_spaces) {
            ++match_index_;
            SetCurrentWeaselPosition(byteArraySubstring(dictionary_, weasel_text_, AfterLastWeasel()));
            if (CurrentWeaselPosition() == -1) {
                SetCurrentMoonpiePosition(-1);
            } else {
                SetCurrentAfterWeaselPosition(CurrentWeaselPosition()
                        + weasel_text_.length
                        + (include_trailing_spaces ?
                        kTrailingSpaces : 0));
                SetCurrentMoonpiePosition(AfterLastMoonpie()
                        + CurrentBoilerplateLength());
                SetCurrentAfterMoonpiePosition(CurrentMoonpiePosition()
                        + moonpie_text_.length
                        + (include_trailing_spaces ?
                        kTrailingSpaces : 0));
            }
        }

        protected boolean NoMoreMoonpies() {
            return CurrentMoonpiePosition() == -1;
        }

        protected int CurrentWeaselPosition() {
            return weasel_positions_[match_index_];
        }

        protected int LastWeaselPosition() {
            return weasel_positions_[match_index_ - 1];
        }

        protected int CurrentMoonpiePosition() {
            return moonpie_positions_[match_index_];
        }

        protected int LastMoonpiePosition() {
            return moonpie_positions_[match_index_ - 1];
        }

        protected int AfterLastWeasel() {
            assertTrue(match_index_ >= 1);
            return after_weasel_[match_index_ - 1];
        }

        protected int AfterPreviousWeasel() {
            assertTrue(match_index_ >= 2);
            return after_weasel_[match_index_ - 2];
        }

        protected int AfterLastMoonpie() {
            assertTrue(match_index_ >= 1);
            return after_moonpie_[match_index_ - 1];
        }

        protected int AfterPreviousMoonpie() {
            assertTrue(match_index_ >= 2);
            return after_moonpie_[match_index_ - 2];
        }

        protected void SetCurrentWeaselPosition(int value) {
            weasel_positions_[match_index_] = value;
        }

        protected void SetCurrentAfterWeaselPosition(int value) {
            after_weasel_[match_index_] = value;
        }

        protected void SetCurrentMoonpiePosition(int value) {
            moonpie_positions_[match_index_] = value;
        }

        protected void SetCurrentAfterMoonpiePosition(int value) {
            after_moonpie_[match_index_] = value;
        }

        // Find the length of the text in between the "weasel" strings in the
        // compressible dictionary, which is the same as the text between
        // the "moon-pie" strings in the compressible target.
        protected int CurrentBoilerplateLength() {
            assertTrue(match_index_ >= 1);
            return CurrentWeaselPosition() - AfterLastWeasel();
        }

        protected int DistanceFromLastWeasel() {
            assertTrue(match_index_ >= 1);
            return CurrentWeaselPosition() - LastWeaselPosition();
        }

        protected int DistanceFromLastMoonpie() {
            assertTrue(match_index_ >= 1);
            return CurrentMoonpiePosition() - LastMoonpiePosition();
        }

        protected int DistanceBetweenLastTwoWeasels() {
            assertTrue(match_index_ >= 2);
            return AfterLastWeasel() - AfterPreviousWeasel();
        }

        protected int DistanceBetweenLastTwoMoonpies() {
            assertTrue(match_index_ >= 2);
            return AfterLastMoonpie() - AfterPreviousMoonpie();
        }

        protected int FindBoilerplateAddressForCopyMode(short copy_mode) {
            int copy_address = 0;
            if (VCDiffAddressCache.IsSelfMode(copy_mode)) {
                copy_address = AfterLastWeasel();
            } else if (default_cache_.IsNearMode(copy_mode)) {
                copy_address = DistanceBetweenLastTwoWeasels();
            } else if (default_cache_.IsSameMode(copy_mode)) {
                copy_address = AfterLastWeasel() % 256;
            }
            return copy_address;
        }

        protected short UpdateCopyModeForMoonpie(short copy_mode) {
            if (copy_mode == default_cache_.FirstSameMode()) {
                return (short) (default_cache_.FirstSameMode() + ((copied_moonpie_address_ / 256) % 3));
            } else {
                return copy_mode;
            }
        }

        protected int FindMoonpieAddressForCopyMode(short copy_mode) {
            int copy_address = 0;
            if (VCDiffAddressCache.IsHereMode(copy_mode)) {
                copy_address = DistanceFromLastMoonpie();
            } else if (default_cache_.IsNearMode(copy_mode)) {
                copy_address = DistanceBetweenLastTwoMoonpies() - kTrailingSpaces;
            } else if (default_cache_.IsSameMode(copy_mode)) {
                copy_address = copied_moonpie_address_ % 256;
            }
            return copy_address;
        }

        // Expect one dictionary instance of "weasel" to be replaced with "moon-pie"
        // in the encoding.
        protected void CopyBoilerplateAndAddMoonpie(short copy_mode, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
            assertFalse(NoMoreMoonpies());
            ExpectCopyForSize(CurrentBoilerplateLength(), copy_mode, actual);
            ExpectAddress(FindBoilerplateAddressForCopyMode(copy_mode), copy_mode, actual);
            ExpectAddInstructionForStringLength(moonpie_text_, actual);
            ExpectDataString(moonpie_text_, actual);
        }

        // Expect one dictionary instance of "weasel" to be replaced with "moon-pie"
        // in the encoding.  The "moon-pie" text will be copied from the previously
        // encoded target.
        protected void CopyBoilerplateAndCopyMoonpie(short copy_mode, short moonpie_copy_mode, ByteBuffer actual) throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
            assertFalse(NoMoreMoonpies());
            ExpectCopyForSize(CurrentBoilerplateLength(), copy_mode, actual);
            ExpectAddress(FindBoilerplateAddressForCopyMode(copy_mode), copy_mode, actual);
            moonpie_copy_mode = UpdateCopyModeForMoonpie(moonpie_copy_mode);
            ExpectCopyForSize(moonpie_text_.length + kTrailingSpaces, moonpie_copy_mode, actual);
            ExpectAddress(FindMoonpieAddressForCopyMode(moonpie_copy_mode), moonpie_copy_mode, actual);
            copied_moonpie_address_ = dictionary_.length + LastMoonpiePosition();
        }

        @Test
        public void EngineEncodeCompressibleNoTargetMatching() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
            Encode(/* interleaved = */ true, /* target matching = */ false);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);

            FindNextMoonpie(false);
            // Expect all five "weasel"s to be replaced with "moon-pie"s
            CopyBoilerplateAndAddMoonpie(default_cache_.FirstSameMode(), actual);
            FindNextMoonpie(false);
            CopyBoilerplateAndAddMoonpie(VCDiffAddressCache.VCD_SELF_MODE, actual);
            FindNextMoonpie(false);
            CopyBoilerplateAndAddMoonpie((short) (VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 1), actual);
            FindNextMoonpie(false);
            CopyBoilerplateAndAddMoonpie((short) (VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 2), actual);
            FindNextMoonpie(false);
            CopyBoilerplateAndAddMoonpie((short) (VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 3), actual);
            FindNextMoonpie(false);
            assertTrue(NoMoreMoonpies());
            ExpectCopyForSize(dictionary_.length - AfterLastWeasel(), VCDiffAddressCache.VCD_FIRST_NEAR_MODE, actual);
            ExpectAddressVarintForSize(DistanceBetweenLastTwoWeasels(), actual);
            VerifySizes(actual);
        }

        @Test
        public void EngineEncodeCompressibleWithTargetMatching() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
            Encode(/* interleaved = */ true, /* target matching = */ true);

            ByteBuffer actual = ByteBuffer.wrap(diff_.toByteArray());
            VerifyHeaderForDictionaryAndTargetText(dictionary_, target_, actual);

            // Expect all five "weasel"s to be replaced with "moon-pie"s.
            // Every "moon-pie" after the first one should be copied from the
            // previously encoded target text.
            FindNextMoonpie(false);
            CopyBoilerplateAndAddMoonpie(default_cache_.FirstSameMode(), actual);
            FindNextMoonpie(true);
            CopyBoilerplateAndCopyMoonpie(VCDiffAddressCache.VCD_SELF_MODE, VCDiffAddressCache.VCD_HERE_MODE, actual);
            FindNextMoonpie(true);
            CopyBoilerplateAndCopyMoonpie((short) (VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 1), default_cache_.FirstSameMode(), actual);
            FindNextMoonpie(true);
            CopyBoilerplateAndCopyMoonpie((short) (VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 3), VCDiffAddressCache.VCD_HERE_MODE, actual);
            FindNextMoonpie(true);
            CopyBoilerplateAndCopyMoonpie((short) (VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 1), default_cache_.FirstSameMode(), actual);
            FindNextMoonpie(true);
            assertTrue(NoMoreMoonpies());
            ExpectCopyForSize(dictionary_.length - AfterLastWeasel(), VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 3, actual);
            ExpectAddressVarintForSize(DistanceBetweenLastTwoWeasels(), actual);
            VerifySizes(actual);
        }
    }
}
