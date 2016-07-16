package com.googlecode.jvcdiff.codec;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_TARGET;
import static org.junit.Assert.*;

public class VCDiffStandardWindowDecoderTest extends VCDiffStandardWindowDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    // Bug 1287926: If DecodeChunk() stops in the middle of the window header,
    // and the expected size of the current target window is smaller than the
    // cumulative target bytes decoded so far, an underflow occurs and the decoder
    // tries to allocate ~MAX_INT bytes.
    @Test
    public void DecodeBreakInFourthWindowHeader() throws Exception {
        // Parse file header + first two windows.
        final int chunk_1_size = delta_file_header_.length + 83;
        // Parse third window, plus everything up to "Size of the target window" field
        // of fourth window, but do not parse complete header of fourth window.
        final int chunk_2_size = 12 + 5;
        // TODO: was CHECK_EQ
        assertEquals(VCD_TARGET, (delta_file_[chunk_1_size]));
        assertEquals(0x00, delta_file_[chunk_1_size + chunk_2_size]);
        ByteArrayOutputStream output_chunk1 = new ByteArrayOutputStream();
        ByteArrayOutputStream output_chunk2 = new ByteArrayOutputStream();
        ByteArrayOutputStream output_chunk3 = new ByteArrayOutputStream();

        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                chunk_1_size,
                output_chunk1));
        assertTrue(decoder_.DecodeChunk(delta_file_,
                chunk_1_size,
                chunk_2_size,
                output_chunk2));
        assertTrue(decoder_.DecodeChunk(delta_file_,
                chunk_1_size + chunk_2_size,
                delta_file_.length - (chunk_1_size + chunk_2_size),
                output_chunk3));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_,
                ArraysExtra.concat(output_chunk1.toByteArray(), output_chunk2.toByteArray(), output_chunk3.toByteArray()));
    }

    @Test
    public void DecodeChunkNoVcdTargetAllowed() throws Exception {
        decoder_.SetAllowVcdTarget(false);
        // Parse file header + first two windows.
        final int chunk_1_size = delta_file_header_.length + 83;
        // The third window begins with Win_Indicator = VCD_TARGET which is not
        // allowed.
        // TODO: CHECK_EQ
        assertEquals(VCD_TARGET, delta_file_[chunk_1_size]);
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_, 0, chunk_1_size, output_));
        // Just parsing one more byte (the VCD_TARGET) should result in an error.
        assertFalse(decoder_.DecodeChunk(delta_file_, chunk_1_size, 1, output_));
        // The target data for the first two windows should have been output.
        assertArrayEquals(Arrays.copyOf(expected_target_, 89), output_.toByteArray());
    }

    @Test
    public void DecodeInTwoParts() throws Exception {
        final int delta_file_size = delta_file_.length;
        for (int i = 1; i < delta_file_size; i++) {
            ByteArrayOutputStream output_chunk1 = new ByteArrayOutputStream();
            ByteArrayOutputStream output_chunk2 = new ByteArrayOutputStream();
            decoder_.StartDecoding(dictionary_);
            assertTrue(decoder_.DecodeChunk(delta_file_,
                    0,
                    i,
                    output_chunk1));
            assertTrue(decoder_.DecodeChunk(delta_file_,
                    i,
                    delta_file_size - i,
                    output_chunk2));
            assertTrue(decoder_.FinishDecoding());
            assertArrayEquals(expected_target_, ArraysExtra.concat(output_chunk1.toByteArray(), output_chunk2.toByteArray()));
        }
    }

    @Test
    public void DecodeInThreeParts() throws Exception {
        final int delta_file_size = delta_file_.length;
        for (int i = 1; i < delta_file_size - 1; i++) {
            for (int j = i + 1; j < delta_file_size; j++) {
                ByteArrayOutputStream output_chunk1 = new ByteArrayOutputStream();
                ByteArrayOutputStream output_chunk2 = new ByteArrayOutputStream();
                ByteArrayOutputStream output_chunk3 = new ByteArrayOutputStream();

                decoder_.StartDecoding(dictionary_);
                assertTrue(decoder_.DecodeChunk(delta_file_,
                        0,
                        i,
                        output_chunk1));
                assertTrue(decoder_.DecodeChunk(delta_file_,
                        i,
                        j - i,
                        output_chunk2));
                assertTrue(decoder_.DecodeChunk(delta_file_,
                        j,
                        delta_file_size - j,
                        output_chunk3));
                assertTrue(decoder_.FinishDecoding());
                assertArrayEquals(expected_target_,
                        ArraysExtra.concat(output_chunk1.toByteArray(), output_chunk2.toByteArray(), output_chunk3.toByteArray()));
            }
        }
    }

    // For the window test, the maximum target window size is much smaller than the
    // target file size.  (The largest window is Window 2, with 61 target bytes.)
    // Use the minimum values possible.
    @Test
    public void TargetMatchesWindowSizeLimit() throws Exception {
        decoder_.SetMaximumTargetWindowSize(kWindow2Size);
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void TargetMatchesFileSizeLimit() throws Exception {
        decoder_.SetMaximumTargetFileSize(expected_target_.length);
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void TargetExceedsWindowSizeLimit() throws Exception {
        decoder_.SetMaximumTargetWindowSize(kWindow2Size - 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetExceedsFileSizeLimit() throws Exception {
        decoder_.SetMaximumTargetFileSize(expected_target_.length - 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }
}
