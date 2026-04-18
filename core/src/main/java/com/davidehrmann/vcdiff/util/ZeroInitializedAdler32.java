/*
 * Copyright 2016 David Ehrmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davidehrmann.vcdiff.util;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * An {@link Adler32} implementation initialized with zero rather than one.
 * It's intended to be used as a workaround for code with buggy Adler-32
 * implementations.
 *
 * It delegates to {@link Adler32} and behaves the same, aside from returning a
 * different checksum.
 *
 * @see         Adler32
 * @author      David Ehrmann
 */
public final class ZeroInitializedAdler32 implements Checksum {

    private static final int MOD_ADLER = 65521;

    private final Adler32 delegate = new Adler32();
    private long bytesUpdated = 0L;

    @Override
    public void update(int b) {
        delegate.update(b);
        bytesUpdated++;
    }

    @Override
    public void update(byte[] b, int off, int len) {
        delegate.update(b, off, len);
        bytesUpdated += len;
    }

    public void update(byte[] b) {
        update(b, 0, b.length);
    }

    public void update(ByteBuffer buffer) {
        int remaining = buffer.remaining();
        delegate.update(buffer);
        bytesUpdated += remaining;
    }

    @Override
    public void reset() {
        delegate.reset();
        bytesUpdated = 0L;
    }

    @Override
    public long getValue() {
        long canonical = delegate.getValue();
        int a = (int) (canonical & 0xffff);
        int b = (int) (canonical >>> 16);
        int nMod = (int) (bytesUpdated % MOD_ADLER);
        int zeroA = Math.floorMod(a - 1, MOD_ADLER);
        int zeroB = Math.floorMod(b - nMod, MOD_ADLER);

        return (((long) zeroB << 16) | zeroA) & 0xffffffffL;
    }
}
