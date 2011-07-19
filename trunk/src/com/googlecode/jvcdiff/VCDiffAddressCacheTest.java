package com.googlecode.jvcdiff;

import junit.framework.Assert;

import org.junit.Test;

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
		
		Assert.assertEquals(VCDiffAddressCache.kDefaultNearCacheSize, cache.near_addresses_.length);
		Assert.assertEquals(VCDiffAddressCache.kDefaultSameCacheSize * 256, cache.same_addresses_.length);
		
		int test_address = 0;
		// Check that caches are initially set to zero
		for (test_address = 0; test_address < cache.near_addresses_.length; ++test_address) {
			Assert.assertEquals(0, cache.near_addresses_[test_address]);
		}
		for (test_address = 0; test_address < cache.same_addresses_.length; ++test_address) {
			Assert.assertEquals(0, cache.same_addresses_[test_address]);
		}
	}
	
	@Test
	public void InsertFirstTen() {
		VCDiffAddressCacheImpl cache = new VCDiffAddressCacheImpl();
		cache.Init();

		for (int test_address = 1; test_address <= 10; ++test_address) {
			cache.UpdateCache(test_address);
		}

		Assert.assertEquals(9, cache.near_addresses_[0]);   // slot 0: 1 => 5 => 9
		Assert.assertEquals(10, cache.near_addresses_[1]);  // slot 1: 2 => 6 => 10
		Assert.assertEquals(7, cache.near_addresses_[2]);   // slot 2: 3 => 7
		Assert.assertEquals(8, cache.near_addresses_[3]);   // slot 3: 4 => 8
		Assert.assertEquals(0, cache.same_addresses_[0]);

		for (int test_address = 1; test_address <= 10; ++test_address) {
			Assert.assertEquals(test_address, cache.same_addresses_[test_address]);
		}
		for (int test_address = 11; test_address < 256 * 3; ++test_address) {
			Assert.assertEquals(0, cache.same_addresses_[test_address]);
		}
	}
	
	@Test
	public void InsertIntMax() {
		VCDiffAddressCacheImpl cache_ = new VCDiffAddressCacheImpl();
		cache_.Init();

		cache_.UpdateCache(Integer.MAX_VALUE);
		Assert.assertEquals(Integer.MAX_VALUE, cache_.near_addresses_[0]);
		Assert.assertEquals(Integer.MAX_VALUE, cache_.same_addresses_[Integer.MAX_VALUE % (256 * 3)]);
		Assert.assertEquals(0, cache_.same_addresses_[(Integer.MAX_VALUE - 256) % (256 * 3)]);
		Assert.assertEquals(0, cache_.same_addresses_[(Integer.MAX_VALUE - 512) % (256 * 3)]);
	}
}
