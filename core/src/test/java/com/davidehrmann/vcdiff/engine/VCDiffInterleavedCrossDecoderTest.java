package com.davidehrmann.vcdiff.engine;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class VCDiffInterleavedCrossDecoderTest extends VCDiffInterleavedCrossDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_file_, output_);
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DecodeWithChecksum() throws Exception {
        ComputeAndAddChecksum();
        InitializeDeltaFile();
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_file_, output_);
        decoder_.finishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
