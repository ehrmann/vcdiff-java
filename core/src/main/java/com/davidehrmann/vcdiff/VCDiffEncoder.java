// Copyright 2007-2016 Google Inc., David Ehrmann
// Author: Lincoln Smith, David Ehrmann
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.davidehrmann.vcdiff;

import com.davidehrmann.vcdiff.util.Objects;

import java.io.IOException;

/**
 * A simpler (non-streaming) interface to the VCDIFF encoder that can be used
 * if the entire target data string is available.
 *
 * @param <OUT> The output type the {@link VCDiffCodeTableWriter} uses
 */
public class VCDiffEncoder<OUT> {

    private final VCDiffStreamingEncoder<OUT> encoder;

    public VCDiffEncoder(VCDiffStreamingEncoder<OUT> streamingEncoder) {
        this.encoder = Objects.requireNotNull(streamingEncoder, "encoder was null");
    }

    /**
     * Replaces old contents of output_string with the encoded form of
     * target_data.
     *
     * @param data data to encode
     * @param offset offset into the data array to encode from
     * @param length number of bytes from the array to encode
     * @param out writer to write encoded data to
     * @throws IOException if an exception occurs in the encoder or writing to the output writer
     */
    public void encode(byte[] data, int offset, int length, OUT out) throws IOException {
        encoder.startEncoding(out);
        encoder.encodeChunk(data, offset, length, out);
        encoder.finishEncoding(out);
    }

    /**
     * This is a convenience method that's equivalent to encode(data, 0, data.length, out)
     *
     * @param data data to encode
     * @param out writer to write encoded data to
     * @throws IOException if an exception occurs in the encoder or writing to the output writer
     */
    public void encode(byte[] data, OUT out) throws IOException {
        encode(data, 0, data.length, out);
    }
}
