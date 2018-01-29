package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.*;
import com.davidehrmann.vcdiff.util.VarInt;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

public abstract class VerifyEncodedBytesTest {

    protected static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final Charset UTF16BE = Charset.forName("UTF-16BE");
    protected static final int kFileHeaderSize = DeltaFileHeader.SERIALIZED_SIZE;

    // This is to check the maximum possible encoding size
    // if using a single ADD instruction, so assume that the
    // dictionary size, the length of the ADD data, the size
    // of the target window, and the length of the delta window
    // are all two-byte Varints, that is, 128 <= length < 4096.
    // This figure includes three extra bytes for a zero-sized
    // ADD instruction with a two-byte Varint explicit size.
    // Any additional COPY & ADD instructions must reduce
    // the length of the encoding from this maximum.
    protected static final int kWindowHeaderSize = 21;

    protected final ByteArrayOutputStream delta_ = new ByteArrayOutputStream();

    protected final byte[] dictionary_;
    protected final byte[] target_;

    protected VCDiffStreamingEncoder<OutputStream> encoder_;
    protected VCDiffStreamingDecoder decoder_ = VCDiffDecoderBuilder.builder().buildStreaming();
    protected VCDiffEncoder<OutputStream> simple_encoder_;
    protected VCDiffDecoder simple_decoder_ = VCDiffDecoderBuilder.builder().buildSimple();

    ByteArrayOutputStream result_target_ = new ByteArrayOutputStream();

    public VerifyEncodedBytesTest(byte[] dictionary, byte[] target) {
        dictionary_ = dictionary;
        target_ = target;

        encoder_ = VCDiffEncoderBuilder.builder()
                .withDictionary(dictionary_)
                .withInterleaving(true)
                .withChecksum(true)
                .withTargetMatches(true)
                .buildStreaming();

        simple_encoder_ = VCDiffEncoderBuilder.builder()
                .withDictionary(dictionary_)
                .buildSimple();
    }

    protected void SimpleEncode() throws IOException {
        simple_encoder_.encode(target_, 0, target_.length, delta_);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        simple_decoder_.decode(
                dictionary_,
                delta_.toByteArray(),
                result_target_);
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    protected void StreamingEncode() throws  IOException {
        encoder_.startEncoding(delta_);
        encoder_.encodeChunk(target_, 0, target_.length, delta_);
        encoder_.finishEncoding(delta_);
    }


    protected static class EncodedBytesVerifier {
        private final byte[] delta_;
        private int delta_index_ = 0;

        public EncodedBytesVerifier(byte[] delta) {
            this.delta_ = delta;
        }

        protected void ExpectByte(byte b) {
            assertEquals(b, delta_[delta_index_]);
            ++delta_index_;
        }

        protected void ExpectString(byte[] s) {
            assertArrayEquals(s, Arrays.copyOfRange(delta_, delta_index_, delta_index_ + s.length));
            delta_index_ += s.length;
        }

        protected void ExpectNoMoreBytes() {
            assertEquals(delta_index_, delta_.length);
        }

        protected void ExpectSize(int size) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(delta_, delta_index_, delta_.length - delta_index_).slice();
            Assert.assertEquals(size, VarInt.getInt(buffer));
            delta_index_ += buffer.position();
        }

        protected void ExpectChecksum(int checksum) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(delta_, delta_index_, delta_.length - delta_index_).slice();
            assertEquals(checksum, VarInt.getLong(buffer));
            delta_index_ += buffer.position();
        }
    }
}
