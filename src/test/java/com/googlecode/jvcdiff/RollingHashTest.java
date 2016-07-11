package com.googlecode.jvcdiff;

import com.googlecode.jvcdiff.RollingHash.RollingHashUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;

public class RollingHashTest {
    static final int kBase = RollingHash.RollingHashUtil.kBase;

    static final int kUpdateHashTestIterations = 400;
    static final int kTimingTestSize = 1 << 14;  // 16K iterations
    static final int kUpdateHashBlocks = 1000;
    static final int kLargestBlockSize = 128;

    public void TestModBase(long operand) {
        assertEquals(0L, operand & ~0xFFFFFFFFL);

        assertEquals((0xffffffffL & operand) % kBase, RollingHashUtil.ModBase(operand & 0xFFFFFFFFL));
        assertEquals((0x100000000L - ((0xFFFFFFFFL & operand) % kBase)) & 0xFFFFFFFFL, RollingHashUtil.FindModBaseInverse(operand));
        assertEquals(0, (int) (RollingHashUtil.ModBase(operand) + RollingHashUtil.FindModBaseInverse(operand)));
    }

    public void TestHashFirstTwoBytes(byte first_value, byte second_value) {
        byte buf[] = new byte[2];

        buf[0] = first_value;
        buf[1] = second_value;

        assertEquals(RollingHashUtil.HashFirstTwoBytes(buf, 0), RollingHashUtil.HashStep(RollingHashUtil.HashStep(0, first_value), second_value));
        assertEquals(RollingHashUtil.HashFirstTwoBytes(buf, 0), RollingHashUtil.HashStep(first_value & 0xff, second_value));
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
                assertEquals(running_hash, hasher.Hash(buffer_, i + 1 - kBlockSize, buffer_.length - (i + 1 - kBlockSize)));
            }
        }
    }

    private void RunTimingTestForBlockSize(int kBlockSize, Random random) {
        byte[] buffer = new byte[kUpdateHashBlocks + kLargestBlockSize];
        random.nextBytes(buffer);

        final double time_for_default_hash = DefaultHashTimingTest(kBlockSize, buffer);
        final double time_for_rolling_hash = RollingTimingTest(kBlockSize, buffer);
        System.out.printf("%d\t%.3f\t%.3f (%.1f%%)\n",
                kBlockSize,
                time_for_default_hash,
                time_for_rolling_hash,
                FindPercentage(time_for_default_hash, time_for_rolling_hash));

        Assert.assertTrue(time_for_default_hash > 0.0);
        Assert.assertTrue(time_for_rolling_hash > 0.0);
    }

    private double DefaultHashTimingTest(int kBlockSize, byte[] buffer_) {
        // Execution time is expected to be O(kBlockSize) per hash operation,
        // so scale the number of iterations accordingly
        final int kTimingTestIterations = kTimingTestSize / kBlockSize;
        long time = System.nanoTime();
        BM_DefaultHash(kBlockSize, kTimingTestIterations, buffer_);
        time = System.nanoTime() - time;

        return time / 1000.0;
    }

    private double RollingTimingTest(int kBlockSize, byte[] buffer_) {
        // Execution time is expected to be O(1) per hash operation,
        // so leave the number of iterations constant
        final int kTimingTestIterations = kTimingTestSize;
        long time = System.nanoTime();
        BM_UpdateHash(kBlockSize, kTimingTestIterations, buffer_);
        time = System.nanoTime() - time;

        return time / 1000.0;
    }

    private double FindPercentage(double original, double modified) {
        if (original < 0.0001) {
            return 0.0;
        } else {
            return ((modified - original) / original) * 100.0;
        }
    }

    private void BM_DefaultHash(int kBlockSize, int iterations, byte[] buffer) {
        // TODO: why is result_array never read?
        RollingHash hasher = new RollingHash(kBlockSize);
        long result_array[] = new long[kUpdateHashBlocks];
        for (int iter = 0; iter < iterations; ++iter) {
            for (int i = 0; i < kUpdateHashBlocks; ++i) {
                result_array[i] = hasher.Hash(buffer, i, buffer.length - i);
            }
        }
    }

    private void BM_UpdateHash(int kBlockSize, int iterations, byte[] buffer) {
        RollingHash hasher = new RollingHash(kBlockSize);
        // TODO: why is result_array never read?
        long[] result_array = new long[kUpdateHashBlocks];
        for (int iter = 0; iter < iterations; ++iter) {
            long running_hash = hasher.Hash(buffer, 0, buffer.length);
            for (int i = 0; i < kUpdateHashBlocks; ++i) {
                running_hash = hasher.UpdateHash(running_hash, buffer[i], buffer[i + kBlockSize]);
                result_array[i] = running_hash;
            }
        }
    }

    @Test
    public void KBaseIsAPowerOfTwo() {
        assertEquals(0, kBase & (kBase - 1));
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
        TestHashFirstTwoBytes((byte) 0x00, (byte) 0x00);
        TestHashFirstTwoBytes((byte) 0x00, (byte) 0xFF);
        TestHashFirstTwoBytes((byte) 0xFF, (byte) 0x00);
        TestHashFirstTwoBytes((byte) 0xFF, (byte) 0xFF);
        TestHashFirstTwoBytes((byte) 0x00, (byte) 0x80);
        TestHashFirstTwoBytes((byte) 0x7F, (byte) 0xFF);
        TestHashFirstTwoBytes((byte) 0x7F, (byte) 0x80);
        TestHashFirstTwoBytes((byte) 0x01, (byte) 0x8F);
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


    @Test
    public void TimingTests() {
        Random random = new Random(1);

        RunTimingTestForBlockSize(4, random);
        RunTimingTestForBlockSize(8, random);
        RunTimingTestForBlockSize(16, random);
        RunTimingTestForBlockSize(32, random);
        RunTimingTestForBlockSize(64, random);
        RunTimingTestForBlockSize(128, random);
    }

    @Test
    public void testAgainstCppResults() {
        // This code was used to generate a file that was run through the C++ version of this code
        Random random = new Random(1);
        byte[] buffer = new byte[4096];
        random.nextBytes(buffer);

        CRC32 crc32 = new CRC32();
        crc32.update(buffer);
        assertEquals(0x83402f90, (int) crc32.getValue());

        // These are the results the C++ code produced.
        int[] expectedHashes = new int[]{
                0xefa82,
                0x546aae,
                0x44eaf,
                0x6b4d62,
                0x5c9691,
                0x546486,
                0x3dc508,
                0x45123c,
        };

        for (int windowSize = 16, i = 0; i < expectedHashes.length; i++, windowSize <<= 1) {
            RollingHash hasher = new RollingHash(windowSize);
            assertEquals(expectedHashes[i], hasher.Hash(buffer, 0, buffer.length));
        }
    }
}
