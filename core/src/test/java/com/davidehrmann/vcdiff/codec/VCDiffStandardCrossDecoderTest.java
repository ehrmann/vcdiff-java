package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class VCDiffStandardCrossDecoderTest extends VCDiffStandardCrossDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        decoder_.DecodeChunk(delta_file_, output_);
        decoder_.FinishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
