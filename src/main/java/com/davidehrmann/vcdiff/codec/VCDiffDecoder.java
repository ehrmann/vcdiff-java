package com.davidehrmann.vcdiff.codec;

import com.davidehrmann.vcdiff.util.Objects;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A simpler (non-streaming) interface to the VCDIFF decoder that can be used
 * if the entire delta file is available.
 */
public class VCDiffDecoder {
    private final VCDiffStreamingDecoder decoder_;

    public VCDiffDecoder(VCDiffStreamingDecoder decoder) {
        this.decoder_ = Objects.requireNotNull(decoder, "decoder was null");
    }

    /**
     * Replaces old contents of "*target" with the result of decoding
     * the bytes found in "encoding."
     *
     * @throws IOException
     */
    public void Decode(byte[] dictionary, byte[] encoding, int offset, int len, OutputStream target) throws IOException {
        decoder_.StartDecoding(dictionary);
        decoder_.DecodeChunk(encoding, offset, len, target);
        decoder_.FinishDecoding();
    }

    public void Decode(byte[] dictionary, byte[] encoding, OutputStream target) throws IOException {
        Decode(dictionary, encoding, 0, encoding.length, target);
    }
}
