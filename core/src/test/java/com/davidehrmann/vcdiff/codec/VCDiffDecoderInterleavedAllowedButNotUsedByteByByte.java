package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class VCDiffDecoderInterleavedAllowedButNotUsedByteByByte extends VCDiffDecoderInterleavedAllowedButNotUsedBase {
    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            decoder_.DecodeChunk(delta_file_, i, 1, output_);
        }
        decoder_.FinishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DecodeWithChecksum() throws Exception {
        ComputeAndAddChecksum();
        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        for (int i = 0; i < delta_file_.length; ++i) {
            decoder_.DecodeChunk(delta_file_, i, 1, output_);
        }
        decoder_.FinishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }
}
