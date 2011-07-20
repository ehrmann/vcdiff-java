package com.googlecode.jvcdiff;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.jvcdiff.VarInt.VarIntParseException;

public class VarIntTest {
	@Test
	public void basicIntTest() throws VarIntParseException {
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
	public void randomIntTest() throws VarIntParseException {
		Random random = new Random(0x42);
		
		ByteBuffer buffer = ByteBuffer.allocate(8192);
		
		while (buffer.remaining() >= 5) {
			VarInt.putInt(buffer, random.nextInt());
		}
		
		buffer.flip();
		
		random = new Random(0x42);
		while (buffer.hasRemaining()) {
			Assert.assertEquals(random.nextInt(), VarInt.getInt(buffer));
		}
	}
}
