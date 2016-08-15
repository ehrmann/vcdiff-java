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

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.Adler32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class ZeroInitializedAdler32Test {
    private final Random random = new Random(42);
    private final Adler32 expected = new Adler32();
    private final ZeroInitializedAdler32 actual = new ZeroInitializedAdler32();

    public ZeroInitializedAdler32Test() {
        expected.update(ZeroInitializedAdler32.ADLER_ZERO);
        assumeTrue("ADLER_ZERO didn't reinitialize adler32 to zero", expected.getValue() == 0);
    }

    @Test
    public void testEmpty() {
        assertEquals(0, actual.getValue());
    }

    @Test
    public void testByteUpdate() {
        expected.update(42);
        actual.update(42);
        assertEquals(expected.getValue(), actual.getValue());
    }

    @Test
    public void testByteBuffer() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        ByteBuffer buffer = (ByteBuffer) ByteBuffer.wrap(b).position(7).limit(29);

        actual.update(buffer);
        expected.update(b, 7, 22);

        assertEquals(0, buffer.remaining());
        assertEquals(expected.getValue(), actual.getValue());
    }

    @Test
    public void testArrayUpdate() {
        byte[] b = new byte[32];
        random.nextBytes(b);

        actual.update(b);
        expected.update(b);

        assertEquals(expected.getValue(), actual.getValue());
    }

    @Test
    public void testArrayRangeUpdate() {
        byte[] b = new byte[32];
        random.nextBytes(b);

        actual.update(b, 3, 27);
        expected.update(b, 3, 27);

        assertEquals(expected.getValue(), actual.getValue());
    }

    @Test
    public void testLongTermCorrectness() {
        for (int i = 0; i < 10000; i++) {
            int action = random.nextInt(100);
            if (action == 0) {
                assertEquals(expected.getValue(), actual.getValue());
            } else if (action < 50) {
                int b = random.nextInt(256);
                actual.update(b);
                expected.update(b);
            } else {
                byte[] b = new byte[random.nextInt(65536)];
                random.nextBytes(b);
                actual.update(b);
                expected.update(b);
            }
        }

        assertEquals(expected.getValue(), actual.getValue());
    }
}
