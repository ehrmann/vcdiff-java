// Copyright 2007 Google Inc.
// Author: Lincoln Smith
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Implementation of the Address Cache and Address Encoding
// algorithms described in sections 5.1 - 5.4 of RFC 3284 -
// The VCDIFF Generic Differencing and Compression Data Format.
// The RFC text can be found at http://www.faqs.org/rfcs/rfc3284.html
//
// Assumptions:
//   * The VCDAddress type is large enough to hold any offset within
//     the source and target windows.  The limit (for int32_t) is 2^31-1 bytes.
//     The source (dictionary) should not approach this size limit;
//     to compress a target file that is larger than
//     INT_MAX - (dictionary size) bytes, the encoder must
//     break it up into multiple target windows.


package com.googlecode.jvcdiff;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;


public class VCDiffAddressCacheImpl extends VCDiffAddressCache {

	public VCDiffAddressCacheImpl() {
		super();
	}

	/**
	 * The constructor does not initialize near_addresses_ and same_addresses_.
	 * Therefore, Init() must be called before any other method can be used.
	 * 
	 * Because the mode is expressed as a byte value,
	 * near_cache_size + same_cache_size should not exceed 254.
	 * 
	 * @param near_cache_size Size of the NEAR cache (number of 4-byte integers)
	 * @param same_cache_size Size of the SAME cache (number of blocks of 256 4-byte integers per block)
	 */
	public VCDiffAddressCacheImpl(short near_cache_size, short same_cache_size) {
		super(near_cache_size, same_cache_size);
	}

	// Sets up data structures needed to call other methods.  Operations that may
	// fail at runtime (for example, validating the provided near_cache_size_ and
	// same_cache_size_ parameters against their maximum allowed values) are
	// confined to this routine in order to guarantee that the class constructor
	// will never fail.  Other methods (except the destructor) cannot be invoked
	// until this method has been called successfully.  After the object has been
	// initialized and used, Init() can be called again to reset it to its initial
	// state.
	//
	// Return value: "true" if initialization succeeded, "false" if it failed.
	//	     No method may be invoked if this method
	//	     returns false.  The caller is responsible for checking the return value
	//	     and providing an exit path in case of error.
	@Override
	public void Init() {
		// The mode is expressed as a byte value, so there is only room for 256 modes,
		// including the two non-cached modes (SELF and HERE).  Do not allow a larger
		// number of modes to be defined.  We do a separate sanity check for
		// near_cache_size_ and same_cache_size_ because adding them together can
		// cause an integer overflow if each is set to, say, INT_MAX.

		Arrays.fill(near_addresses_, 0);
		Arrays.fill(same_addresses_, 0);

		// in case Init() is called a second time to reinit
		next_slot_ = 0;
	}

	// This method will be called whenever an address is calculated for an
	// encoded or decoded COPY instruction, and will update the contents
	// of the SAME and NEAR caches.  It is vital that the use of
	// UpdateCache (called cache_update in the RFC examples) exactly match
	// the RFC standard, and that the same caching logic be used in the
	// decoder as in the encoder, in order for the decoded addresses to
	// match.
	//
	// Argument:
	//   address: This must be a valid address between 0 and
	//	       (source window size + target window size).  It is assumed that
	//	       these bounds have been checked before calling UpdateCache.
	@Override
	public void UpdateCache(int address) {
		if (near_addresses_.length > 0) {
			near_addresses_[next_slot_] = address;
			next_slot_ = (next_slot_ + 1) % near_addresses_.length;
		}
		if (same_addresses_.length > 0) {
			same_addresses_[address % same_addresses_.length] = address;
		}
	}

	// Determines the address mode that yields the most compact encoding
	// of the given address value, writes the encoded address into the
	// address stream, and returns the mode used.  The most compact encoding
	// is found by looking for the numerically lowest encoded address.
	//
	// Arguments:
	//   address: The address to be encoded.  Must be a non-negative integer
	//	       between 0 and (here_address - 1).
	//   here_address: The current location in the target data (i.e., the
	//	       position just after the last encoded value.)  Must be non-negative.
	//   encoded_addr: Points to an VCDAddress that will be replaced
	//	       with the encoded representation of address.
	//	       If WriteAddressAsVarintForMode returns true when passed
	//	       the return value, then encoded_addr should be written
	//	       into the delta file as a variable-length integer (Varint);
	//	       otherwise, it should be written as a byte (unsigned char).
	//
	// Return value: A mode value between 0 and 255.  The mode will tell
	//	       how to interpret the next value in the address stream.
	//	       The values 0 and 1 correspond to SELF and HERE addressing.
	//
	// The function is guaranteed to succeed unless the conditions on the arguments
	// have not been met, in which case a VCD_DFATAL message will be produced,
	// 0 will be returned, and *encoded_addr will be replaced with 0.
	@Override
	public short EncodeAddress(int address, int here_address,
			AtomicReference<Integer> encoded_addr) {
		if (address < 0) {
			encoded_addr.set(0);
			throw new IllegalArgumentException("EncodeAddress was passed a negative address: " + address);
		}

		if (address >= here_address) {
			encoded_addr.set(0);
			throw new IllegalArgumentException(String.format("EncodeAddress was called with address (%d) < here_address (%d)", address, here_address));
		}

		// Try using the SAME cache.  This method, if available, always
		// results in the smallest encoding and takes priority over other modes.
		if (same_addresses_.length > 0) {
			final int same_cache_pos = address % same_addresses_.length;
			if (same_addresses_[same_cache_pos] == address) {
				// This is the only mode for which an single byte will be written
				// to the address stream instead of a variable-length integer.
				UpdateCache(address);
				encoded_addr.set(same_cache_pos % 256);
				return (short) (FirstSameMode() + (same_cache_pos / 256));  // SAME mode
			}
		}

		// Try SELF mode
		short best_mode = VCD_SELF_MODE;
		int best_encoded_address = address;

		// Try HERE mode
		final int here_encoded_address = here_address - address;
		if (here_encoded_address < best_encoded_address) {
			best_mode = VCD_HERE_MODE;
			best_encoded_address = here_encoded_address;
		}

		// Try using the NEAR cache
		for (int i = 0; i < near_addresses_.length; ++i) {
			final int near_encoded_address = address - near_addresses_[i];
			if ((near_encoded_address >= 0) &&
					(near_encoded_address < best_encoded_address)) {
				best_mode = (short) (VCD_FIRST_NEAR_MODE + i);
				best_encoded_address = near_encoded_address;
			}
		}

		UpdateCache(address);
		encoded_addr.set(best_encoded_address);
		return best_mode;
	}

