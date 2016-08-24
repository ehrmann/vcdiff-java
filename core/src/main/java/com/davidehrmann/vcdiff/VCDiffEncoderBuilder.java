// Copyright 2016 David Ehrmann
// Author: David Ehrmann
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
//
// Definition of an abstract class that describes the interface between the
// encoding engine (which finds the best string matches between the source and
// target data) and the code table writer.  The code table writer is passed a
// series of add, copy, and run instructions and produces an output file in the
// desired format.

package com.davidehrmann.vcdiff;

import com.davidehrmann.vcdiff.engine.HashedDictionary;
import com.davidehrmann.vcdiff.engine.JSONCodeTableWriter;
import com.davidehrmann.vcdiff.engine.VCDiffStreamingEncoderImpl;
import com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriterImpl;
import com.davidehrmann.vcdiff.io.VCDiffOutputStream;

import java.io.OutputStream;
import java.util.EnumSet;

public class VCDiffEncoderBuilder {

    protected boolean interleaved = false;
    protected boolean checksum = false;
    protected boolean targetMatches = true;
    protected byte[] dictionary = null;

    protected VCDiffEncoderBuilder() {

    }

    public synchronized VCDiffEncoderBuilder withChecksum(boolean checksum) {
        this.checksum = checksum;
        return this;
    }

    public synchronized VCDiffEncoderBuilder withInterleaving(boolean interleaved) {
        this.interleaved = interleaved;
        return this;
    }

    public synchronized VCDiffEncoderBuilder withDictionary(byte[] dictionary) {
        this.dictionary = dictionary;
        return this;
    }

    public synchronized VCDiffEncoderBuilder withTargetMatches(boolean targetMatches) {
        this.targetMatches = targetMatches;
        return this;
    }

    public synchronized VCDiffStreamingEncoder<OutputStream> buildStreaming() {
        if (dictionary == null) {
            throw new IllegalArgumentException("dictionary not set");
        }

        EnumSet<VCDiffFormatExtension> formatFlags = EnumSet.noneOf(VCDiffFormatExtension.class);
        if (interleaved) {
            formatFlags.add(VCDiffFormatExtension.GOOGLE_INTERLEAVED);
        }
        if (checksum) {
            formatFlags.add(VCDiffFormatExtension.GOOGLE_CHECKSUM);
        }

        VCDiffCodeTableWriter<OutputStream> coder = new VCDiffCodeTableWriterImpl(interleaved);

        return new VCDiffStreamingEncoderImpl<OutputStream>(
                coder,
                new HashedDictionary(dictionary),
                formatFlags,
                targetMatches
        );
    }

    public VCDiffOutputStream buildOutputStream(OutputStream out) {
        return new VCDiffOutputStream(out, buildStreaming());
    }

    public synchronized VCDiffStreamingEncoder<Appendable> buildStreamingJson() {
        if (dictionary == null) {
            throw new IllegalArgumentException("dictionary not set");
        }
        if (interleaved) {
            throw new IllegalArgumentException("Interleaved not supported with JSON encoder");
        }
        if (checksum) {
            throw new IllegalArgumentException("Checksum not supported with JSON encoder");
        }

        VCDiffCodeTableWriter<Appendable> coder = new JSONCodeTableWriter();

        return new VCDiffStreamingEncoderImpl<Appendable>(
                coder,
                new HashedDictionary(dictionary),
                EnumSet.noneOf(VCDiffFormatExtension.class),
                targetMatches
        );
    }

    public VCDiffEncoder<OutputStream> buildSimple() {
        return new VCDiffEncoder<OutputStream>(buildStreaming());
    }

    public VCDiffEncoder<Appendable> buildSimpleJson() {
        return new VCDiffEncoder<Appendable>(buildStreamingJson());
    }

    public static VCDiffEncoderBuilder builder() {
        return new VCDiffEncoderBuilder();
    }
}
