// Copyright 2008-2016 Google Inc., David Ehrmann
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

package com.davidehrmann.vcdiff.engine;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VCDiffLargeTargetTest extends VCDiffDecoderTest {
    protected final byte[] kLargeRunWindow = {
            0x00,         // Win_Indicator: no source segment
            0x0E,         // Length of the delta encoding
            (byte) 0xA0,  // Size of the target window (0x4000000)
            (byte) 0x80,  // Size of the target window cont'd
            (byte) 0x80,  // Size of the target window cont'd
            0x00,         // Size of the target window cont'd
            0x00,         // Delta_indicator (no compression)
            0x00,         // length of data for ADDs and RUNs
            0x06,         // length of instructions section
            0x00,         // length of addresses for COPYs
            // Interleaved segment
            0x00,         // VCD_RUN size 0
            (byte) 0xA0,  // Size of RUN (0x4000000)
            (byte) 0x80,  // Size of RUN cont'd
            (byte) 0x80,  // Size of RUN cont'd
            (byte) 0x00,  // Size of RUN cont'd
            (byte) 0xBE,  // Data for RUN
    };

    public VCDiffLargeTargetTest() {
        delta_window_header_ = new byte[0];
        delta_window_body_ = new byte[0];
        UseInterleavedFileHeader();
    }

    // Ensure that, with allowVcdTarget set to false, we can decode any number of
    // 64MB windows without running out of memory.
    @Test
    public void Decode() throws Exception {
        // 50 x 64MB = 3.2GB, which should be too large if memory usage accumulates
        // during each iteration.
        final int kIterations = 50;
        decoder_.setAllowVcdTarget(false);
        decoder_.setMaximumTargetFileSize(0x4000000L * kIterations);
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_file_header_, output_);
        assertArrayEquals(new byte[0], output_.toByteArray());

        for (int i = 0; i < kIterations; i++) {
            AssertRepeatedByteOutputStream out = new AssertRepeatedByteOutputStream((byte) 0xBE);
            try {
                decoder_.decodeChunk(kLargeRunWindow, out);
            } finally {
                out.close();
            }

            assertEquals(0x4000000, out.getSize());
        }
        decoder_.finishDecoding();
    }

    // If we don't increase the maximum target file size first, the same test should
    // produce an error.
    @Test
    public void DecodeReachesMaxFileSize() throws Exception {
        decoder_.setAllowVcdTarget(false);
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_file_header_, output_);
        assertArrayEquals(new byte[0], output_.toByteArray());

        // The default maximum target file size is 64MB, which just matches the target
        // data produced by a single iteration.
        AssertRepeatedByteOutputStream out = new AssertRepeatedByteOutputStream((byte) 0xBE);
        try {
            decoder_.decodeChunk(kLargeRunWindow, out);
        } finally {
            out.close();
        }

        assertEquals(0x4000000, out.getSize());

        // Trying to decode a second window should exceed the target file size limit.
        thrown.expect(IOException.class);
        decoder_.decodeChunk(kLargeRunWindow, output_);
    }

    protected static class AssertRepeatedByteOutputStream extends OutputStream {

        private final byte repeated;
        private final AtomicLong size = new AtomicLong();

        public AssertRepeatedByteOutputStream(byte repeated) {
            this.repeated = repeated;
        }

        @Override
        public void write(int b) throws IOException {
            if ((byte) (b & 0xff) == repeated) {
                size.getAndIncrement();
            } else {
                String message = String.format("Expected 0x%02X at position %d, got 0x%02X", repeated, size.get(), b);
                fail(message);
                throw new IOException(message);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int end = off + len;
            for (int i = off; i < len; i++) {
                if (b[i] == repeated) {
                    size.getAndIncrement();
                } else {
                    String message = String.format("Expected 0x%02X at position %d, got 0x%02X", repeated, size.get(), b[i]);
                    fail(message);
                    throw new IOException(message);
                }
            }
        }

        @Override
        public void close() { }

        public long getSize() {
            return size.get();
        }
    }
}
