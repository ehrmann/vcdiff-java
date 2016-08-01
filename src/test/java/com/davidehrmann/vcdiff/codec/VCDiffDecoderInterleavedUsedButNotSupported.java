package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import static org.junit.Assert.*;

public class VCDiffDecoderInterleavedUsedButNotSupported extends VCDiffInterleavedDecoderTestBase {
    public VCDiffDecoderInterleavedUsedButNotSupported() {
        UseStandardFileHeader();
    }

    @Test
    public void DecodeShouldFail() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void DecodeByteByByteShouldFail() throws Exception {
        decoder_.StartDecoding(dictionary_);
        boolean failed = false;
        for (int i = 0; i < delta_file_.length; ++i) {
            if (!decoder_.DecodeChunk(delta_file_, i, 1, output_)) {
                failed = true;
                break;
            }
        }
        assertTrue(failed);
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }
}
