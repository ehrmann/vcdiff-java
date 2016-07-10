// Copyright 2008 Google Inc.
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

package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.VarInt;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.Adler32;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_CHECKSUM;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;

public abstract class VCDiffDecoderTest {

    protected static final Charset US_ASCII = Charset.forName("US-ASCII");

    private static final byte[] kStandardFileHeader = {
            (byte) 0xD6,  // 'V' | 0x80
            (byte) 0xC3,  // 'C' | 0x80
            (byte) 0xC4,  // 'D' | 0x80
            0x00,         // Draft standard version number
            0x00          // Hdr_Indicator: no custom code table, no compression
    };

    private static final byte[] kInterleavedFileHeader = {
            (byte) 0xD6,  // 'V' | 0x80
            (byte) 0xC3,  // 'C' | 0x80
            (byte) 0xC4,  // 'D' | 0x80
            'S',          // SDCH version code
            0x00          // Hdr_Indicator: no custom code table, no compression
    };

    protected static final byte[] kDictionary = (
            "\"Just the place for a Snark!\" the Bellman cried,\n" +
                    "As he landed his crew with care;\n" +
                    "Supporting each man on the top of the tide\n" +
                    "By a finger entwined in his hair.\n"
    ).getBytes(US_ASCII);

    protected static final byte[] kExpectedTarget = (
            "\"Just the place for a Snark! I have said it twice:\n" +
                    "That alone should encourage the crew.\n" +
                    "Just the place for a Snark! I have said it thrice:\n" +
                    "What I tell you three times is true.\"\n"
    ).getBytes(US_ASCII);

    // These two counters are used by FuzzOneByteInDeltaFile() to iterate through
    // different ways to corrupt the delta file.
    private int fuzzer_;
    private int fuzzed_byte_position_;

    protected VCDiffStreamingDecoder decoder_ = new VCDiffStreamingDecoderImpl();

    // delta_file_ will be populated by InitializeDeltaFile() using the components
    // delta_file_header_, delta_window_header_, and delta_window_body_.
    protected byte[] delta_file_;

    // This string is not populated during setup, but is used to receive the
    // decoded target file in each test.
    protected ByteArrayOutputStream output_ = new ByteArrayOutputStream();

    // Test fixtures that inherit from VCDiffDecoderTest can set these strings in
    // their constructors to override their default values (which come from
    // kDictionary, kExpectedTarget, etc.)
    protected byte[] dictionary_;
    protected byte[] expected_target_;

    // The components that will be used to construct delta_file_.
    protected byte[] delta_file_header_;
    protected byte[] delta_window_header_;
    protected byte[] delta_window_body_;


    protected VCDiffDecoderTest() {
        fuzzer_ = 0;
        fuzzed_byte_position_ = 0;
        dictionary_ = kDictionary.clone();
        expected_target_ = kExpectedTarget.clone();
    }

    @Before
    public void SetUp() {
        InitializeDeltaFile();
    }

    // These functions populate delta_file_header_ with a standard or interleaved
    // file header.
    protected void UseStandardFileHeader() {
        delta_file_header_ = kStandardFileHeader.clone();
    }

    // These functions populate delta_file_header_ with a standard or interleaved
    // file header.
    protected void UseInterleavedFileHeader() {
        delta_file_header_ = kInterleavedFileHeader.clone();
    }

    // This function is called by SetUp().  It populates delta_file_ with the
    // concatenated delta file header, delta window header, and delta window
    // body, plus (if UseChecksum() is true) the corresponding checksum.
    // It can be called again by a test that has modified the contents of
    // delta_file_ and needs to restore them to their original state.
    protected void InitializeDeltaFile() {
        delta_file_ = new byte[delta_file_header_.length + delta_window_header_.length + delta_window_body_.length];
        ByteBuffer.wrap(delta_file_)
                .put(delta_file_header_)
                .put(delta_window_header_)
                .put(delta_window_body_);
    }

    // Assuming the length of the given string can be expressed as a VarintBE
    // of length N, this function returns the byte at position which_byte, where
    // 0 <= which_byte < N.
    protected static byte GetByteFromStringLength(byte[] s, int which_byte) {
        // FIXME: hard-coded length
        ByteBuffer buffer = ByteBuffer.allocate(8);
        VarInt.putInt(buffer, s.length);
        buffer.flip();
        return buffer.get(which_byte);
    }

