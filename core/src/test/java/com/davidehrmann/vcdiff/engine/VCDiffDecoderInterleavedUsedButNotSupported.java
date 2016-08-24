package com.davidehrmann.vcdiff.engine;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class VCDiffDecoderInterleavedUsedButNotSupported extends VCDiffInterleavedDecoderTestBase {
    public VCDiffDecoderInterleavedUsedButNotSupported() {
        UseStandardFileHeader();
    }

    @Test
    public void DecodeShouldFail() throws Exception {
        decoder_.startDecoding(dictionary_);
        try {
            thrown.expect(IOException.class);
            decoder_.decodeChunk(delta_file_, output_);
        } finally {
            assertArrayEquals(new byte[0], output_.toByteArray());
        }
    }

    @Test
    public void DecodeByteByByteShouldFail() throws Exception {
        decoder_.startDecoding(dictionary_);
        boolean failed = false;
        try {
            for (int i = 0; i < delta_file_.length; ++i) {
                decoder_.decodeChunk(delta_file_, i, 1, output_);
            }

            fail();
        } catch (IOException ignored) {
        } finally {
            // The decoder should not create more target bytes than were expected.
            assertTrue(expected_target_.length >= output_.size());
        }
    }
}
