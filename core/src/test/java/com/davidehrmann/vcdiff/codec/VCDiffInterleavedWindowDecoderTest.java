package com.davidehrmann.vcdiff.codec;

import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertArrayEquals;

// Divides up the interleaved encoding into eight separate delta file windows.
public class VCDiffInterleavedWindowDecoderTest extends VCDiffInterleavedWindowDecoderTestBase {
    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        decoder_.DecodeChunk(delta_file_, 0, delta_file_.length, output_);
        decoder_.FinishDecoding();
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DecodeInTwoParts() throws Exception {
        final int delta_file_size = delta_file_.length;
        for (int i = 1; i < delta_file_size; i++) {
            ByteArrayOutputStream output_chunk1 = new ByteArrayOutputStream();
            ByteArrayOutputStream output_chunk2 = new ByteArrayOutputStream();
            decoder_.StartDecoding(dictionary_);
            decoder_.DecodeChunk(delta_file_, 0, i, output_chunk1);
            decoder_.DecodeChunk(delta_file_, i, delta_file_size - i, output_chunk2);
            decoder_.FinishDecoding();
            assertArrayEquals(expected_target_, ArraysExtra.concat(output_chunk1.toByteArray(), output_chunk2.toByteArray()));
        }
    }

    @Test
    public void DecodeInThreeParts() throws Exception {
        final int delta_file_size = delta_file_.length;
        for (int i = 1; i < delta_file_size - 1; i++) {
            for (int j = i + 1; j < delta_file_size; j++) {
                ByteArrayOutputStream output_chunk1 = new ByteArrayOutputStream();
                ByteArrayOutputStream output_chunk2 = new ByteArrayOutputStream();
                ByteArrayOutputStream output_chunk3 = new ByteArrayOutputStream();
                decoder_.StartDecoding(dictionary_);
                decoder_.DecodeChunk(delta_file_, 0, i, output_chunk1);
                decoder_.DecodeChunk(delta_file_, i, j - i, output_chunk2);
                decoder_.DecodeChunk(delta_file_, j, delta_file_size - j, output_chunk3);
                decoder_.FinishDecoding();
                assertArrayEquals(
                        expected_target_,
                        ArraysExtra.concat(output_chunk1.toByteArray(), output_chunk2.toByteArray(), output_chunk3.toByteArray())
                );
            }
        }
    }


}
