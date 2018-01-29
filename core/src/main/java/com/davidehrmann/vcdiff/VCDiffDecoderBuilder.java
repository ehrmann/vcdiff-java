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

import com.davidehrmann.vcdiff.engine.VCDiffStreamingDecoderImpl;
import com.davidehrmann.vcdiff.io.VCDiffInputStream;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class VCDiffDecoderBuilder {

    protected long maximumTargetFileSize = -1;
    protected int maximumTargetWindowSize = -1;
    protected boolean allowTargetMatches = true;

    protected VCDiffDecoderBuilder() {

    }

    public synchronized VCDiffDecoderBuilder withMaxTargetFileSize(long maximumTargetFileSize) {
        if (maximumTargetFileSize < 0) {
            throw new IllegalArgumentException("maximumTargetFileSize can't be negative");
        }
        this.maximumTargetFileSize = maximumTargetFileSize;
        return this;
    }

    public synchronized VCDiffDecoderBuilder withMaxTargetWindowSize(int maximumTargetWindowSize) {
        if (maximumTargetWindowSize < 0) {
            throw new IllegalArgumentException("maximumTargetWindowSize can't be negative");
        }
        this.maximumTargetWindowSize = maximumTargetWindowSize;
        return this;
    }

    public synchronized VCDiffDecoderBuilder withAllowTargetMatches(boolean allowTargetMatches) {
        this.allowTargetMatches = allowTargetMatches;
        return this;
    }

    public VCDiffStreamingDecoder buildStreaming() {
        return buildStreaming(new VCDiffStreamingDecoderImpl());
    }

    public synchronized VCDiffStreamingDecoder buildStreaming(VCDiffStreamingDecoder decoder) {
        if (maximumTargetFileSize >= 0) {
            decoder.setMaximumTargetFileSize(maximumTargetFileSize);
        }
        if (maximumTargetWindowSize >= 0) {
            decoder.setMaximumTargetWindowSize(maximumTargetWindowSize);
        }
        decoder.setAllowVcdTarget(allowTargetMatches);
        return decoder;
    }

    public VCDiffInputStream buildInputStream(InputStream in, byte[] dictionary) {
        return buildInputStream(in, ByteBuffer.wrap(dictionary));
    }

    public VCDiffInputStream buildInputStream(InputStream in, ByteBuffer dictionary) {
        return new VCDiffInputStream(in, dictionary, buildStreaming());
    }

    public VCDiffDecoder buildSimple() {
        return new VCDiffDecoder(buildStreaming());
    }

    public VCDiffDecoder buildSimple(VCDiffStreamingDecoder decoder) {
        return new VCDiffDecoder(buildStreaming(decoder));
    }

    public static VCDiffDecoderBuilder builder() {
        return new VCDiffDecoderBuilder();
    }
}
