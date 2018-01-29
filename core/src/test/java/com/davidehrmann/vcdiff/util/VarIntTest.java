package com.davidehrmann.vcdiff.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class VarIntTest {
    @Test
    public void basicIntTest() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        ByteBuffer buffer = ByteBuffer.allocate(64);

        buffer.clear();
        VarInt.putInt(buffer, 0);
        buffer.flip();
        assertEquals(0, VarInt.getInt(buffer));

        buffer.clear();
        VarInt.putInt(buffer, Integer.MAX_VALUE);
        buffer.flip();
        assertEquals(Integer.MAX_VALUE, VarInt.getInt(buffer));

        buffer.clear();
        VarInt.putInt(buffer, 1);
        buffer.flip();
        assertEquals(1, VarInt.getInt(buffer));
    }

    @Test
    public void randomIntTest() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
        Random random = new Random(0x42);

        ByteBuffer buffer = ByteBuffer.allocate(16384);
        ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.remaining());

        while (buffer.remaining() >= 5) {
            int startPos = buffer.position();
            int testVal = random.nextInt() & Integer.MAX_VALUE;
            VarInt.putInt(buffer, testVal);
            VarInt.writeInt(out, testVal);
            assertEquals(buffer.position() - startPos, VarInt.calculateIntLength(testVal));
        }

        buffer.flip();

        assertEquals(0, buffer.compareTo(ByteBuffer.wrap(out.toByteArray())));

        random = new Random(0x42);
        while (buffer.hasRemaining()) {
            assertEquals(random.nextInt() & Integer.MAX_VALUE, VarInt.getInt(buffer));
        }
    }

    @Test
    public void basicLongTest() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException {
        ByteBuffer buffer = ByteBuffer.allocate(64);

        buffer.clear();
        VarInt.putLong(buffer, 0);
        buffer.flip();
        assertEquals(0, VarInt.getLong(buffer));

        buffer.clear();
        VarInt.putLong(buffer, Long.MAX_VALUE);
        buffer.flip();
        assertEquals(Long.MAX_VALUE, VarInt.getLong(buffer));

        buffer.clear();
        VarInt.putLong(buffer, 1);
        buffer.flip();
        assertEquals(1, VarInt.getLong(buffer));
    }

    @Test
    public void randomLongTest() throws VarInt.VarIntParseException, VarInt.VarIntEndOfBufferException, IOException {
        Random random = new Random(0x42);

        ByteBuffer buffer = ByteBuffer.allocate(16384);
        ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.remaining());

        while (buffer.remaining() >= 10) {
            int startPos = buffer.position();
            long testVal = random.nextLong() & Long.MAX_VALUE;
            VarInt.putLong(buffer, testVal);
            VarInt.writeLong(out, testVal);
            assertEquals(buffer.position() - startPos, VarInt.calculateLongLength(testVal));
        }

        buffer.flip();

        assertEquals(0, buffer.compareTo(ByteBuffer.wrap(out.toByteArray())));

        random = new Random(0x42);
        while (buffer.hasRemaining()) {
            assertEquals(random.nextLong() & Long.MAX_VALUE, VarInt.getLong(buffer));
        }
    }

    @Test
    public void longMatchesSpecTest() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        VarInt.putLong(buffer, 0xffffffffL);

        int size = buffer.position();
        buffer.flip();
        byte[] data = new byte[size];
        buffer.get(data);

        assertArrayEquals(new byte[]{
                (byte) 0x8F,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                0x7F,
        }, data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeInt1() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        VarInt.putInt(buffer, Integer.MIN_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeInt2() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        VarInt.putInt(buffer, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeLong1() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        VarInt.putLong(buffer, Integer.MIN_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeLong2() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        VarInt.putLong(buffer, -1);
    }
}