    // Assuming the length of the given string can be expressed as a one-byte
    // VarintBE, this function returns that byte value.
    protected static byte StringLengthAsByte(byte[] s) {
        return GetByteFromStringLength(s, 0);
    }

    // Assuming the length of the given string can be expressed as a two-byte
    // VarintBE, this function returns the first byte of its representation.
    protected static byte FirstByteOfStringLength(byte[] s) {
        return GetByteFromStringLength(s, 0);
    }

    // Assuming the length of the given string can be expressed as a two-byte
    // VarintBE, this function returns the second byte of its representation.
    protected static byte SecondByteOfStringLength(byte[] s) {
        return GetByteFromStringLength(s, 1);
    }

    // This function adds an Adler32 checksum to the delta window header.
    protected void AddChecksum(int checksum) {
        int checksumLength = VarInt.calculateIntLength(checksum);
        int oldDeltaWindowHeaderLength = delta_window_header_.length;

        delta_window_header_ = Arrays.copyOf(delta_window_header_, oldDeltaWindowHeaderLength + checksumLength);
        delta_window_header_[0] |= VCD_CHECKSUM;
        VarInt.putInt(ByteBuffer.wrap(delta_window_header_, oldDeltaWindowHeaderLength, checksumLength), checksum);

        // Adjust delta window size to include checksum.
        // This method wouldn't work if adding to the length caused the VarintBE
        // value to spill over into another byte.  Luckily, this test data happens
        // not to cause such an overflow.
        delta_window_header_[4] += VarInt.calculateIntLength(checksum);
    }

    // This function computes the Adler32 checksum for the expected target
    // and adds it to the delta window header.
    protected void ComputeAndAddChecksum() {
        Adler32 adler32 = new Adler32();
        adler32.update(expected_target_);
        AddChecksum((int) adler32.getValue());
    }

