package com.googlecode.jvcdiff;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;

public class VarIntTest {
	@Test
	public void basicIntTest() throws VarIntParseException, VarIntEndOfBufferException {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		
		buffer.clear();
		VarInt.putInt(buffer, 0);
		buffer.flip();
		Assert.assertEquals(0, VarInt.getInt(buffer));
		
		buffer.clear();
		VarInt.putInt(buffer, Integer.MAX_VALUE);
		buffer.flip();
		Assert.assertEquals(Integer.MAX_VALUE, VarInt.getInt(buffer));
		
		buffer.clear();
		VarInt.putInt(buffer, Integer.MIN_VALUE);
		buffer.flip();
		Assert.assertEquals(Integer.MIN_VALUE, VarInt.getInt(buffer));
		
		buffer.clear();
		VarInt.putInt(buffer, 1);
		buffer.flip();
		Assert.assertEquals(1, VarInt.getInt(buffer));
		
		buffer.clear();
		VarInt.putInt(buffer, -1);
		buffer.flip();
		Assert.assertEquals(-1, VarInt.getInt(buffer));
	}
	
	@Test
	public void randomIntTest() throws VarIntParseException, VarIntEndOfBufferException {
		Random random = new Random(0x42);
		
		ByteBuffer buffer = ByteBuffer.allocate(16384);
		
		while (buffer.remaining() >= 5) {
			int startPos = buffer.position();
			int testVal = random.nextInt();
			VarInt.putInt(buffer, testVal);
			Assert.assertEquals(buffer.position() - startPos, VarInt.calculateIntLength(testVal));
		}
		
		buffer.flip();
		
		random = new Random(0x42);
		while (buffer.hasRemaining()) {
			Assert.assertEquals(random.nextInt(), VarInt.getInt(buffer));
		}
	}
	
	@Test
	public void basicLongTest() throws VarIntParseException, VarIntEndOfBufferException {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		
		buffer.clear();
		VarInt.putLong(buffer, 0);
		buffer.flip();
		Assert.assertEquals(0, VarInt.getLong(buffer));
		
		buffer.clear();
		VarInt.putLong(buffer, Long.MAX_VALUE);
		buffer.flip();
		Assert.assertEquals(Long.MAX_VALUE, VarInt.getLong(buffer));
		
		buffer.clear();
		VarInt.putLong(buffer, Long.MIN_VALUE);
		buffer.flip();
		Assert.assertEquals(Long.MIN_VALUE, VarInt.getLong(buffer));
		
		buffer.clear();
		VarInt.putLong(buffer, 1);
		buffer.flip();
		Assert.assertEquals(1, VarInt.getLong(buffer));
		
		buffer.clear();
		VarInt.putLong(buffer, -1);
		buffer.flip();
		Assert.assertEquals(-1, VarInt.getLong(buffer));
	}
	
	@Test
	public void randomLongTest() throws VarIntParseException, VarIntEndOfBufferException {
		Random random = new Random(0x42);
		
		ByteBuffer buffer = ByteBuffer.allocate(16384);
		
		while (buffer.remaining() >= 10) {
			int startPos = buffer.position();
			long testVal = random.nextLong();
			VarInt.putLong(buffer, testVal);
			Assert.assertEquals(buffer.position() - startPos, VarInt.calculateLongLength(testVal));
		}
		
		buffer.flip();
		
		random = new Random(0x42);
		while (buffer.hasRemaining()) {
			Assert.assertEquals(random.nextLong(), VarInt.getLong(buffer));
		}
	}
}
