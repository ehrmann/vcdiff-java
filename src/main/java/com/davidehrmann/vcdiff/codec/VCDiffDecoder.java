package com.davidehrmann.vcdiff.codec;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A simpler (non-streaming) interface to the VCDIFF decoder that can be used
 * if the entire delta file is available.
 */
public class VCDiffDecoder {
    private final VCDiffStreamingDecoder decoder_;

    public VCDiffDecoder(VCDiffStreamingDecoder decoder) {
        if (decoder == null) {
            throw new NullPointerException("decoder was null");
        }
        this.decoder_ = decoder;
    }

    /**
     * Replaces old contents of "*target" with the result of decoding
     * the bytes found in "encoding."
     *
     * Returns true if "encoding" was a well-formed sequence of
     * instructions, and returns false if not.
     * @throws IOException
     */
    public boolean Decode(byte[] dictionary, byte[] encoding, int offset, int len, OutputStream target) throws IOException {
        decoder_.StartDecoding(dictionary);
        return decoder_.DecodeChunk(encoding, offset, len, target) && decoder_.FinishDecoding();
    }
}
