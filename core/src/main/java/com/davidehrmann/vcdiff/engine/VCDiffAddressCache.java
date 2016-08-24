package com.davidehrmann.vcdiff.engine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

abstract class VCDiffAddressCache {

    public static final short VCD_SELF_MODE = (short)0;
    public static final short VCD_HERE_MODE = (short)1;

    // Returns the first mode number that represents one of the NEAR modes.
    // The number of NEAR modes is near_cache_size.  Each NEAR mode refers to
    // an element of the near_addresses_ array, where a recently-referenced
    // address is stored.
    public static final short VCD_FIRST_NEAR_MODE = (short)2;
    public static final short VCD_MAX_MODES = (short)256;

    // The default cache sizes specified in the RFC
    static final short kDefaultNearCacheSize = 4;
    static final short kDefaultSameCacheSize = 3;

    /**
     * An error occurred while performing the requested operation.
     */
    // static final int RESULT_ERROR = -1;

    /**
     * The end of available data was reached before the requested operation could be completed.
     */
    static final int RESULT_END_OF_DATA = -2;

    // The next position in the NEAR cache to which an address will be written.
    int       next_slot_;

    // NEAR cache contents
    final int[] near_addresses_;

    // SAME cache contents
    final int[] same_addresses_;

    protected VCDiffAddressCache() {
        this(kDefaultNearCacheSize, kDefaultSameCacheSize);
    }

    protected VCDiffAddressCache(short near_cache_size, short same_cache_size) {
        if ((near_cache_size > (VCD_MAX_MODES - 2)) || (near_cache_size < 0)) {
            throw new IllegalArgumentException("Near cache size " + near_cache_size + " is invalid");
        }

        if ((same_cache_size > (VCD_MAX_MODES - 2)) || (same_cache_size < 0)) {
            throw new IllegalArgumentException("Same cache size " + same_cache_size + " is invalid");
        }

        if ((near_cache_size + same_cache_size) > VCD_MAX_MODES - 2) {
            throw new IllegalArgumentException(
                    "Using near cache size " + near_cache_size +
                    " and same cache size " + same_cache_size +
                    " would exceed maximum number of COPY modes (" +
                    VCD_MAX_MODES);
        }

        this.near_addresses_ = new int[near_cache_size];
        this.same_addresses_ = new int[same_cache_size * 256];
    }

    // Initializes the object before use.  This method must be called after
    // constructing a VCDiffAddressCache/ object, before any other method may be
    // called.  This is because init() validates near_cache_size_ and
    // same_cache_size_ before initializing the same and near caches.  After the
    // object has been initialized and used, init() can be called again to reset
    // it to its initial state.
    public abstract void Init();

    /*
    public short near_cache_size() {
        return near_cache_size;
    }

    int same_cache_size() {
        return same_cache_size;
    }
    */

    // Returns the first mode number that represents one of the SAME modes.
    // The number of SAME modes is same_cache_size.  Each SAME mode refers to
    // a block of 256 elements of the same_addresses_ array; the lowest-order
    // 8 bits of the address are used to find the element of this block that
    // may match the desired address value.
    public short FirstSameMode() {
        return (short) (VCD_FIRST_NEAR_MODE + near_addresses_.length);
    }

    // Returns the maximum valid mode number, which happens to be
    // the last SAME mode.
    public short LastMode() {
        return (byte) (FirstSameMode() + (same_addresses_.length / 256) - 1);
    }

    static byte DefaultLastMode() {
        return (byte) (VCD_FIRST_NEAR_MODE + kDefaultNearCacheSize + kDefaultSameCacheSize - 1);
    }

    // See the definition of enum VCDiffModes in vcdiff_defs.h,
    // as well as section 5.3 of the RFC, for a description of
    // each address mode type (SELF, HERE, NEAR, and SAME).
    static boolean IsSelfMode(short mode) {
        return mode == VCD_SELF_MODE;
    }

    static boolean IsHereMode(short mode) {
        return mode == VCD_HERE_MODE;
    }

    boolean IsNearMode(short mode) {
        return (mode >= VCD_FIRST_NEAR_MODE) && (mode < FirstSameMode());
    }

    boolean IsSameMode(short mode) {
        return (mode >= FirstSameMode()) && (mode <= LastMode());
    }

    static int DecodeSelfAddress(int encoded_address) {
        return encoded_address;
    }

    static int DecodeHereAddress(int encoded_address,
            int here_address) {
        return here_address - encoded_address;
    }

    int DecodeNearAddress(short mode,
            int encoded_address) {
        return near_addresses_[mode - VCD_FIRST_NEAR_MODE] + encoded_address;
    }

    public final int DecodeSameAddress(short mode, short encoded_address) {
        return same_addresses_[(mode - FirstSameMode()) * 256 + encoded_address];
    }

    // Returns true if, when using the given mode, an encoded address
    // should be written to the delta file as a variable-length integer;
    // returns false if the encoded address should be written
    // as a byte value (unsigned char).
    public final boolean WriteAddressAsVarintForMode(short mode) {
        return !IsSameMode(mode);
    }

    // This method will be called whenever an address is calculated for an
    // encoded or decoded COPY instruction, and will update the contents
    // of the SAME and NEAR caches.
    //
    public abstract void UpdateCache(int address);

    // Determines the address mode that yields the most compact encoding
    // of the given address value.  The most compact encoding
    // is found by looking for the numerically lowest encoded address.
    // Sets *encoded_addr to the encoded representation of the address
    // and returns the mode used.
    //
    // The caller should pass the return value to the method
    // WriteAddressAsVarintForMode() to determine whether encoded_addr
    // should be written to the delta file as a variable-length integer
    // or as a byte (unsigned char).
    //
    public abstract short EncodeAddress(int address,
            int here_address,
            AtomicInteger encoded_addr);

    /**
     * Interprets the next value in the address_stream using the provided mode,
     * which may need to access the SAME or NEAR address cache.
     * @param here_address
     * @param vcDiffMode
     * @param address_stream
     * @return If successful, the new offset will be returned.
     *				RESULT_END_OF_DATA: The limit address_stream_end was reached before
     *				the address could be decoded.  If more streamed data is expected,
     *				this means that the consumer should block and wait for more data
     *				before continuing to decode.  If no more data is expected, this
     *				return value signals an error condition.
     * @throws IOException if an invalid address value was found in address_stream.
     */
    public abstract int DecodeAddress(int here_address, short vcDiffMode, ByteBuffer address_stream) throws IOException;
}
