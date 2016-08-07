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

package com.davidehrmann.vcdiff;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

/**
 * An {@link Adler32} implementation initialized with zero rather than one.
 * It's intended to be used as a workaround for code with buggy Adler-32
 * implementations.
 *
 * It extends {@link Adler32} and behaves the same, aside from returning a
 * different checksum.
 *
 * @see         Adler32
 * @author      David Ehrmann
 */
public class ZeroInitializedAdler32 extends Adler32 {

    // An alternate implementation initializes Adler32 with this byte array, resulting in an
    // Adler32 sum of zero, effectively initializing Adler32 with zero rather than one.
    @SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
    static final byte[] ADLER_ZERO = new byte[] {
            -26, 11, -99, -30, 23, -120, -6, -15, 8, 48, -61, 112, 94, 116, -23, 86,
            -30, -112, -118, -90, -85, -36, -69, -29, 124, 98, -120, -62, 106, -80, 56, 33,
            -62, -18, -44, 56, 40, 50, -22, 13, 3, 18, -114, 45, 24, 122, -95, -104,
            117, -64, 85, -4, -112, -11, -16, 38, -82, 125, -49, -75, -119, -30, -83, 16,
            44, -28, 37, -103, 34, -44, 116, 34, -49, 33, -124, -97, 28, 51, 37, 29,
            90, 119, -99, -13, 95, -70, 98, 54, -11, -69, 37, 113, 117, -6, 18, -107,
            -82, -42, 86, 46, 10, -42, -31, -41, -13, 70, 108, -16, 114, 5, -67, -115,
            25, -99, 41, -79, 35, -108, 90, -69, -39, 1, 82, 32, 54, 97, -81, 90,
            -51, -85, 48, 31, -22, -95, 119, -42, -9, 104, 16, 70, 33, 90, -78, -112,
            59, 27, -61, 95, -30, -22, 33, 93, -32, 57, 31, -24, -43, 126, -51, 5,
            20, 75, 124, -108, -54, -100, -128, -59, 101, 111, 55, -54, 50, -102, 74, -51,
            72, -40, 50, 36, -92, 36, -2, -61, -107, 74, 82, -14, 90, -70, -78, 63,
            9, -97, 0, -75, -79, 47, 67, 77, -62, 68, -87, -92, -83, 43, -50, -18,
            -57, -68, 62, 42, 114, -86, 56, -106, 126, 28, -75, 101, -52, 124, -88, -81,
            52, -115, -122, -87, -88, -61, 7, 21, -103, -50, 46, -5, -103, 119, -121, -103,
            65, -80, -81, -105, 84, -27, 25, 127, 36, -104, 14, 107, 52, 44, -11, -1,
            30, 0, -44, -103, -73, -65, 22, -57, 112, -11, -114, -7, -2, -26, -69, 119,
            61, -102, 77, -108, 81, 60, -24, 35, 14, -91, 39, 42, -103, 50, -29, -43,
            79, -35, -76, -83, -27, -117, -73, 9, -49, -94, -111, -60, -27, -34, -76, 48,
            68, 101, -31, 73, 114, 24, -19, 45, -59, 43, -57, -53, 4, -9, -72, -37,
            78, 14, -19, 13, 58, -113, 50, -124, 63, -68, 0, -123, 86, -55, -71, 107,
            -35, -10, 49, -34, -66, -33, 112, 68, -48, 18, -75, 118, 125, -30, -66, -73,
            -92, 122, 70, -31, -65, 52, -49, -32, 72, 43, -95, 100, 88, 14, 17, -46,
            -111, 55, 6, 123, 62, -5, -71, 15, -72, 67, -69, 83, 11, 91, 60, 17,
            -40, 10, -119, 80, 124, -30, -42, -59, 113, -40, 61, -119, -12, 22, 6, 11,
            -24, 15, 40, 71, -6, -44, 16, 55, 4, -91, -96, -118, -74, -53, 122, -109,
            115, -92, 125, -124, 120, -1, -113, -37, 67, -86, -99, 27, 49, 72, -33, -65,
            105, 103, 96, 107, -7, -48, -67, 100, 61, 33, 44, 66, -121, -103, -64, -28,
            -4, 50, -22, -93, -68, -11, 66, -55, 69, -119, -121, 68, 24, 12, -28, -106,
            -99, 37, 116, 15, 67, 89, 8, 62, -21, -98, 39, -92, -122, 45, 85, -85,
            -4, -100, -104, -42, 79, 43, 55, 56, 0, 93, -59, 42, -79, -54, 93, 55,
            11, 51, 46, 91, -31, -41, 57, 109, -107, 4, 89, -40, -70, -16, -125, -111,
    };

    private static final int MOD_ADLER = 65521;

    private volatile int bytesUpdatedModAdler = 0;

    @Override
    public void update(int b) {
        super.update(b);
        addAndModBytesUpdated(1);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        super.update(b, off, len);
        addAndModBytesUpdated(len);
    }

    @Override
    public void update(byte[] b) {
        super.update(b);
        addAndModBytesUpdated(b.length);
    }

    // This method exists in Java8.  Its implementation is more efficient, so switch
    // to super.update(buffer) when support for older Java is dropped.
    public void update(ByteBuffer buffer) {
        byte[] copyBuffer = new byte[2048];
        int read;
        while ((read = Math.min(copyBuffer.length, buffer.remaining())) > 0) {
            buffer.get(copyBuffer, 0, read);
            update(copyBuffer, 0, read);
        }
    }

    @Override
    public void reset() {
        super.reset();
        bytesUpdatedModAdler = 0;
    }

    @Override
    public long getValue() {
        long adler32 = super.getValue();
        int adlerA = (int) adler32 & 0xffff;
        int adlerB = (int) adler32 >>> 16;

        int buggyA = (adlerA - 1) % 65521;
        int buggyB = ((adlerB - bytesUpdatedModAdler) % 65521);
        if (buggyB < 0) { buggyB += 65521; }
        if (buggyA < 0) { buggyA += 65521; }

        return (((buggyB << 16) | buggyA)) & 0xffffffffL;
    }

    private void addAndModBytesUpdated(int bytesUpdated) {
        bytesUpdatedModAdler = (int)(((long) bytesUpdatedModAdler + bytesUpdated) % MOD_ADLER);
    }
}
