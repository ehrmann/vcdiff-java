package com.googlecode.jvcdiff;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.jvcdiff.RollingHash.RollingHashUtil;

public class RollingHashTest {
	static final int kBase = RollingHash.RollingHashUtil.kBase;

	static final int kUpdateHashTestIterations = 400;
	static final int kTimingTestSize = 1 << 14;  // 16K iterations
	static final int kUpdateHashBlocks = 1000;
	static final int kLargestBlockSize = 128;

	public void TestModBase(long operand) {
		Assert.assertEquals(0L, operand & ~0xFFFFFFFFL);

		Assert.assertEquals((0xffffffffL & operand) % kBase, RollingHashUtil.ModBase(operand & 0xFFFFFFFFL));
		Assert.assertEquals((0x100000000L - ((0xFFFFFFFFL & operand) % kBase)) & 0xFFFFFFFFL, RollingHashUtil.FindModBaseInverse(operand));
		Assert.assertEquals(0, (int)(RollingHashUtil.ModBase(operand) + RollingHashUtil.FindModBaseInverse(operand)));
	}

	public void TestHashFirstTwoBytes(byte first_value, byte second_value) {
		byte buf[] = new byte[2];

		buf[0] = first_value;
		buf[1] = second_value;

		Assert.assertEquals(RollingHashUtil.HashFirstTwoBytes(buf, 0), RollingHashUtil.HashStep(RollingHashUtil.HashStep(0, first_value), second_value));
		Assert.assertEquals(RollingHashUtil.HashFirstTwoBytes(buf, 0), RollingHashUtil.HashStep(first_value & 0xff, second_value));
	}

	public void UpdateHashMatchesHashForBlockSize(int kBlockSize, Random random) {
		RollingHash hasher = new RollingHash(kBlockSize);
		for (int x = 0; x < kUpdateHashTestIterations; ++x) {
			int random_buffer_size = random.nextInt(kUpdateHashBlocks) + kBlockSize;
			
			byte[] buffer_ = new byte[random_buffer_size];
			random.nextBytes(buffer_);
			
			long running_hash = hasher.Hash(buffer_, 0, buffer_.length);
			for (int i = kBlockSize; i < random_buffer_size; ++i) {
				// UpdateHash() calculates the hash value incrementally.
				running_hash = hasher.UpdateHash(running_hash,
						buffer_[i - kBlockSize],
						buffer_[i]);
				// Hash() calculates the hash value from scratch.  Verify that both
				// methods return the same hash value.
				Assert.assertEquals(running_hash, hasher.Hash(buffer_, i + 1 - kBlockSize, buffer_.length - (i + 1 - kBlockSize)));
			}
		}
	}

	@Test
	public void KBaseIsAPowerOfTwo() {
		Assert.assertEquals(0, kBase & (kBase - 1));
	}

	@Test
	public void TestModBaseForValues() {
		TestModBase(0);
		TestModBase(10);
		TestModBase(-10 & 0xFFFFFFFFL);
		TestModBase(kBase - 1);
		TestModBase(kBase);
		TestModBase(kBase + 1);
		TestModBase(0x7FFFFFFFL);
		TestModBase(0x80000000L);
		TestModBase(0xFFFFFFFEL);
		TestModBase(0xFFFFFFFFL);
	}

	@Test
	public void VerifyHashFirstTwoBytes() {
		TestHashFirstTwoBytes((byte)0x00, (byte)0x00);
		TestHashFirstTwoBytes((byte)0x00, (byte)0xFF);
		TestHashFirstTwoBytes((byte)0xFF, (byte)0x00);
		TestHashFirstTwoBytes((byte)0xFF, (byte)0xFF);
		TestHashFirstTwoBytes((byte)0x00, (byte)0x80);
		TestHashFirstTwoBytes((byte)0x7F, (byte)0xFF);
		TestHashFirstTwoBytes((byte)0x7F, (byte)0x80);
		TestHashFirstTwoBytes((byte)0x01, (byte)0x8F);
	}

	@Test
	public void UpdateHashMatchesHashFromScratch() {
		Random random = new Random(1);

		UpdateHashMatchesHashForBlockSize(4, random);
		UpdateHashMatchesHashForBlockSize(8, random);
		UpdateHashMatchesHashForBlockSize(16, random);
		UpdateHashMatchesHashForBlockSize(32, random);
		UpdateHashMatchesHashForBlockSize(64, random);
		UpdateHashMatchesHashForBlockSize(128, random);
	}

	/*
	@Test
	public void intVsLongMod() {
		int i = 0xc1841943;
		long l = 0xc1841943L;

		Assert.assertTrue(i < 0);
		Assert.assertTrue(l > 0L);

		int ii = i % 7393;
		long ll = l % 7393;

		System.out.println("int mod = " + ii);
		System.out.println("long mod = " + ll);


	}
	 */
}
