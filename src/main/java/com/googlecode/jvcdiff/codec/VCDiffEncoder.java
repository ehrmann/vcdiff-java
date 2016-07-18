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

package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.CodeTableWriterInterface;
import com.googlecode.jvcdiff.JSONCodeTableWriter;
import com.googlecode.jvcdiff.google.VCDiffFormatExtensionFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;

import static com.googlecode.jvcdiff.google.VCDiffFormatExtensionFlag.VCD_FORMAT_JSON;
import static com.googlecode.jvcdiff.google.VCDiffFormatExtensionFlag.VCD_STANDARD_FORMAT;

// A simpler (non-streaming) interface to the VCDIFF encoder that can be used
// if the entire target data string is available.
//
public class VCDiffEncoder<OUT> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseVCDiffStreamingEncoder.class);

    protected final EnumSet<VCDiffFormatExtensionFlag> format_extensions_;

    // Determines whether to look for matches within the previously encoded
    // target data, or just within the source (dictionary) data.  Please see
    // vcencoder.h for a full explanation of this parameter.
    protected final boolean look_for_target_matches_;

    protected final CodeTableWriterInterface<OUT> coder_;

    protected final HashedDictionary dictionary_;
    protected volatile VCDiffStreamingEncoder<OUT> encoder_;

    // This state variable is used to ensure that StartEncoding(), EncodeChunk(),
    // and FinishEncoding() are called in the correct order.  It will be true
    // if StartEncoding() has been called, followed by zero or more calls to
    // EncodeChunk(), but FinishEncoding() has not yet been called.  It will
    // be false initially, and also after FinishEncoding() has been called.
    protected boolean encode_chunk_allowed_;

    public VCDiffEncoder(CodeTableWriterInterface<OUT> coder,
                         byte[] dictionary_contents) {
        this(coder, dictionary_contents, EnumSet.of(VCD_STANDARD_FORMAT), true);
    }

    public VCDiffEncoder(CodeTableWriterInterface<OUT> coder,
                         byte[] dictionary_contents,
                         EnumSet<VCDiffFormatExtensionFlag> format_extensions) {
        this(coder, dictionary_contents, format_extensions, true);
    }

    public VCDiffEncoder(CodeTableWriterInterface<OUT> coder,
                         byte[] dictionary_contents,
                         boolean look_for_target_matches) {
        this(coder, dictionary_contents, EnumSet.of(VCD_STANDARD_FORMAT), look_for_target_matches);
    }

    public VCDiffEncoder(CodeTableWriterInterface<OUT> coder,
                         byte[] dictionary_contents,
                         EnumSet<VCDiffFormatExtensionFlag> format_extensions,
                         boolean look_for_target_matches) {
        this.look_for_target_matches_ = look_for_target_matches;
        this.format_extensions_ = format_extensions;
        this.dictionary_ = new HashedDictionary(dictionary_contents);
        this.coder_ = coder;

        if (format_extensions.contains(VCD_FORMAT_JSON) && !(coder instanceof JSONCodeTableWriter)) {
            throw new IllegalArgumentException(VCD_FORMAT_JSON +
                    " specified in format_extensions, but coder wasn't a " +
                    JSONCodeTableWriter.class.getSimpleName()
            );
        }
    }

    // Replaces old contents of output_string with the encoded form of
    // target_data.
    public boolean Encode(byte[] data, int offset, int length, OUT out) throws IOException {
        if (encoder_ == null) {
            encoder_ = new BaseVCDiffStreamingEncoder<OUT>(
                    coder_,
                    dictionary_,
                    format_extensions_,
                    look_for_target_matches_
            );
        }
        if (!encoder_.StartEncoding(out)) {
            return false;
        }
        if (!encoder_.EncodeChunk(data, offset, length, out)) {
            return false;
        }
        return encoder_.FinishEncoding(out);
    }
}
