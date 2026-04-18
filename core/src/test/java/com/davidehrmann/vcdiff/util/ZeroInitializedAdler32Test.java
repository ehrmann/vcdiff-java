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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ZeroInitializedAdler32Test {

    @Parameter(0)
    public String label;

    @Parameter(1)
    public byte[] data;

    @Parameter(2)
    public long expectedChecksum;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "empty", new byte[0], 0x00000000L },
                { "single_zero_byte", new byte[] { 0 }, 0x00000000L },
                { "single_0xff_byte", new byte[] { -1 }, 0x00ff00ffL },
                { "single_char_a", new byte[] { 97 }, 0x00610061L },
                { "ascii_abc", bytes("abc"), 0x024a0126L },
                { "ascii_hello", bytes("Hello, World!"), 0x1f910469L },
                { "all_zeros_16", filled(16, (byte) 0), 0x00000000L },
                { "all_zeros_1000", filled(1000, (byte) 0), 0x00000000L },
                { "all_0xff_16", filled(16, (byte) 0xff), 0x87780ff0L },
                { "all_0xff_5552", filled(5552, (byte) 0xff), 0xdbdf9b8bL },
                { "all_0xff_5553", filled(5553, (byte) 0xff), 0x78789c8aL },
                { "all_0xff_11104", filled(11104, (byte) 0xff), 0xd40f3725L },
                { "counting_256", counting(256), 0xacf67f80L },
                { "counting_256_x10", repeat(counting(256), 10), 0x923bfb3cL },
                { "alphabet_x100", repeat(bytes("abcdefghijklmnopqrstuvwxyz"), 100), 0xa9e45858L },
                { "large_mixed_65536", mixed(65536), 0x7b1f8771L },
                { "len_65520_ff", filled(65520, (byte) 0xff), 0x0000fef2L },
                { "len_65521_ff", filled(65521, (byte) 0xff), 0x00000000L },
                { "len_65522_ff", filled(65522, (byte) 0xff), 0x00ff00ffL },
        });
    }

    @Test
    public void updateWholeArray() {
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        adler32.update(data, 0, data.length);
        assertEquals(expectedChecksum, adler32.getValue());
    }

    @Test
    public void updateByteAtATime() {
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        for (byte b : data) {
            adler32.update(b & 0xff);
        }
        assertEquals(expectedChecksum, adler32.getValue());
    }

    @Test
    public void updateInSmallChunks() {
        final int chunkSize = 1000;
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        for (int off = 0; off < data.length; off += chunkSize) {
            int len = Math.min(chunkSize, data.length - off);
            adler32.update(data, off, len);
        }
        assertEquals(expectedChecksum, adler32.getValue());
    }

    @Test
    public void updateInOddChunks() {
        final int[] chunkSizes = { 1, 2, 3, 5, 7, 11, 13, 17, 19, 23 };
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        int off = 0;
        int i = 0;
        while (off < data.length) {
            int len = Math.min(chunkSizes[i++ % chunkSizes.length], data.length - off);
            adler32.update(data, off, len);
            off += len;
        }
        assertEquals(expectedChecksum, adler32.getValue());
    }

    @Test
    public void updateViaByteBuffer() {
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        adler32.update(ByteBuffer.wrap(data));
        assertEquals(expectedChecksum, adler32.getValue());
    }

    @Test
    public void resetRestoresInitialState() {
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        adler32.update(new byte[] { 1, 2, 3, 4, 5 }, 0, 5);
        adler32.reset();
        adler32.update(data, 0, data.length);
        assertEquals(expectedChecksum, adler32.getValue());
    }

    @Test
    public void emptyChecksumIsZero() {
        ZeroInitializedAdler32 adler32 = new ZeroInitializedAdler32();
        assertEquals(0L, adler32.getValue());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] filled(int len, byte value) {
        byte[] out = new byte[len];
        Arrays.fill(out, value);
        return out;
    }

    private static byte[] counting(int len) {
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) i;
        }
        return out;
    }

    private static byte[] repeat(byte[] unit, int times) {
        byte[] out = new byte[unit.length * times];
        for (int i = 0; i < times; i++) {
            System.arraycopy(unit, 0, out, i * unit.length, unit.length);
        }
        return out;
    }

    private static byte[] mixed(int len) {
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) ((i * 31 + 7) & 0xff);
        }
        return out;
    }
}