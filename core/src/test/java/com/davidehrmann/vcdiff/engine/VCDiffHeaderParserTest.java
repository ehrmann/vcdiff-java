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

package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.util.VarInt;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
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

    private void VerifyByte(byte expected_value) throws IOException {
        ByteBuffer unparsedData = parser.unparsedData();
        byte decoded_byte = parser.parseByte();
        assertEquals(expected_value, decoded_byte);
        assertEquals(unparsedData.remaining() - 1, parser.unparsedData().remaining());
    }

    private void VerifyInt32(int expected_value) throws IOException {
        ByteBuffer prior_position = parser.unparsedData();
        int decoded_integer = parser.parseInt32("decoded int32");
        assertEquals(expected_value, decoded_integer);
        Assert.assertEquals(prior_position.remaining(), parser.unparsedData().remaining() + VarInt.calculateIntLength(decoded_integer));
    }

    private void VerifyUInt32(int expected_value) throws IOException {
        ByteBuffer prior_position = parser.unparsedData();
        int decoded_integer = parser.parseUInt32("decoded uint32");
        assertEquals(expected_value, decoded_integer);
        assertEquals(prior_position.remaining(), parser.unparsedData().remaining() + VarInt.calculateLongLength(decoded_integer & 0xffffffffL));

    }

    private void VerifyChecksum(int expected_value) throws IOException {
        ByteBuffer prior_position = parser.unparsedData();
        int decoded_checksum = parser.parseChecksum("decoded checksum");
        assertEquals(expected_value, decoded_checksum);
        assertEquals(prior_position.remaining(), parser.unparsedData().remaining() + VarInt.calculateLongLength(decoded_checksum & 0xffffffffL));
    }

    @Test
    public void ParseRandomBytes() throws IOException {
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

        assertNull(parser.parseByte());
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.getResult());

        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseRandomInt32() throws Exception {
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

        assertNull(parser.parseInt32("decoded integer"));
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.getResult());
        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseRandomUInt32() throws Exception {
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

        assertNull(parser.parseUInt32("decoded integer"));
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.getResult());
        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseRandomChecksum() throws Exception {
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

        assertNull(parser.parseChecksum("decoded checksum"));
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.getResult());
        assertEquals(0, parser.unparsedData().remaining());
    }

    @Test
    public void ParseMixed() throws Exception {
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

        parser.parseInt32("incomplete Varint");
        assertEquals(VCDiffHeaderParser.RESULT_END_OF_DATA, parser.getResult());
        assertEquals(2, parser.unparsedData().remaining());
    }

    @Test
    public void ParseInvalidVarint() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(64);

        // Start with a byte that has the continuation bit plus a high-order bit set
        buffer.put((byte) 0xC0);
        // add too many bytes with continuation bits
        for (int i = 0; i < 6; i++) {
            buffer.put((byte) 0x80);
        }

        buffer.flip();
        ByteBuffer backup = buffer.duplicate();
        StartParsing(buffer);

        try {
            parser.parseInt32("invalid Varint");
            fail();
        } catch (IOException ignored) { }

        assertEquals(backup, parser.unparsedData());

        // After the parse failure, any other call to Parse... should return an error,
        // even though there is still a byte that could be read as valid.
        try {
            parser.parseByte();
            fail();
        } catch (IOException ignored) { }

        assertEquals(backup, parser.unparsedData());
    }

}
