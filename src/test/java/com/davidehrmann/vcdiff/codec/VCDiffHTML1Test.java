// Copyright 2008-2016 Google Inc., David Ehrmann
// Author: Lincoln Smith, David Ehrmann
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

package com.davidehrmann.vcdiff.codec;

import com.davidehrmann.vcdiff.BlockHash;
import org.junit.Test;

import static com.davidehrmann.vcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class VCDiffHTML1Test extends VerifyEncodedBytesTest {
    protected static final byte[] kDictionary =
            "<html><font color=red>This part from the dict</font><br>\0".getBytes(US_ASCII);

    protected static final byte[] kTarget = (
            "<html><font color=red>This part from the dict</font><br>\n"
            + "And this part is not...</html>"
    ).getBytes(US_ASCII);

    protected static final byte[] kRedundantTarget = (
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"  // 256
    ).getBytes(US_ASCII);

    public VCDiffHTML1Test() {
        super(kDictionary, kTarget);
    }

    @Test
    public void CheckOutputOfSimpleEncoder() throws Exception {
        SimpleEncode();

        EncodedBytesVerifier verifier = new EncodedBytesVerifier(delta_.toByteArray());

        // These values do not depend on the block size used for encoding
        verifier.ExpectByte((byte) 0xD6);  // 'V' | 0x80
        verifier.ExpectByte((byte) 0xC3);  // 'C' | 0x80
        verifier.ExpectByte((byte) 0xC4);  // 'D' | 0x80
        verifier.ExpectByte((byte) 0x00);  // Simple encoder never uses interleaved format
        verifier.ExpectByte((byte) 0x00);  // Hdr_Indicator
        verifier.ExpectByte((byte) VCD_SOURCE);  // Win_Indicator: VCD_SOURCE (dictionary)
        verifier.ExpectByte((byte) kDictionary.length);  // Dictionary length
        verifier.ExpectByte((byte) 0x00);  // Source segment position: start of dictionary
        //noinspection ConstantConditions
        if (BlockHash.kBlockSize < 16) {
            // A medium block size will catch the "his part " match.
            verifier.ExpectByte((byte) 0x22);  // Length of the delta encoding
            verifier.ExpectSize(kTarget.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectByte((byte) 0x16);  // Length of the data section
            verifier.ExpectByte((byte) 0x05);  // Length of the instructions section
            verifier.ExpectByte((byte) 0x02);  // Length of the address section
            // Data section
            verifier.ExpectString("\nAnd t".getBytes(US_ASCII));      // Data for 1st ADD
            verifier.ExpectString("is not...</html>".getBytes(US_ASCII));  // Data for 2nd ADD
            // Instructions section
            verifier.ExpectByte((byte) 0x73);  // COPY size 0 mode VCD_SAME(0)
            verifier.ExpectByte((byte) 0x38);  // COPY size (56)
            verifier.ExpectByte((byte) 0x07);  // ADD size 6
            verifier.ExpectByte((byte) 0x19);  // COPY size 9 mode VCD_SELF
            verifier.ExpectByte((byte) 0x11);  // ADD size 16
            // Address section
            verifier.ExpectByte((byte) 0x00);  // COPY address (0) mode VCD_SAME(0)
            verifier.ExpectByte((byte) 0x17);  // COPY address (23) mode VCD_SELF
        } else //noinspection ConstantConditions
            if (BlockHash.kBlockSize <= 56) {
            // Any block size up to 56 will catch the matching prefix string.
            verifier.ExpectByte((byte) 0x29);  // Length of the delta encoding
            verifier.ExpectSize(kTarget.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectByte((byte) 0x1F);  // Length of the data section
            verifier.ExpectByte((byte) 0x04);  // Length of the instructions section
            verifier.ExpectByte((byte) 0x01);  // Length of the address section
            verifier.ExpectString("\nAnd this part is not...</html>".getBytes(US_ASCII));  // Data for ADD
            // Instructions section
            verifier.ExpectByte((byte) 0x73);  // COPY size 0 mode VCD_SAME(0)
            verifier.ExpectByte((byte) 0x38);  // COPY size (56)
            verifier.ExpectByte((byte) 0x01);  // ADD size 0
            verifier.ExpectByte((byte) 0x1F);  // Size of ADD (31)
            // Address section
            verifier.ExpectByte((byte) 0x00);  // COPY address (0) mode VCD_SAME(0)
        } else {
            // The matching string is 56 characters long, and the block size is
            // 64 or greater, so no match should be found.
            verifier.ExpectSize(kTarget.length + 7);  // Delta encoding len
            verifier.ExpectSize(kTarget.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectSize(kTarget.length);  // Length of the data section
            verifier.ExpectByte((byte) 0x02);  // Length of the instructions section
            verifier.ExpectByte((byte) 0x00);  // Length of the address section
            // Data section
            verifier.ExpectString(kTarget);
            verifier.ExpectByte((byte) 0x01);  // ADD size 0
            verifier.ExpectSize((byte) kTarget.length);
        }
        verifier.ExpectNoMoreBytes();
    }

    @Test
    public void SimpleEncoderPerformsTargetMatching() throws Exception {
        simple_encoder_.Encode(kRedundantTarget, delta_);
        assertTrue(kRedundantTarget.length + kFileHeaderSize + kWindowHeaderSize >=
                delta_.size());
        simple_decoder_.Decode(kDictionary, delta_.toByteArray(), result_target_);
        assertArrayEquals(kRedundantTarget, result_target_.toByteArray());

        EncodedBytesVerifier verifier = new EncodedBytesVerifier(delta_.toByteArray());

        // These values do not depend on the block size used for encoding
        verifier.ExpectByte((byte) 0xD6);  // 'V' | 0x80
        verifier.ExpectByte((byte) 0xC3);  // 'C' | 0x80
        verifier.ExpectByte((byte) 0xC4);  // 'D' | 0x80
        verifier.ExpectByte((byte) 0x00);  // Simple encoder never uses interleaved format
        verifier.ExpectByte((byte) 0x00);  // Hdr_Indicator
        verifier.ExpectByte((byte) VCD_SOURCE);  // Win_Indicator: VCD_SOURCE (dictionary)
        verifier.ExpectByte((byte) kDictionary.length);  // Dictionary length
        verifier.ExpectByte((byte) 0x00);  // Source segment position: start of dictionary
        verifier.ExpectByte((byte) 0x0C);  // Length of the delta encoding
        verifier.ExpectSize(kRedundantTarget.length);  // Size of the target window
        verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
        verifier.ExpectByte((byte) 0x01);  // Length of the data section
        verifier.ExpectByte((byte) 0x04);  // Length of the instructions section
        verifier.ExpectByte((byte) 0x01);  // Length of the address section
        // Data section
        verifier.ExpectString("A".getBytes(US_ASCII));      // Data for ADD
        // Instructions section
        verifier.ExpectByte((byte) 0x02);  // ADD size 1
        verifier.ExpectByte((byte) 0x23);  // COPY size 0 mode VCD_HERE
        verifier.ExpectSize(kRedundantTarget.length - 1);  // COPY size 255
        // Address section
        verifier.ExpectByte((byte) 0x01);  // COPY address (1) mode VCD_HERE
        verifier.ExpectNoMoreBytes();
    }

    @Test
    public void SimpleEncoderWithoutTargetMatching() throws Exception {
        simple_encoder_ = EncoderBuilder.builder()
                .withDictionary(kDictionary)
                .withTargetMatches(false)
                .buildSimple();

        simple_encoder_.Encode(kRedundantTarget, delta_);
        assertTrue(kRedundantTarget.length + kFileHeaderSize + kWindowHeaderSize >=
                delta_.size());
        simple_decoder_.Decode(kDictionary, delta_.toByteArray(), result_target_);
        assertArrayEquals(kRedundantTarget, result_target_.toByteArray());

        EncodedBytesVerifier verifier = new EncodedBytesVerifier(delta_.toByteArray());

        // These values do not depend on the block size used for encoding
        verifier.ExpectByte((byte) 0xD6);  // 'V' | 0x80
        verifier.ExpectByte((byte) 0xC3);  // 'C' | 0x80
        verifier.ExpectByte((byte) 0xC4);  // 'D' | 0x80
        verifier.ExpectByte((byte) 0x00);  // Simple encoder never uses interleaved format
        verifier.ExpectByte((byte) 0x00);  // Hdr_Indicator
        verifier.ExpectByte((byte) VCD_SOURCE);  // Win_Indicator: VCD_SOURCE (dictionary)
        verifier.ExpectByte((byte) kDictionary.length);  // Dictionary length
        verifier.ExpectByte((byte) 0x00);  // Source segment position: start of dictionary
        verifier.ExpectSize(kRedundantTarget.length + 0x0A);  // Length of the delta encoding
        verifier.ExpectSize(kRedundantTarget.length);  // Size of the target window
        verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
        verifier.ExpectSize(kRedundantTarget.length);  // Length of the data section
        verifier.ExpectByte((byte) 0x03);  // Length of the instructions section
        verifier.ExpectByte((byte) 0x00);  // Length of the address section
        // Data section
        verifier.ExpectString(kRedundantTarget);      // Data for ADD
        // Instructions section
        verifier.ExpectByte((byte) 0x01);  // ADD size 0
        verifier.ExpectSize(kRedundantTarget.length);  // ADD size
        // Address section empty
        verifier.ExpectNoMoreBytes();
    }

}