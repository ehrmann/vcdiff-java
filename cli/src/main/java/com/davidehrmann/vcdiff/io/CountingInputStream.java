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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

public class CountingInputStream extends FilterInputStream {

    private final AtomicLong bytesRead = new AtomicLong();

    public CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0) {
            bytesRead.getAndIncrement();
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read > 0) {
            bytesRead.getAndAdd(read);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        bytesRead.getAndAdd(skipped);
        return skipped;
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

    public long getBytesRead() {
        return bytesRead.get();
    }
}
