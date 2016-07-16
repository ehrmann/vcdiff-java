package com.googlecode.jvcdiff.codec;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class VCDiffDecoderInterleavedAllowedButNotUsed extends VCDiffDecoderInterleavedAllowedButNotUsedBase {
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

    @Test
    public void DecodeWithChecksum() throws Exception {
        ComputeAndAddChecksum();
        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
