package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class VCDiffStandardWindowDecoderTestByteByByte extends VCDiffStandardWindowDecoderTestBase {
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
    public void DecodeExplicitVcdTarget() throws Exception {
        decoder_.SetAllowVcdTarget(true);
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

    @Test
    public void DecodeWithBufferReuse() throws Exception {
        decoder_.StartDecoding(dictionary_);
        byte[] buffer = new byte[1];
        for (byte aDelta_file_ : delta_file_) {
            buffer[0] = aDelta_file_;
            assertTrue(decoder_.DecodeChunk(buffer, 0, 1, output_));
        }
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
