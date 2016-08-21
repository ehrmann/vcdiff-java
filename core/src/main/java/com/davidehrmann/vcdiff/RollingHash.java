package com.davidehrmann.vcdiff;

import java.nio.ByteBuffer;

public class RollingHash {
    private final int window_size;

    // We keep a table that maps from any byte "b" to
    //    (- b * pow(kMult, window_size - 1)) % kBase
    private final long[] remove_table;


    public RollingHash(int window_size) {
        if (window_size < 2) {
            throw new IllegalArgumentException();
        }
        this.window_size = window_size;

        remove_table = RollingHashUtil.BuildRemoveTable(window_size);
    }

    // Compute a hash of the window "ptr[0, window_size - 1]".
    public long Hash(byte[] data, int offset, int length) {
        long h = RollingHashUtil.HashFirstTwoBytes(data, offset);
        for (int i = 2; i < window_size; ++i) {
            h = RollingHashUtil.HashStep(h, data[offset + i]);
        }
        return h;
    }

    public long Hash(ByteBuffer data) {
        long h = RollingHashUtil.HashFirstTwoBytes(data);
        for (int i = 2; i < window_size; ++i) {
            h = RollingHashUtil.HashStep(h, data.get());
        }
        return h;
    }

    // Update a hash by removing the oldest byte and adding a new byte.
    //
    // UpdateHash takes the hash value of buffer[0] ... buffer[window_size -1]
    // along with the value of buffer[0] (the "old_first_byte" argument)
    // and the value of buffer[window_size] (the "new_last_byte" argument).
    // It quickly computes the hash value of buffer[1] ... buffer[window_size]
    // without having to run Hash() on the entire window.
    //
    // The larger the window, the more advantage comes from using UpdateHash()
    // (which runs in time independent of window_size) instead of Hash().
    // Each time window_size doubles, the time to execute Hash() also doubles,
    // while the time to execute UpdateHash() remains constant.  Empirical tests
    // have borne out this statement.
    public long UpdateHash(long old_hash, byte old_first_byte, byte new_last_byte) {
        long partial_hash = RemoveFirstByteFromHash(old_hash, old_first_byte);
        return RollingHashUtil.HashStep(partial_hash, new_last_byte);
    }

    // Given a full hash value for buffer[0] ... buffer[window_size -1], plus the
    // value of the first byte buffer[0], this function returns a *partial* hash
    // value for buffer[1] ... buffer[window_size -1].  See the comments in
    // init(), below, for a description of how the contents of remove_table_ are
    // computed.
    protected long RemoveFirstByteFromHash(long full_hash, byte first_byte) {
        return RollingHashUtil.ModBase(full_hash + remove_table[first_byte & 0xff]);
    }

    protected static class RollingHashUtil {
        // Multiplier for incremental hashing.  The compiler should be smart enough to
        // convert (val * kMult) into ((val << 8) + val).
        public static final int kMult = 257;

        // All hashes are returned modulo "kBase".  Current implementation requires
        // kBase <= 2^32/kMult to avoid overflow.  Also, kBase must be a power of two
        // so that we can compute modulus efficiently.
        public static final int kBase = (1 << 23);

        // Returns operand % kBase, assuming that kBase is a power of two.
        public static long ModBase(long operand) {
            return operand & (kBase - 1);
        }

        // Given an unsigned integer "operand", returns an unsigned integer "result"
        // such that
        //     result < kBase
        // and
        //     ModBase(operand + result) == 0
        public static long FindModBaseInverse(long operand) {
            // The subtraction (0 - operand) produces an unsigned underflow for any
            // operand except 0.  The underflow results in a (very large) unsigned
            // number.  Binary subtraction is used instead of unary negation because
            // some compilers (e.g. Visual Studio 7+) produce a warning if an unsigned
            // value is negated.
            //
            // The C++ mod operation (operand % kBase) may produce different results for
            // different compilers if operand is negative.  That is not a problem in
            // this case, since all numbers used are unsigned, and ModBase does its work
            // using bitwise arithmetic rather than the % operator.
            return (0x100000000L - ModBase(operand)) & 0xFFFFFFFFL;
        }

        // Here's the heart of the hash algorithm.  Start with a partial_hash value of
        // 0, and run this HashStep once against each byte in the data window to be
        // hashed.  The result will be the hash value for the entire data window.  The
        // Hash() function, below, does exactly this, albeit with some refinements.
        public static long HashStep(long partial_hash, byte next_byte) {
            return ModBase((partial_hash * kMult) + (next_byte & 0xff));
        }

        // Use this function to start computing a new hash value based on the first
        // two bytes in the window.  It is equivalent to calling
        //     HashStep(HashStep(0, ptr[0]), ptr[1])
        // but takes advantage of the fact that the maximum value of
        // (ptr[0] * kMult) + ptr[1] is not large enough to exceed kBase, thus
        // avoiding an unnecessary ModBase operation.
        public static long HashFirstTwoBytes(byte[] ptr, int offset) {
            return ((ptr[offset] & 0xff) * kMult) + (ptr[offset + 1] & 0xff);
        }

        public static long HashFirstTwoBytes(ByteBuffer data) {
            long hash = (data.get() & 0xff) * kMult;
            hash += (data.get() & 0xff);
            return hash;
        }

        public static long[] BuildRemoveTable(int window_size) {
            if (window_size < 2) {
                throw new IllegalArgumentException();
            }

            // The new object is placed into a local pointer instead of directly into
            // remove_table_, for two reasons:
            //   1. remove_table_ is a pointer to const.  The table is populated using
            //      the non-const local pointer and then assigned to the global const
            //      pointer once it's ready.
            //   2. No other thread will ever see remove_table_ pointing to a
            //      partially-initialized table.  If two threads happen to call init()
            //      at the same time, two tables with the same contents may be created
            //      (causing a memory leak), but the results will be consistent
            //      no matter which of the two tables is used.
            long[] remove_table = new long[256];
            // Compute multiplier.  Concisely, it is:
            //     pow(kMult, (window_size - 1)) % kBase,
            // but we compute the power in integer form.
            long multiplier = 1;
            for (int i = 0; i < window_size - 1; ++i) {
                multiplier = RollingHashUtil.ModBase(multiplier * RollingHashUtil.kMult);
            }
            // For each character removed_byte, compute
            //     remove_table_[removed_byte] ==
            //         (- (removed_byte * pow(kMult, (window_size - 1)))) % kBase
            // where the power operator "pow" is taken in integer form.
            //
            // If you take a hash value fp representing the hash of
            //     buffer[0] ... buffer[window_size - 1]
            // and add the value of remove_table_[buffer[0]] to it, the result will be
            // a partial hash value for
            //     buffer[1] ... buffer[window_size - 1]
            // that is to say, it no longer includes buffer[0].
            //
            // The following byte at buffer[window_size] can then be merged with this
            // partial hash value to arrive quickly at the hash value for a window that
            // has advanced by one byte, to
            //     buffer[1] ... buffer[window_size]
            // In fact, that is precisely what happens in UpdateHash, above.
            long byte_times_multiplier = 0;
            for (int removed_byte = 0; removed_byte < 256; ++removed_byte) {
                remove_table[removed_byte] = RollingHashUtil.FindModBaseInverse(byte_times_multiplier);
                // Iteratively adding the multiplier in this loop is equivalent to
                // computing (removed_byte * multiplier), and is faster
                byte_times_multiplier = RollingHashUtil.ModBase(byte_times_multiplier + multiplier);
            }

            return remove_table;
        }
    }
}
