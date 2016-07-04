package com.googlecode.jvcdiff;

import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class VCDiffAddressCacheTest {
	@Test
	public void ZeroCacheSizes() {
		VCDiffAddressCache zero_cache = new VCDiffAddressCacheImpl((short)0, (short)0);
		zero_cache.Init();
	}

	@Test(expected=IllegalArgumentException.class)
	public void NegativeCacheSizes() {
		new VCDiffAddressCacheImpl((short)-1, (short)-1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void OnlySameCacheSizeIsNegative() {
		new VCDiffAddressCacheImpl((short)0, (short)-1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void ExtremePositiveCacheSizes() {
		new VCDiffAddressCacheImpl(Short.MAX_VALUE, Short.MAX_VALUE);
	}

	@Test(expected=IllegalArgumentException.class)
	public void ExtremeNegativeCacheSizes() {
		new VCDiffAddressCacheImpl(Short.MIN_VALUE, Short.MIN_VALUE);
	}

	@Test(expected=IllegalArgumentException.class)
	public void NearCacheSizeIsTooBig() {
		new VCDiffAddressCacheImpl((short)(VCDiffAddressCache.VCD_MAX_MODES - 1), (short)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void SameCacheSizeIsTooBig() {
		new VCDiffAddressCacheImpl((short)0, (short)(VCDiffAddressCache.VCD_MAX_MODES - 1));
	}

	@Test(expected=IllegalArgumentException.class)
	public void CombinedSizesAreTooBig() {
		new VCDiffAddressCacheImpl((short)(VCDiffAddressCache.VCD_MAX_MODES / 2), (short)(VCDiffAddressCache.VCD_MAX_MODES / 2 - 1));
	}

	@Test
	public void MaxLegalNearCacheSize() {
		VCDiffAddressCacheImpl i = new VCDiffAddressCacheImpl((short)(VCDiffAddressCache.VCD_MAX_MODES - 2), (short)0);
		i.Init();
	}

	@Test
	public void MaxLegalSameCacheSize() {
		VCDiffAddressCacheImpl i = new VCDiffAddressCacheImpl((short)0, (short)(VCDiffAddressCache.VCD_MAX_MODES - 2));
		i.Init();
	}

	@Test
	public void MaxLegalCombinedSizes() {
		VCDiffAddressCacheImpl i = new VCDiffAddressCacheImpl((short)(VCDiffAddressCache.VCD_MAX_MODES / 2 - 1), (short)(VCDiffAddressCache.VCD_MAX_MODES / 2 - 1));
		i.Init();
	}

	@Test
	public void CacheContentsInitiallyZero() {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.kDefaultNearCacheSize, cache.near_addresses_.length);
		assertEquals(VCDiffAddressCache.kDefaultSameCacheSize * 256, cache.same_addresses_.length);

		int test_address = 0;
		// Check that caches are initially set to zero
		for (test_address = 0; test_address < cache.near_addresses_.length; ++test_address) {
			assertEquals(0, cache.near_addresses_[test_address]);
		}
		for (test_address = 0; test_address < cache.same_addresses_.length; ++test_address) {
			assertEquals(0, cache.same_addresses_[test_address]);
		}
	}

	@Test
	public void InsertFirstTen() {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		for (int test_address = 1; test_address <= 10; ++test_address) {
			cache.UpdateCache(test_address);
		}

		assertEquals(9, cache.near_addresses_[0]);   // slot 0: 1 => 5 => 9
		assertEquals(10, cache.near_addresses_[1]);  // slot 1: 2 => 6 => 10
		assertEquals(7, cache.near_addresses_[2]);   // slot 2: 3 => 7
		assertEquals(8, cache.near_addresses_[3]);   // slot 3: 4 => 8
		assertEquals(0, cache.same_addresses_[0]);

		for (int test_address = 1; test_address <= 10; ++test_address) {
			assertEquals(test_address, cache.same_addresses_[test_address]);
		}
		for (int test_address = 11; test_address < 256 * 3; ++test_address) {
			assertEquals(0, cache.same_addresses_[test_address]);
		}
	}

	@Test
	public void InsertIntMax() {
		VCDiffAddressCacheImpl cache_ = new VCDiffAddressCacheImpl();
		cache_.Init();

		cache_.UpdateCache(Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, cache_.near_addresses_[0]);
		assertEquals(Integer.MAX_VALUE, cache_.same_addresses_[Integer.MAX_VALUE % (256 * 3)]);
		assertEquals(0, cache_.same_addresses_[(Integer.MAX_VALUE - 256) % (256 * 3)]);
		assertEquals(0, cache_.same_addresses_[(Integer.MAX_VALUE - 512) % (256 * 3)]);
	}

	@Test
	public void EncodeAddressModes() throws VarIntParseException, VarIntEndOfBufferException {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		ByteBuffer buffer = ByteBuffer.allocate(2048);

		TestEncode(cache, buffer, 0x0000FFFF, 0x10000000,  VCDiffAddressCache.VCD_SELF_MODE, 3);
		TestEncode(cache, buffer, 0x10000000, 0x10000010,  VCDiffAddressCache.VCD_HERE_MODE, 1);
		TestEncode(cache, buffer, 0x10000004, 0x10000020,  (short)(VCDiffAddressCache.VCD_FIRST_NEAR_MODE + 0x01), 1);
		TestEncode(cache, buffer, 0x0FFFFFFE, 0x10000030,  VCDiffAddressCache.VCD_HERE_MODE, 1);
		TestEncode(cache, buffer, 0x10000004, 0x10000040,  (short)(cache.FirstSameMode() + 0x01), 1);

		buffer.flip();

		assertEquals(0xFFFF, VarInt.getInt(buffer));	// SELF mode: addr 0x0000FFFF
		assertEquals(3, buffer.position());

		assertEquals(0x10, VarInt.getInt(buffer));	// HERE mode: here - 0x10 = 0x10000000
		assertEquals(4, buffer.position());

		assertEquals(0x04, VarInt.getInt(buffer));	// NEAR cache #1:
		assertEquals(5, buffer.position());

		// last addr + 0x4 = 0x10000004

		assertEquals(0x32, VarInt.getInt(buffer));	// HERE mode: here - 0x32 = 0x0FFFFFFE
		assertEquals(6, buffer.position());

		assertEquals(0x04, buffer.get());			// SAME cache #1: 0x10000004 hits
	}

	@Test
	public void DecodeAddressModes() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);

		VarInt.putInt(buffer, 0xCAFE);
		VarInt.putInt(buffer, 0xCAFE);
		VarInt.putInt(buffer, 0x1000);
		buffer.put((byte)0xFE);			// SAME mode uses a byte, not a Varint
		VarInt.putInt(buffer, 0xFE);
		VarInt.putInt(buffer, 0x1000);

		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(0xCAFE, cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(3, buffer.position());

		assertEquals(0x20000 - 0xCAFE, cache.DecodeAddress(0x20000, VCDiffAddressCache.VCD_HERE_MODE, buffer));
		assertEquals(6, buffer.position());

		assertEquals(0xDAFE, cache.DecodeAddress(0x30000, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, buffer));
		assertEquals(8, buffer.position());

		assertEquals(0xCAFE, cache.DecodeAddress(0x40000, (short)(cache.FirstSameMode() + (0xCA % 3)), buffer));
		assertEquals(9, buffer.position());	// a byte, not a Varint

		assertEquals(0xFE, cache.DecodeAddress( 0x50000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(11, buffer.position());

		// NEAR mode #0 has been overwritten by fifth computed addr (wrap around)
		assertEquals(0x10FE, cache.DecodeAddress(0x60000, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, buffer));
		assertEquals(13, buffer.position());

		assertEquals(0, buffer.remaining());
	}

	@Test
	public void EncodeAddressZeroCacheSizes() {
		AtomicInteger encoded_addr = new AtomicInteger();
		VCDiffAddressCacheImpl zero_cache = new VCDiffAddressCacheImpl((short)0, (short)0);

		assertEquals(VCDiffAddressCache.VCD_SELF_MODE, zero_cache.EncodeAddress(0x0000FFFF, 0x10000000, encoded_addr));
		assertEquals(0xFFFF, encoded_addr.get());

		assertEquals(VCDiffAddressCache.VCD_HERE_MODE, zero_cache.EncodeAddress(0x10000000, 0x10000010, encoded_addr));
		assertEquals(0x10, encoded_addr.get());

		assertEquals(VCDiffAddressCache.VCD_HERE_MODE, zero_cache.EncodeAddress(0x10000004, 0x10000020, encoded_addr));
		assertEquals(0x1C, encoded_addr.get());

		assertEquals(VCDiffAddressCache.VCD_HERE_MODE, zero_cache.EncodeAddress(0x0FFFFFFE, 0x10000030, encoded_addr));
		assertEquals(0x32, encoded_addr.get());

		assertEquals(VCDiffAddressCache.VCD_HERE_MODE, zero_cache.EncodeAddress(0x10000004, 0x10000040, encoded_addr));
		assertEquals(0x3C, encoded_addr.get());
	}

	@Test
	public void DecodeAddressZeroCacheSizes() {
		VCDiffAddressCacheImpl zero_cache = new VCDiffAddressCacheImpl((short)0, (short)0);
		ByteBuffer buffer = ByteBuffer.allocate(2048);

		VarInt.putInt(buffer, 0xCAFE);
		VarInt.putInt(buffer, 0xCAFE);
		VarInt.putInt(buffer, 0xDAFE);

		buffer.flip();

		assertEquals(0xCAFE, zero_cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(3, buffer.position());

		assertEquals(0x20000 - 0xCAFE, zero_cache.DecodeAddress(0x20000, VCDiffAddressCache.VCD_HERE_MODE, buffer));
		assertEquals(6, buffer.position());

		assertEquals(0xDAFE, zero_cache.DecodeAddress(0x30000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(9, buffer.position());

		assertEquals(0, buffer.remaining());
	}

	@Test(expected = IllegalArgumentException.class)
	public void EncodeNegativeAddress() {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();
		cache.EncodeAddress(-1, -1, new AtomicInteger());
	}

	@Test(expected = IllegalArgumentException.class)
	public void EncodeAddressPastHereAddress1() {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();
		cache.EncodeAddress(0x100, 0x100, new AtomicInteger());
	}

	@Test(expected = IllegalArgumentException.class)
	public void EncodeAddressPastHereAddress2() {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();
		cache.EncodeAddress(0x200, 0x100, new AtomicInteger());
	}

	@Test(expected = IllegalArgumentException.class)
	public void DecodeInvalidMode1() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000000, (short)(cache.LastMode() + 1), buffer));
	}

	@Test(expected = IllegalArgumentException.class)
	public void DecodeInvalidMode2() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000000, (short)0xFF, buffer));
	}

	@Test(expected = IllegalArgumentException.class)
	public void DecodeZeroOrNegativeHereAddress1() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();
		cache.DecodeAddress(-1, VCDiffAddressCache.VCD_SELF_MODE, buffer);
	}

	@Test
	public void DecodeZeroOrNegativeHereAddress2() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		// A zero value for here_address should not kill the decoder,
		// but instead should return an error value.  A delta file may contain
		// a window that has no source segment and that (erroneously)
		// uses a COPY instruction as its first instruction.  This should
		// cause an error to be reported, not a debug check failure.
		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0, VCDiffAddressCache.VCD_SELF_MODE, buffer));
	}

	@Test
	public void DecodeAddressPastHereAddress() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x1000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(0, buffer.position());
	}

	@Test
	public void HereModeAddressTooLarge() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0x10001);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_HERE_MODE, buffer));
		assertEquals(0, buffer.position());
	}

	@Test
	public void NearModeAddressOverflow() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		VarInt.putInt(buffer, 0x7FFFFFFF);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(0xCAFE, cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(3, buffer.position());

		// Now decode a NEAR mode address of base address 0xCAFE
		// (the first decoded address) + offset 0x7FFFFFFF.  This will cause
		// an integer overflow and should signal an error.
		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000000, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, buffer));
		assertEquals(3, buffer.position());
	}

	// A Varint should contain at most 9 bytes that have their continuation bit
	// (the uppermost, or 7 bit) set.  A longer string of bytes that all have
	// bit 7 set is not a valid Varint.  Try to parse such a string as a Varint
	// and confirm that it does not run off the end of the input buffer and
	// it returns an error value (RESULT_ERROR).
	@Test
	public void DecodeInvalidVarint() {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		Arrays.fill(buffer.array(), (byte)0xfe);
		buffer.limit(buffer.capacity());

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(0, buffer.position());
	}

	// If only part of a Varint appears in the data to be decoded,
	// then DecodeAddress should return RESULT_END_OF_DATA,
	// which means that the Varint *may* be valid if there is more
	// data expected to be returned.
	@Test
	public void DecodePartialVarint() {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[] {(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0x01 });
		buffer.limit(3);

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_END_OF_DATA, cache.DecodeAddress(0x10000000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(0, buffer.position());

		// Now add the missing last byte (supposedly read from a stream of data)
		// and verify that the Varint is now valid.
		buffer.limit(4);

		assertEquals(0xFDFBF01, cache.DecodeAddress(0x10000000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(4, buffer.position());
	}

	@Test(expected = IllegalArgumentException.class)
	public void DecodeBadMode() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		cache.DecodeAddress(0x10000, (short)(cache.LastMode() + 1), buffer);
	}

	@Test
	public void DecodeInvalidHereAddress() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0x10001); // offset larger than here_address
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_HERE_MODE, buffer));
		assertEquals(0, buffer.position());
	}

	@Test
	public void DecodeInvalidNearAddress() {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		VarInt.putInt(buffer, 0xCAFE);
		VarInt.putInt(buffer, Integer.MAX_VALUE); // offset will cause integer overflow
		buffer.flip();

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		assertEquals(0xCAFE, cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_SELF_MODE, buffer));
		assertEquals(3, buffer.position());

		assertEquals(VCDiffAddressCache.RESULT_ERROR, cache.DecodeAddress(0x10000, VCDiffAddressCache.VCD_FIRST_NEAR_MODE, buffer));
		assertEquals(3, buffer.position());
	}

	@Test
	public void PerformanceTest() {
		final int test_size = 20 * 1024;  // 20K random encode/decode operations
		final int num_iterations = 40;  // run test 40 times and take average

		int[] verify_stream = new int[test_size];
		short[] mode_stream = new short[test_size];

		ByteBuffer large_address_stream = ByteBuffer.allocate(test_size * 8);

		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();

		BM_Setup(verify_stream, mode_stream, cache, large_address_stream);

		long time1 = System.nanoTime();
		BM_CacheEncode(num_iterations, cache, verify_stream, mode_stream, large_address_stream);
		time1 = System.nanoTime() - time1;
		System.out.printf("Time to encode: %.3f ms\n", time1 / 1000000.0);

		long time2 = System.nanoTime();
		BM_CacheDecode(num_iterations, cache, verify_stream, mode_stream, large_address_stream);
		time2 = System.nanoTime() - time2;
		System.out.printf("Time to encode: %.3f ms\n", time2 / 1000000.0);
	}

	private void BM_Setup(int[] verify_stream, short[] mode_stream, VCDiffAddressCache cache, ByteBuffer large_address_stream) {
		int here_address = 1;
		Random random = new Random(1);
		for (int i = 0; i < verify_stream.length; ++i) {
			verify_stream[i] = random.nextInt(here_address);
			here_address += 4;
		}
		BM_CacheEncode(1, cache, verify_stream, mode_stream, large_address_stream);  // populate large_address_stream_, mode_stream_
	}

	private void BM_CacheEncode(int iterations, VCDiffAddressCache cache, int[] verify_stream, short[] mode_stream, ByteBuffer large_address_stream) {
		int here_address = 1;
		AtomicInteger encoded_addr = new AtomicInteger(0);
		for (int test_iteration = 0; test_iteration < iterations; ++test_iteration) {
			cache.Init();
			large_address_stream.clear();
			here_address = 1;
			for (int i = 0; i < verify_stream.length; ++i) {
				short mode = cache.EncodeAddress(verify_stream[i], here_address, encoded_addr);
				if (cache.WriteAddressAsVarintForMode(mode)) {
					VarInt.putInt(large_address_stream, encoded_addr.get());
				} else {
					Assert.assertTrue(256 > encoded_addr.get());
					large_address_stream.put((byte)encoded_addr.get());
				}
				mode_stream[i] = mode;
				here_address += 4;
			}

			large_address_stream.flip();
		}
	}

	private void BM_CacheDecode(int iterations, VCDiffAddressCache cache, int[] verify_stream, short[] mode_stream, ByteBuffer large_address_stream) {
		int here_address = 1;
		for (int test_iteration = 0; test_iteration < iterations; ++test_iteration) {
			cache.Init();
			large_address_stream.rewind();
			here_address = 1;
			for (int i = 0; i < verify_stream.length; ++i) {
				assertEquals(verify_stream[i], cache.DecodeAddress(here_address, mode_stream[i], large_address_stream));
				here_address += 4;
			}
			assertEquals(0, large_address_stream.remaining());
		}
	}

	static void TestEncode(VCDiffAddressCache cache_, ByteBuffer buffer, int address, int here_address, short mode, int size) {
		int startPosition = buffer.position();

		AtomicInteger encoded_addr = new AtomicInteger(0);

		assertEquals(mode, cache_.EncodeAddress(address, here_address, encoded_addr));
		if (cache_.WriteAddressAsVarintForMode(mode)) {
			VarInt.putInt(buffer, encoded_addr.get());
		} else {
			Assert.assertTrue(256 > encoded_addr.get());
			buffer.put((byte)encoded_addr.get());
		}

		assertEquals(size, buffer.position() - startPosition);
	}
}
