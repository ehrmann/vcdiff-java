package com.googlecode.jvcdiff;

import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class VarIntTest {
	@Test
	public void basicIntTest() throws VarIntParseException, VarIntEndOfBufferException {
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
		VarInt.putInt(buffer, Integer.MIN_VALUE);
		buffer.flip();
		assertEquals(Integer.MIN_VALUE, VarInt.getInt(buffer));

		buffer.clear();
		VarInt.putInt(buffer, 1);
		buffer.flip();
		assertEquals(1, VarInt.getInt(buffer));

		buffer.clear();
		VarInt.putInt(buffer, -1);
		buffer.flip();
		assertEquals(-1, VarInt.getInt(buffer));
	}

	@Test
	public void randomIntTest() throws VarIntParseException, VarIntEndOfBufferException, IOException {
		Random random = new Random(0x42);

		ByteBuffer buffer = ByteBuffer.allocate(16384);
		ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.remaining());

		while (buffer.remaining() >= 5) {
			int startPos = buffer.position();
			int testVal = random.nextInt();
			VarInt.putInt(buffer, testVal);
			VarInt.writeInt(out, testVal);
			assertEquals(buffer.position() - startPos, VarInt.calculateIntLength(testVal));
		}

		buffer.flip();

		assertEquals(0, buffer.compareTo(ByteBuffer.wrap(out.toByteArray())));

		random = new Random(0x42);
		while (buffer.hasRemaining()) {
			assertEquals(random.nextInt(), VarInt.getInt(buffer));
		}
	}

	@Test
	public void basicLongTest() throws VarIntParseException, VarIntEndOfBufferException {
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
		VarInt.putLong(buffer, Long.MIN_VALUE);
		buffer.flip();
		assertEquals(Long.MIN_VALUE, VarInt.getLong(buffer));

		buffer.clear();
		VarInt.putLong(buffer, 1);
		buffer.flip();
		assertEquals(1, VarInt.getLong(buffer));

		buffer.clear();
		VarInt.putLong(buffer, -1);
		buffer.flip();
		assertEquals(-1, VarInt.getLong(buffer));
	}

	@Test
	public void randomLongTest() throws VarIntParseException, VarIntEndOfBufferException, IOException {
		Random random = new Random(0x42);

		ByteBuffer buffer = ByteBuffer.allocate(16384);
		ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.remaining());

		while (buffer.remaining() >= 10) {
			int startPos = buffer.position();
			long testVal = random.nextLong();
			VarInt.putLong(buffer, testVal);
			VarInt.writeLong(out, testVal);
			assertEquals(buffer.position() - startPos, VarInt.calculateLongLength(testVal));
		}

		buffer.flip();

		assertEquals(0, buffer.compareTo(ByteBuffer.wrap(out.toByteArray())));

		random = new Random(0x42);
		while (buffer.hasRemaining()) {
			assertEquals(random.nextLong(), VarInt.getLong(buffer));
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

		assertArrayEquals(new byte[] {
				(byte)0x8F,
				(byte)0xFF,
				(byte)0xFF,
				(byte)0xFF,
				0x7F,
		}, data);
	}
}
