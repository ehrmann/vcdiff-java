package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class VCDiffStandardCrossDecoderTest extends VCDiffStandardCrossDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_file_, output_);
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
