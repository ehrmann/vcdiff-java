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

package com.davidehrmann.vcdiff.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ComparingOutputStream extends OutputStream {

    private final InputStream expected;
    private volatile boolean open = true;

    public ComparingOutputStream(InputStream expected) {
        this.expected = new BufferedInputStream(expected);
    }

    @Override
    public void write(int b) throws IOException {
        if (!open) {
            throw new IllegalStateException();
        }

        b = b & 0xff;
        int read = expected.read();
        if (read < 0) {
            throw new IOException("Decoded target is longer than original target file");
        } else if (b != read) {
            throw new IOException("Original target file does not match decoded target");
        }
    }

    @Override
    public void close() throws IOException {
        open = false;
        if (expected.read() >= 0) {
            throw new IOException("Decoded target is shorter than original target file");
        }
    }
}
