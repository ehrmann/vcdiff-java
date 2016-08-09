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

package com.davidehrmann.vcdiff.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A simpler (non-streaming) interface to the VCDIFF encoder that can be used
 * if the entire target data string is available.
 *
 * @param <OUT> The output type the {@link ../CodeTableWriterInterface} uses
 */
public class VCDiffEncoder<OUT> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffEncoder.class);

    protected final VCDiffStreamingEncoder<OUT> encoder_;

    // This state variable is used to ensure that StartEncoding(), EncodeChunk(),
    // and FinishEncoding() are called in the correct order.  It will be true
    // if StartEncoding() has been called, followed by zero or more calls to
    // EncodeChunk(), but FinishEncoding() has not yet been called.  It will
    // be false initially, and also after FinishEncoding() has been called.
    // TODO: this is never used
    protected boolean encode_chunk_allowed_;

    public VCDiffEncoder(VCDiffStreamingEncoder<OUT> streamingEncoder) {
        this.encoder_ = streamingEncoder;
    }

    /**
     * Replaces old contents of output_string with the encoded form of
     * target_data.
     *
     * @param data
     * @param offset
     * @param length
     * @param out
     * @return
     * @throws IOException
     */
    public boolean Encode(byte[] data, int offset, int length, OUT out) throws IOException {
        if (!encoder_.StartEncoding(out)) {
            return false;
        }
        if (!encoder_.EncodeChunk(data, offset, length, out)) {
            return false;
        }
        return encoder_.FinishEncoding(out);
    }
}
