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
import com.davidehrmann.vcdiff.VarInt;
import com.davidehrmann.vcdiff.ZeroInitializedAdler32;
import org.junit.Assert;
import org.junit.Test;

import java.util.zip.Adler32;

import static com.davidehrmann.vcdiff.VCDiffCodeTableWriter.VCD_CHECKSUM;
import static com.davidehrmann.vcdiff.VCDiffCodeTableWriter.VCD_SOURCE;

public class VCDiffHTML2Test extends VerifyEncodedBytesTest {
    protected static final byte[] kDictionary = "10\nThis is a test\0".getBytes(US_ASCII);
    protected static final byte[] kTarget = "This is a test!!!\n".getBytes(US_ASCII);

    public VCDiffHTML2Test() {
        super(kDictionary, kTarget);
    }

    @Test
    public void VerifyOutputOfSimpleEncoder() throws Exception {
        SimpleEncode();

        EncodedBytesVerifier verifier = new EncodedBytesVerifier(delta_.toByteArray());

        // These values do not depend on the block size used for encoding
        verifier.ExpectByte((byte) 0xD6);  // 'V' | 0x80
        verifier.ExpectByte((byte) 0xC3);  // 'C' | 0x80
        verifier.ExpectByte((byte) 0xC4);  // 'D' | 0x80
        verifier.ExpectByte((byte) 0x00);  // Simple encoder never uses interleaved format
        verifier.ExpectByte((byte) 0x00);  // Hdr_Indicator
        verifier.ExpectByte((byte) VCD_SOURCE);  // Win_Indicator: VCD_SOURCE (dictionary)
        verifier.ExpectByte((byte) dictionary_.length);  // Dictionary length
        verifier.ExpectByte((byte) 0x00);  // Source segment position: start of dictionary
        //noinspection ConstantConditions
        if (BlockHash.kBlockSize <= 8) {
            verifier.ExpectByte((byte) 12);  // Length of the delta encoding
            verifier.ExpectSize(target_.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectByte((byte) 0x04);  // Length of the data section
            verifier.ExpectByte((byte) 0x02);  // Length of the instructions section
            verifier.ExpectByte((byte) 0x01);  // Length of the address section
            verifier.ExpectByte((byte) '!');
            verifier.ExpectByte((byte) '!');
            verifier.ExpectByte((byte) '!');
            verifier.ExpectByte((byte) '\n');
            verifier.ExpectByte((byte) 0x1E);  // COPY size 14 mode VCD_SELF
            verifier.ExpectByte((byte) 0x05);  // ADD size 4
            verifier.ExpectByte((byte) 0x03);  // COPY address (3) mode VCD_SELF
        } else {
            // Larger block sizes will not catch any matches.
            verifier.ExpectSize(target_.length + 7);  // Delta encoding len
            verifier.ExpectSize(target_.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectSize(target_.length);  // Length of the data section
            verifier.ExpectByte((byte) 0x02);  // Length of the instructions section
            verifier.ExpectByte((byte) 0x00);  // Length of the address section
            // Data section
            verifier.ExpectString(kTarget);
            verifier.ExpectByte((byte) 0x01);  // ADD size 0
            verifier.ExpectSize(target_.length);
        }
        verifier.ExpectNoMoreBytes();
    }

    @Test
    public void VerifyOutputWithChecksum() throws Exception {
        StreamingEncode();

        Adler32 adler32 = new ZeroInitializedAdler32();
        adler32.update(target_);
        final int html2_checksum = (int) adler32.getValue();

        Assert.assertEquals(5, VarInt.calculateLongLength(html2_checksum));

        EncodedBytesVerifier verifier = new EncodedBytesVerifier(delta_.toByteArray());

        // These values do not depend on the block size used for encoding
        verifier.ExpectByte((byte) 0xD6);  // 'V' | 0x80
        verifier.ExpectByte((byte) 0xC3);  // 'C' | 0x80
        verifier.ExpectByte((byte) 0xC4);  // 'D' | 0x80
        verifier.ExpectByte((byte) 'S');  // Format extensions
        verifier.ExpectByte((byte) 0x00);  // Hdr_Indicator
        verifier.ExpectByte((byte) (VCD_SOURCE | VCD_CHECKSUM));  // Win_Indicator
        verifier.ExpectByte((byte) dictionary_.length);  // Dictionary length
        verifier.ExpectByte((byte) 0x00);  // Source segment position: start of dictionary
        //noinspection ConstantConditions
        if (BlockHash.kBlockSize <= 8) {
            verifier.ExpectByte((byte) 17);  // Length of the delta encoding
            verifier.ExpectSize(target_.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectByte((byte) 0x00);  // Length of the data section
            verifier.ExpectByte((byte) 0x07);  // Length of the instructions section
            verifier.ExpectByte((byte) 0x00);  // Length of the address section
            verifier.ExpectChecksum(html2_checksum);
            verifier.ExpectByte((byte) 0x1E);  // COPY size 14 mode VCD_SELF
            verifier.ExpectByte((byte) 0x03);  // COPY address (3) mode VCD_SELF
            verifier.ExpectByte((byte) 0x05);  // ADD size 4
            verifier.ExpectByte((byte) '!');
            verifier.ExpectByte((byte) '!');
            verifier.ExpectByte((byte) '!');
            verifier.ExpectByte((byte) '\n');
        } else {
            // Larger block sizes will not catch any matches.
            verifier.ExpectSize(target_.length + 12);  // Delta encoding len
            verifier.ExpectSize(target_.length);  // Size of the target window
            verifier.ExpectByte((byte) 0x00);  // Delta_indicator (no compression)
            verifier.ExpectByte((byte) 0x00);  // Length of the data section
            verifier.ExpectSize(0x02 + target_.length);  // Interleaved
            verifier.ExpectByte((byte) 0x00);  // Length of the address section
            verifier.ExpectChecksum(html2_checksum);
            // Data section
            verifier.ExpectByte((byte) 0x01);  // ADD size 0
            verifier.ExpectSize(target_.length);
            verifier.ExpectString(kTarget);
        }
        verifier.ExpectNoMoreBytes();
    }
}