    // Write the maximum expressible positive 32-bit VarintBE
    // (0x7FFFFFFF) at the given offset in the delta window.
    protected void WriteMaxVarintAtOffset(int offset, int bytes_to_replace) {
        byte[] kMaxVarint = { (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F };
        byte[] header = Arrays.copyOfRange(delta_file_, 0, delta_file_header_.length + offset);
        byte[] trailer = Arrays.copyOfRange(delta_file_, delta_file_header_.length + offset + bytes_to_replace, delta_file_.length);

        delta_file_  = new byte[header.length + kMaxVarint.length + trailer.length];
        ByteBuffer.wrap(delta_file_)
                .put(header)
                .put(kMaxVarint)
                .put(trailer);
    }

    // Write a negative 32-bit VarintBE (0x80000000) at the given offset
    // in the delta window.
    protected void WriteNegativeVarintAtOffset(int offset, int bytes_to_replace) {
        byte[] kNegativeVarint = { (byte) 0x88, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00 };
        byte[] header = Arrays.copyOfRange(delta_file_, 0, delta_file_header_.length + offset);
        byte[] trailer = Arrays.copyOfRange(delta_file_, delta_file_header_.length + offset + bytes_to_replace, delta_file_.length);

        delta_file_  = new byte[header.length + kNegativeVarint.length + trailer.length];
        ByteBuffer.wrap(delta_file_)
                .put(header)
                .put(kNegativeVarint)
                .put(trailer);
    }

    // Write a VarintBE that has too many continuation bytes
    // at the given offset in the delta window.
    protected void WriteInvalidVarintAtOffset(int offset, int bytes_to_replace) {
        byte[] kInvalidVarint = { (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F };
        byte[] header = Arrays.copyOfRange(delta_file_, 0, delta_file_header_.length + offset);
        byte[] trailer = Arrays.copyOfRange(delta_file_, delta_file_header_.length + offset + bytes_to_replace, delta_file_.length);

        delta_file_  = new byte[header.length + kInvalidVarint.length + trailer.length];
        ByteBuffer.wrap(delta_file_)
                .put(header)
                .put(kInvalidVarint)
                .put(trailer);
    }

    // This function iterates through a list of fuzzers (bit masks used to corrupt
    // bytes) and through positions in the delta file.  Each time it is called, it
    // attempts to corrupt a different byte in delta_file_ in a different way.  If
    // successful, it returns true. Once it exhausts the list of fuzzers and of
    // byte positions in delta_file_, it returns false.
    protected boolean FuzzOneByteInDeltaFile() {
        Fuzzer[] fuzzers = {
                new Fuzzer((byte) 0xff, (byte) 0x80, (byte) 0x00),
                new Fuzzer((byte) 0xff, (byte) 0xff, (byte) 0x00),
                new Fuzzer((byte) 0xff, (byte) 0x00, (byte) 0x80),
                new Fuzzer((byte) 0xff, (byte) 0x00, (byte) 0xff),
                new Fuzzer((byte) 0xff, (byte) 0x01, (byte) 0x00),
                new Fuzzer((byte) 0x7f, (byte) 0x00, (byte) 0x00),
        };

        for (; fuzzer_ < fuzzers.length; ++fuzzer_) {
            for (; fuzzed_byte_position_ < delta_file_.length; ++fuzzed_byte_position_) {
                byte fuzzed_byte = (byte) (((delta_file_[fuzzed_byte_position_]
                        & fuzzers[fuzzer_]._and)
                        | fuzzers[fuzzer_]._or)
                        ^ fuzzers[fuzzer_]._xor);
                if (fuzzed_byte != delta_file_[fuzzed_byte_position_]) {
                    delta_file_[fuzzed_byte_position_] = fuzzed_byte;
                    ++fuzzed_byte_position_;
                    return true;
                }
            }
            fuzzed_byte_position_ = 0;
        }
        return false;
    }

    protected static abstract class VCDiffInterleavedDecoderTest extends VCDiffDecoderTest {
        private final byte[] kWindowHeader = {
                VCD_SOURCE,  // Win_Indicator: take source from dictionary
                FirstByteOfStringLength(kDictionary),  // Source segment size
                SecondByteOfStringLength(kDictionary),
                0x00,  // Source segment position: start of dictionary
                0x79,  // Length of the delta encoding
                FirstByteOfStringLength(kExpectedTarget),  // Size of the target window
                SecondByteOfStringLength(kExpectedTarget),
                0x00,  // Delta_indicator (no compression)
                0x00,  // length of data for ADDs and RUNs (unused)
                0x73,  // length of interleaved section
                0x00  // length of addresses for COPYs (unused)
        };

        private final byte[] kWindowBody = {
                0x13,  // VCD_COPY mode VCD_SELF, size 0
                0x1C,  // Size of COPY (28)
                0x00,  // Address of COPY: Start of dictionary
                0x01,  // VCD_ADD size 0
                0x3D,  // Size of ADD (61)
                // Data for ADD (length 61)
                ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
                'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
                'T', 'h', 'a', 't', ' ',
                'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
                'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
                't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
                0x23,  // VCD_COPY mode VCD_HERE, size 0
                0x2C,  // Size of COPY (44)
                0x58,  // HERE mode address (27+61 back from here_address)
                (byte) 0xCB,  // VCD_ADD size 2 + VCD_COPY mode NEAR(1), size 5
                // Data for ADDs: 2nd section (length 2)
                'h', 'r',
                0x2D,  // NEAR(1) mode address (45 after prior address)
                0x0A,  // VCD_ADD size 9
                // Data for ADDs: 3rd section (length 9)
                'W', 'h', 'a', 't', ' ',
                'I', ' ', 't', 'e',
                0x00,  // VCD_RUN size 0
                0x02,  // Size of RUN (2)
                // Data for RUN: 4th section (length 1)
                'l',
                0x01,  // VCD_ADD size 0
                0x1B,  // Size of ADD (27)
                // Data for ADD: 4th section (length 27)
                ' ', 'y', 'o', 'u', ' ',
                't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
                't', 'r', 'u', 'e', '.', '\"', '\n'
        };

        protected VCDiffInterleavedDecoderTest() {
            UseInterleavedFileHeader();
            delta_window_header_ = kWindowHeader;
            delta_window_body_ = kWindowBody;
        }
    }

    private static final class Fuzzer {
        final byte  _and;
        final byte  _or;
        final byte  _xor;

        private Fuzzer(byte and, byte or, byte xor) {
            _and = and;
            _or = or;
            _xor = xor;
        }
    }
}
