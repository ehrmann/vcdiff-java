package com.davidehrmann.vcdiff.engine;

import org.junit.Assert;
import org.junit.Test;

public class VCDiffStandardCrossDecoderTest extends VCDiffStandardCrossDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_file_, output_);
        decoder_.finishDecoding();
        Assert.assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
