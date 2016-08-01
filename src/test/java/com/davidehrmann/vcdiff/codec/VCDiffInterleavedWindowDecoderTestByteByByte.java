package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

public class VCDiffInterleavedWindowDecoderTestByteByByte extends VCDiffInterleavedWindowDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    // Windows 3 and 4 use the VCD_TARGET flag, so decoder should signal an error.
    @Test 
    public void DecodeNoVcdTarget() throws Exception {
        decoder_.SetAllowVcdTarget(false);
        decoder_.StartDecoding(dictionary_);
        int i = 0;
        for (; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                break;
            }
        }
        // The failure should occur just at the position of the first VCD_TARGET.
        assertEquals(delta_file_header_.length + 83, i);
        // The target data for the first two windows should have been output.
        assertArrayEquals(Arrays.copyOf(expected_target_, 89), output_.toByteArray());
    }

    // The original version of VCDiffDecoder did not allow the caller to modify the
    // contents of output_string between calls to DecodeChunk().  That restriction
    // has been removed.  Verify that the same result is still produced if the
    // output string is cleared after each call to DecodeChunk().  Use the window
    // encoding because it refers back to the previously decoded target data, which
    // is the feature that would fail if the restriction still applied.
    //
    @Test
    public void OutputStringCanBeModified() throws Exception {
        ByteArrayOutputStream temp_output = new ByteArrayOutputStream();
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, temp_output));
            output_.write(temp_output.toByteArray());
            temp_output.reset();
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void OutputStringIsPreserved() throws Exception {
        final byte[] previous_data = "Previous data".getBytes(US_ASCII);
        output_.write(previous_data);
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            assertTrue(decoder_.DecodeChunk(delta_file_, i, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(ArraysExtra.concat(previous_data, expected_target_), output_.toByteArray());
    }
}
