package com.googlecode.jvcdiff.codec;

import org.junit.Test;

import java.util.Arrays;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_TARGET;
import static org.junit.Assert.*;

public class VCDiffStandardDecoderTestByteByByte extends VCDiffDecoderTest {

    private static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x79,  // Length of the delta encoding
            FirstByteOfStringLength(kExpectedTarget),  // Size of the target window
            SecondByteOfStringLength(kExpectedTarget),
            0x00,  // Delta_indicator (no compression)
            0x64,  // length of data for ADDs and RUNs
            0x0C,  // length of instructions section
            0x03  // length of addresses for COPYs
    };

    private static final byte[] kWindowBody = {
            // Data for ADDs: 1st section (length 61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            // Data for ADDs: 2nd section (length 2)
            'h', 'r',
            // Data for ADDs: 3rd section (length 9)
            'W', 'h', 'a', 't', ' ',
            'I', ' ', 't', 'e',
            // Data for RUN: 4th section (length 1)
            'l',
            // Data for ADD: 4th section (length 27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n',
            // Instructions and sizes (length 13)
            0x13,         // VCD_COPY mode VCD_SELF, size 0
            0x1C,         // Size of COPY (28)
            0x01,         // VCD_ADD size 0
            0x3D,         // Size of ADD (61)
            0x23,         // VCD_COPY mode VCD_HERE, size 0
            0x2C,         // Size of COPY (44)
            (byte) 0xCB,  // VCD_ADD size 2 + VCD_COPY mode NEAR(1), size 5
            0x0A,         // VCD_ADD size 9
            0x00,         // VCD_RUN size 0
            0x02,         // Size of RUN (2)
            0x01,         // VCD_ADD size 0
            0x1B,         // Size of ADD (27)
            // Addresses for COPYs (length 3)
            0x00,         // Start of dictionary
            0x58,         // HERE mode address for 2nd copy (27+61 back from here_address)
            0x2D          // NEAR(1) mode address for 2nd copy (45 after prior address)
    };

    public VCDiffStandardDecoderTestByteByByte() {
        UseStandardFileHeader();
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
    }

    @Test
    public void DecodeHeaderOnly() throws Exception {
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_header_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_header_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DecodeNoVcdTarget() throws Exception {
        decoder_.SetAllowVcdTarget(false);
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    // Remove one byte from the length of the chunk to process, and
    // verify that an error is returned for FinishDecoding().
    @Test
    public void FinishAfterDecodingPartialWindow() throws Exception {
        delta_file_ = Arrays.copyOf(delta_file_, delta_file_.length - 1);
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertFalse(decoder_.FinishDecoding());
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void FinishAfterDecodingPartialWindowHeader() throws Exception {
        delta_file_ = Arrays.copyOf(delta_file_ , delta_file_header_.length + delta_window_header_.length - 1);
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertFalse(decoder_.FinishDecoding());
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    // If we add a checksum to a standard-format delta file (without using format
    // extensions), it will be interpreted as random bytes inserted into the middle
    // of the file.  The decode operation should fail, but where exactly it fails is
    // undefined.
    @Test
    public void StandardFormatDoesNotSupportChecksum() throws Exception {
        ComputeAndAddChecksum();
        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void TargetMatchesWindowSizeLimit() throws Exception {
        decoder_.SetMaximumTargetWindowSize(expected_target_.length);
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void TargetMatchesFileSizeLimit() throws Exception {
        decoder_.SetMaximumTargetFileSize(expected_target_.length);
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void TargetExceedsWindowSizeLimit() throws Exception {
        decoder_.SetMaximumTargetWindowSize(expected_target_.length - 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetExceedsFileSizeLimit() throws Exception {
        decoder_.SetMaximumTargetFileSize(expected_target_.length - 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    // Fuzz bits to make sure decoder does not violently crash.
// This test has no expected behavior except that no crashes should occur.
// In some cases, changing bits will still decode to the correct target;
// for example, changing unused bits within a bitfield.
    @Test
    public void FuzzBits() throws Exception {
        while (FuzzOneByteInDeltaFile()) {
            decoder_.StartDecoding(dictionary_);
            boolean failed = false;
            for (int i = 0; i < delta_file_.length; ++i) {
                if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                decoder_.FinishDecoding();
            }
            // The decoder should not create more target bytes than were expected.
            assertTrue(expected_target_.length >= output_.size());
            InitializeDeltaFile();
            output_.reset();
        }
    }

    // Change each element of the delta file window to an erroneous value
// and make sure it's caught as an error.
    @Test
    public void WinIndicatorHasBothSourceAndTarget() throws Exception {
        delta_file_[delta_file_header_.length] = VCD_SOURCE + VCD_TARGET;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail at the position that was altered
                assertEquals(delta_file_header_.length, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void OkayToSetUpperBitsOfWinIndicator() throws Exception {
        // It is not an error to set any of the other bits in Win_Indicator
        // besides VCD_SOURCE and VCD_TARGET.
        delta_file_[delta_file_header_.length] = (byte) 0xFD;
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void CopyInstructionsShouldFailIfNoSourceSegment() throws Exception {
        // Replace the Win_Indicator and the source size and source offset with a
        // single 0 byte (a Win_Indicator for a window with no source segment.)
        delta_window_header_ = ArraysExtra.replace(delta_window_header_, 0, 4, new byte[1]);

        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // The first COPY instruction should fail.  With the standard format,
                // it may need to see the whole delta window before knowing that it is
                // invalid.
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentSizeExceedsDictionarySize() throws Exception {
        ++delta_file_[delta_file_header_.length + 2];  // increment size
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment size
                assertEquals(delta_file_header_.length + 2, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(1, 2);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment size
                assertEquals(delta_file_header_.length + 5, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(1, 2);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment size
                assertEquals(delta_file_header_.length + 4, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(1, 2);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment size
                assertTrue(delta_file_header_.length + 6 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentEndExceedsDictionarySize() throws Exception {
        ++delta_file_[delta_file_header_.length + 3];  // increment start pos
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment end
                assertEquals(delta_file_header_.length + 3, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentPosMaxInt() throws Exception {
        WriteMaxVarintAtOffset(3, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment pos
                assertEquals(delta_file_header_.length + 7, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentPosNegative() throws Exception {
        WriteNegativeVarintAtOffset(3, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment pos
                assertEquals(delta_file_header_.length + 6, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SourceSegmentPosInvalid() throws Exception {
        WriteInvalidVarintAtOffset(3, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the source segment pos
                assertTrue(delta_file_header_.length + 8 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DeltaEncodingLengthZero() throws Exception {
        delta_file_[delta_file_header_.length + 4] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DeltaEncodingLengthTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 4];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DeltaEncodingLengthTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 4];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DeltaEncodingLengthMaxInt() throws Exception {
        WriteMaxVarintAtOffset(4, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail before finishing the window header
                assertTrue(delta_file_header_.length + delta_window_header_.length + 4 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DeltaEncodingLengthNegative() throws Exception {
        WriteNegativeVarintAtOffset(4, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the delta encoding length
                assertEquals(delta_file_header_.length + 7, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DeltaEncodingLengthInvalid() throws Exception {
        WriteInvalidVarintAtOffset(4, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the delta encoding length
                assertTrue(delta_file_header_.length + 9 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetWindowSizeZero() throws Exception {
        byte[] zero_size = { 0x00 };
        delta_file_ = ArraysExtra.replace(delta_file_, delta_file_header_.length + 5, 2, zero_size);

        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetWindowSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 6];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void TargetWindowSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 6];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void TargetWindowSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(5, 2);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the target window size
                assertEquals(delta_file_header_.length + 9, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetWindowSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(5, 2);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the target window size
                assertEquals(delta_file_header_.length + 8, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetWindowSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(5, 2);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the target window size
                assertTrue(delta_file_header_.length + 10 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }


    @Test
    public void OkayToSetUpperBitsOfDeltaIndicator() throws Exception {
        delta_file_[delta_file_header_.length + 7] = (byte) 0xF8;
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DataCompressionNotSupported() throws Exception {
        delta_file_[delta_file_header_.length + 7] = 0x01;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the delta indicator
                assertEquals(delta_file_header_.length + 7, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionCompressionNotSupported() throws Exception {
        delta_file_[delta_file_header_.length + 7] = 0x02;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the delta indicator
                assertEquals(delta_file_header_.length + 7, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddressCompressionNotSupported() throws Exception {
        delta_file_[delta_file_header_.length + 7] = 0x04;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the delta indicator
                assertEquals(delta_file_header_.length + 7, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddRunDataSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 8] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddRunDataSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 8];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddRunDataSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 8];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddRunDataSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(8, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail before finishing the window header
                assertTrue(delta_file_header_.length + delta_window_header_.length + 4 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddRunDataSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(8, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the add/run data segment size
                assertEquals(delta_file_header_.length + 11, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddRunDataSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(8, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the add/run data segment size
                assertTrue(delta_file_header_.length + 13 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 9] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 9];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 9];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(9, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail before finishing the window header
                assertTrue(delta_file_header_.length + delta_window_header_.length + 4 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(9, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the instructions segment size
                assertEquals(delta_file_header_.length + 12, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(9, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the instructions segment size
                assertTrue(delta_file_header_.length + 14 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 10] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 10];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 10];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 10, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(10, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 14, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(10, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertEquals(delta_file_header_.length + 13, i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(10, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                // It should fail after decoding the copy address segment size
                assertTrue(delta_file_header_.length + 15 >= i);
                break;
            }
        }
        assertTrue(failed);
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void InstructionsEndEarly() throws Exception {
        --delta_file_[delta_file_header_.length + 9];
        ++delta_file_[delta_file_header_.length + 10];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

// From this point on, the tests should also be run against the interleaved
// format.

    @Test
    public void CopyMoreThanExpectedTarget() throws Exception {
        delta_file_[delta_file_header_.length + 0x70] = FirstByteOfStringLength(kExpectedTarget);
        delta_file_[delta_file_header_.length + 0x71] = (byte) (SecondByteOfStringLength(kExpectedTarget) + 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopySizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 0x70] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopySizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 0x70];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopySizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 0x70];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopySizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x70, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopySizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x70, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopySizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x70, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopyAddressBeyondHereAddress() throws Exception {
        delta_file_[delta_file_header_.length + 0x7B] =
                FirstByteOfStringLength(kDictionary);
        delta_file_[delta_file_header_.length + 0x7C] =
                SecondByteOfStringLength(kDictionary);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopyAddressMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x7B, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopyAddressNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x70, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void CopyAddressInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x70, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddMoreThanExpectedTarget() throws Exception {
        delta_file_[delta_file_header_.length + 0x72] =
                FirstByteOfStringLength(kExpectedTarget);
        delta_file_[delta_file_header_.length + 0x73] = (byte) (SecondByteOfStringLength(kExpectedTarget) + 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 0x72] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 0x72];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 0x72];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x72, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x72, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void AddSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x72, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunMoreThanExpectedTarget() throws Exception {
        delta_file_[delta_file_header_.length + 0x78] = FirstByteOfStringLength(kExpectedTarget);
        delta_file_[delta_file_header_.length + 0x79] = (byte) (SecondByteOfStringLength(kExpectedTarget) + 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 0x78] = 0;
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 0x78];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 0x78];
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x78, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x78, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void RunSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x78, 1);
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

}
