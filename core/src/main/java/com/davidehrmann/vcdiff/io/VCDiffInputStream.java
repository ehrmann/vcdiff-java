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

import com.davidehrmann.vcdiff.codec.DecoderBuilder;
import com.davidehrmann.vcdiff.codec.VCDiffStreamingDecoder;
import com.davidehrmann.vcdiff.util.Objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class VCDiffInputStream extends InputStream {

    public static final int DEFAULT_MAX_TARGET_FILE_SIZE = 1 << 26;
    public static final int DEFAULT_MAX_TARGET_WINDOW_SIZE = 1 << 26;
    public static final boolean DEFAULT_ALLOW_VCD_TARGET = false;

    private final VCDiffStreamingDecoder decoder;
    private final byte[] dictionary;

    private final InputStream in;

    private final byte[] inBuffer = new byte[4096];
    private volatile long totalBytesRead = 0;
    private volatile ByteBuffer decodedBuffer = ByteBuffer.allocate(0);
    private final ByteArrayOutputStream tempDecoded = new ByteArrayOutputStream();

    private volatile boolean decodingStarted = false;
    private volatile boolean closed = false;

    public VCDiffInputStream(InputStream in, byte[] dictionary) {
        this(in, dictionary, DEFAULT_MAX_TARGET_FILE_SIZE, DEFAULT_MAX_TARGET_WINDOW_SIZE,
                DEFAULT_ALLOW_VCD_TARGET);
    }

    public VCDiffInputStream(InputStream in, byte[] dictionary,
                             long maxTargetFileSize, int maxTargetWindowSize, boolean allowVcdTarget) {
        this.in = Objects.requireNotNull(in, "in was null");
        this.dictionary = Objects.requireNotNull(dictionary, "dictionary was null").clone();
        decoder = DecoderBuilder.builder()
                .withMaxTargetFileSize(maxTargetFileSize)
                .withMaxTargetWindowSize(maxTargetWindowSize)
                .withAllowTargetMatches(allowVcdTarget)
                .buildStreaming();
    }

    public VCDiffInputStream(InputStream in, byte[] dictionary, VCDiffStreamingDecoder decoder) {
        this.in = Objects.requireNotNull(in, "in was null");
        this.decoder = Objects.requireNotNull(decoder, "decoder was null");
        this.dictionary = Objects.requireNotNull(dictionary, "dictionary was null").clone();
    }

    @Override
    public int read() throws IOException {
        fillDecodedBuffer();
        if (decodedBuffer.hasRemaining()) {
            return decodedBuffer.get() & 0xff;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        fillDecodedBuffer();
        if (decodedBuffer.hasRemaining()) {
            int lenToCopy = Math.min(len, decodedBuffer.remaining());
            decodedBuffer.get(b, off, lenToCopy);
            return lenToCopy;
        } else {
            return -1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = 0;
        while (skipped < n) {
            fillDecodedBuffer();
            if (!decodedBuffer.hasRemaining()) {
                return skipped;
            } else if (skipped + decodedBuffer.remaining() < n) {
                skipped += decodedBuffer.remaining();
                decodedBuffer.position(decodedBuffer.position() + decodedBuffer.remaining());
            } else {
                decodedBuffer.position(decodedBuffer.position() + (int) (n - skipped));
                return n;
            }
        }

        return skipped;
    }

    @Override
    public int available() throws IOException {
        return decodedBuffer.remaining();
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            closed = true;
        }
    }

    @Override
    public void mark(int readlimit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("Mark not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void fillDecodedBuffer() throws IOException {
        if (closed) {
            throw new IOException("InputStream is closed");
        }
        while (!decodedBuffer.hasRemaining()) {
            int read = in.read(inBuffer);
            if (read >= 0) {
                totalBytesRead += read;
                if (!decodingStarted) {
                    decoder.startDecoding(dictionary);
                    decodingStarted = true;
                }

                try {
                    decoder.decodeChunk(inBuffer, 0, read, tempDecoded);
                } catch (IOException e) {
                    throw new IOException("Error trying to decode data chunk starting at offset " + (totalBytesRead - read), e);
                }

                if (tempDecoded.size() > 0) {
                    decodedBuffer = ByteBuffer.wrap(tempDecoded.toByteArray());
                    tempDecoded.reset();
                }
            } else {
                decoder.finishDecoding();
                break;
            }
        }
    }
}