	// Checks the given decoded address for validity.  Returns true if the
	// address is valid; otherwise, prints an error message to the log and
	// returns false.
	static boolean IsDecodedAddressValid(int decoded_address, int here_address) {
		if (decoded_address < 0) {
			String.format("Decoded address %d is invalid", decoded_address);
			return false;
		} else if (decoded_address >= here_address) {
			String.format("Decoded address (%d) is beyond location in target file (%d)", decoded_address, here_address);
			return false;
		}
		return true;
	}

	// Interprets the next value in the address_stream using the provided mode,
	// which may need to access the SAME or NEAR address cache.  Returns the
	// decoded address.
	// The Init() function must already have been called.
	//
	// Arguments:
	//   here_address: The current location in the source + target data (i.e., the
	//	       location into which the COPY instruction will copy.)  By definition,
	//	       all addresses between 0 and (here_address - 1) are valid, and
	//	       any other address is invalid.
	//   mode: A byte value between 0 and (near_cache_size_ + same_cache_size_ + 1)
	//	       which tells how to interpret the next value in the address stream.
	//	       The values 0 and 1 correspond to SELF and HERE addressing.
	//	       The validity of "mode" should already have been checked before
	//	       calling this function.
	//   address_stream: Points to a pointer holding the position
	//	       in the "Addresses section for COPYs" part of the input data.
	//	       That section must already have been uncompressed
	//	       using a secondary decompressor (if necessary.)
	//	       This is an IN/OUT argument; the value of *address_stream will be
	//	       incremented by the size of an integer, or (if the SAME cache
	//	       was used) by the size of a byte (1).
	//   address_stream_end: Points to the position just after the end of
	//	       the address stream buffer.  All addresses between *address_stream
	//	       and address_stream_end should contain valid address data.
	//
	// Return value: If the input conditions were met, and the address section
	//	     of the input data contains properly encoded addresses that match
	//	     the instructions section, then an integer between 0 and here_address - 1
	//	     will be returned, representing the address from which data should
	//	     be copied from the source or target window into the output stream.
	//	     If an invalid address value is found in address_stream, then
	//	     RESULT_ERROR will be returned.  If the limit address_stream_end
	//	     is reached before the address can be decoded, then
	//	     RESULT_END_OF_DATA will be returned.  If more streamed data
	//	     is expected, this means that the consumer should block and wait
	//	     for more data before continuing to decode.  If no more data is expected,
	//	     this return value signals an error condition.
	@Override
	public int DecodeAddress(int here_address, short vcDiffMode, ByteBuffer address_stream) {
		if (here_address < 0) {
			throw new IllegalArgumentException("DecodeAddress was passed a negative value for here_address: " + here_address);
		}

		if (address_stream.remaining() == 0) {
			return RESULT_END_OF_DATA;
		}

		int decoded_address;
		if (IsSameMode(vcDiffMode)) {
			// SAME mode expects a byte value as the encoded address
			byte encoded_address = address_stream.get();
			decoded_address = DecodeSameAddress(vcDiffMode, encoded_address);
		} else {
			// All modes except SAME mode expect a VarintBE as the encoded address
			int encoded_address = VarInt.getInt(address_stream);
			//VarintBE<int32_t>::Parse(address_stream_end, &new_address_pos);
			switch (encoded_address) {
			case RESULT_ERROR:
				// VCD_ERROR << "Found invalid variable-length integer "
				//            "as encoded address value" << VCD_ENDL;
				return RESULT_ERROR;
			case RESULT_END_OF_DATA:
				return RESULT_END_OF_DATA;
			default:
				break;
			}
			if (IsSelfMode(vcDiffMode)) {
				decoded_address = DecodeSelfAddress(encoded_address);
			} else if (IsHereMode(vcDiffMode)) {
				decoded_address = DecodeHereAddress(encoded_address, here_address);
			} else if (IsNearMode(vcDiffMode)) {
				decoded_address = DecodeNearAddress(vcDiffMode, encoded_address);
			} else {
				throw new IllegalArgumentException(
						"Invalid mode value (" + vcDiffMode + 
						") passed to DecodeAddress; maximum mode value = " +
						this.LastMode());
			}
		}
		// Check for an out-of-bounds address (corrupt/malicious data)
		if (!IsDecodedAddressValid(decoded_address, here_address)) {
			return RESULT_ERROR;
		}

		UpdateCache(decoded_address);
		return decoded_address;
	}
}
