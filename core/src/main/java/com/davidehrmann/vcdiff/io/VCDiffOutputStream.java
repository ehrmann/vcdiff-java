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

package com.davidehrmann.vcdiff.io;

import com.davidehrmann.vcdiff.codec.EncoderBuilder;
import com.davidehrmann.vcdiff.codec.VCDiffStreamingEncoder;
import com.davidehrmann.vcdiff.util.Objects;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class VCDiffOutputStream extends FilterOutputStream {

    private volatile boolean started = false;
    private volatile boolean closed = false;

    private volatile long bytesWritten = 0;

    private final VCDiffStreamingEncoder<OutputStream> encoder;

    public VCDiffOutputStream(OutputStream out, byte[] dictionary, boolean targetMatches,  boolean interleaved, boolean checksum) {
        super(out);
        encoder = EncoderBuilder.builder()
                .withDictionary(dictionary)
                .withTargetMatches(targetMatches)
                .withInterleaving(interleaved)
                .withChecksum(checksum)
                .buildStreaming();
    }

    public VCDiffOutputStream(OutputStream out, VCDiffStreamingEncoder<OutputStream> encoder) {
        super(out);
        this.encoder = Objects.requireNotNull(encoder, "encoder was null");
    }

    @Override
    public void write(int b) throws IOException {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("OutputStream closed");
        }
        if (!started) {
            started = true;
            encoder.startEncoding(super.out);
        }
        try {
            encoder.encodeChunk(b, off, len, super.out);
        } catch (IOException e) {
            throw new IOException("Error trying to encode data chunk at offset " + bytesWritten, e);
        }
        bytesWritten += len;
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            if (!started) {
                started = true;
                encoder.startEncoding(super.out);
            }
            if (!closed) {
                closed = true;
                encoder.finishEncoding(super.out);
            }
        } finally {
            super.close();
        }
    }
}
