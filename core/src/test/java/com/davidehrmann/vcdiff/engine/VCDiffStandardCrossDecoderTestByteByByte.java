package com.davidehrmann.vcdiff.engine;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class VCDiffStandardCrossDecoderTestByteByByte extends VCDiffStandardCrossDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.startDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            decoder_.decodeChunk(delta_file_, i, 1, output_);
        }
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
