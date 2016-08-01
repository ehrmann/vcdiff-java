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

package com.davidehrmann.vcdiff.codec;

import com.davidehrmann.vcdiff.VarInt;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class VCDiffHeaderParserTest {

    private static int kTestSize = 1024;

    private final Random random = new Random(1);

    private VCDiffHeaderParser parser;

    private void StartParsing(ByteBuffer buffer) {
        ByteBuffer backup = buffer.duplicate();
        parser = new VCDiffHeaderParser(buffer);
        assertEquals(backup, parser.unparsedData());
        assertNotSame(buffer, parser.unparsedData());
    }

    private void VerifyByte(byte expected_value) {
        ByteBuffer unparsedData = parser.unparsedData();
        byte decoded_byte = parser.ParseByte();
        assertEquals(expected_value, decoded_byte);
        assertEquals(unparsedData.remaining() - 1, parser.unparsedData().remaining());
    }

    private void VerifyInt32(int expected_value) {
        ByteBuffer prior_position = parser.unparsedData();
        int decoded_integer = parser.ParseInt32("decoded int32");
        assertEquals(expected_value, decoded_integer);
        Assert.assertEquals(prior_position.remaining(), parser.unparsedData().remaining() + VarInt.calculateIntLength(decoded_integer));
    }

    private void VerifyUInt32(int expected_value) {
        ByteBuffer prior_position = parser.unparsedData();
        int decoded_integer = parser.ParseUInt32("decoded uint32");
        assertEquals(expected_value, decoded_integer);
        assertEquals(prior_position.remaining(), parser.unparsedData().remaining() + VarInt.calculateLongLength(decoded_integer & 0xffffffffL));

    }

    private void VerifyChecksum(int expected_value) {
        ByteBuffer prior_position = parser.unparsedData();
        int decoded_checksum = parser.ParseChecksum("decoded checksum");
        assertEquals(expected_value, decoded_checksum);
        assertEquals(prior_position.remaining(), parser.unparsedData().remaining() + VarInt.calculateLongLength(decoded_checksum & 0xffffffffL));
    }

    @Test
    public void ParseRandomBytes() {
        ByteBuffer encoded_buffer_ = ByteBuffer.allocate(kTestSize * 8);
        List<Byte> byte_values = new ArrayList<Byte>();
        for (int i = 0; i < kTestSize; ++i) {
            byte random_byte = (byte)random.nextInt();
            encoded_buffer_.put(random_byte);
            byte_values.add(random_byte);
        }

        encoded_buffer_.flip();
        StartParsing(encoded_buffer_);

        for (int position = 0; position < kTestSize; ++position) {
            VerifyByte(byte_values.get(position));
        }

        assertNull(parser.ParseByte());
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.GetResult());

        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseRandomInt32() {
        ByteBuffer encoded_buffer_ = ByteBuffer.allocate(kTestSize * 8);
        List<Integer> integer_values = new ArrayList<Integer>();
        for (int i = 0; i < kTestSize; ++i) {
            int random_integer = random.nextInt() & Integer.MAX_VALUE;
            VarInt.putInt(encoded_buffer_, random_integer);
            integer_values.add(random_integer);
        }

        encoded_buffer_.flip();
        StartParsing(encoded_buffer_);

        for (int i = 0; i < kTestSize; ++i) {
            VerifyInt32(integer_values.get(i));
        }

        assertNull(parser.ParseInt32("decoded integer"));
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.GetResult());
        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseRandomUInt32() {
        ByteBuffer buffer = ByteBuffer.allocate(kTestSize * 8);
        List<Integer> integer_values = new ArrayList<Integer>();
        for (int i = 0; i < kTestSize; ++i) {
            int random_integer = random.nextInt();
            VarInt.putLong(buffer, random_integer & 0xffffffffL);
            integer_values.add(random_integer);
        }

        buffer.flip();
        StartParsing(buffer);

        for (int i = 0; i < kTestSize; ++i) {
            VerifyUInt32(integer_values.get(i));
        }

        assertNull(parser.ParseUInt32("decoded integer"));
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.GetResult());
        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseRandomChecksum() {
        ByteBuffer buffer = ByteBuffer.allocate(kTestSize * 8);
        List<Integer> checksum_values = new ArrayList<Integer>();

        for (int i = 0; i < kTestSize; ++i) {
            int random_checksum = random.nextInt();
            VarInt.putLong(buffer, random_checksum & 0xffffffffL);
            checksum_values.add(random_checksum);
        }

        buffer.flip();
        StartParsing(buffer);

        for (int i = 0; i < kTestSize; ++i) {
            VerifyChecksum(checksum_values.get(i));
        }

        assertNull(parser.ParseChecksum("decoded checksum"));
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.GetResult());
        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseMixed() {
        ByteBuffer buffer = ByteBuffer.allocate(64);

        VarInt.putLong(buffer, 0xCAFECAFE & 0xFFFFFFFFL);
        buffer.put((byte) 0xff);
        VarInt.putInt(buffer, 0x02020202);
        VarInt.putLong(buffer, 0xCAFECAFE & 0xFFFFFFFFL);
        buffer.put((byte) 0xff);
        buffer.put((byte) 0xff);

        buffer.flip();
        StartParsing(buffer);

        VerifyUInt32(0xCAFECAFE);
        VerifyByte((byte) 0xFF);
        VerifyInt32(0x02020202);
        VerifyChecksum(0xCAFECAFE);

        parser.ParseInt32("incomplete Varint");
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.GetResult());
        assertEquals(2, parser.unparsedData().remaining());
    }

    @Test
    public void ParseInvalidVarint() {
        ByteBuffer buffer = ByteBuffer.allocate(64);

        // Start with a byte that has the continuation bit plus a high-order bit set
        buffer.put((byte) 0xC0);
        // Add too many bytes with continuation bits
        for (int i = 0; i < 6; i++) {
            buffer.put((byte) 0x80);
        }

        buffer.flip();
        ByteBuffer backup = buffer.duplicate();
        StartParsing(buffer);

        assertNull(parser.ParseInt32("invalid Varint"));

        assertEquals(backup, parser.unparsedData());

        // After the parse failure, any other call to Parse... should return an error,
        // even though there is still a byte that could be read as valid.
        assertNull(parser.ParseByte());

        assertEquals(backup, parser.unparsedData());
    }

}
