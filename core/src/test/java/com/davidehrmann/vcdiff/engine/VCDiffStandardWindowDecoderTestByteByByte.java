package com.davidehrmann.vcdiff.engine;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class VCDiffStandardWindowDecoderTestByteByByte extends VCDiffStandardWindowDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.startDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            decoder_.decodeChunk(delta_file_, i, 1, output_);
        }
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DecodeExplicitVcdTarget() throws Exception {
        decoder_.setAllowVcdTarget(true);
        decoder_.startDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            decoder_.decodeChunk(delta_file_, i, 1, output_);
        }
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    // Windows 3 and 4 use the VCD_TARGET flag, so decoder should signal an error.
    @Test
    public void DecodeNoVcdTarget() throws Exception {
        decoder_.setAllowVcdTarget(false);
        decoder_.startDecoding(dictionary_);
        int i = 0;
        for (; i < delta_file_.length; ++i) {
            try {
                decoder_.decodeChunk(delta_file_, i, 1, output_);
            } catch (IOException e) {
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
        decoder_.startDecoding(dictionary_);
        byte[] buffer = new byte[1];
        for (byte aDelta_file_ : delta_file_) {
            buffer[0] = aDelta_file_;
            decoder_.decodeChunk(buffer, output_);
        }
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
