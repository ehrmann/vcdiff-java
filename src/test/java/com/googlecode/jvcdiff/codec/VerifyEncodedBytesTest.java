package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.VCDiffCodeTableWriter;
import com.googlecode.jvcdiff.VarInt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;

import static com.googlecode.jvcdiff.google.VCDiffFormatExtensionFlag.VCD_FORMAT_CHECKSUM;
import static com.googlecode.jvcdiff.google.VCDiffFormatExtensionFlag.VCD_FORMAT_INTERLEAVED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    protected HashedDictionary hashed_dictionary_;
    protected final byte[] dictionary_;
    protected final byte[] target_;
    protected VCDiffCodeTableWriter interleavedCodeTableWriter = new VCDiffCodeTableWriter(true);
    protected VCDiffCodeTableWriter normalCodeTableWriter = new VCDiffCodeTableWriter(false);
    protected VCDiffStreamingEncoder<OutputStream> encoder_;
    protected VCDiffStreamingDecoder decoder_ = new VCDiffStreamingDecoderImpl();
    protected VCDiffEncoder<OutputStream> simple_encoder_;
    protected VCDiffDecoder simple_decoder_ = new VCDiffDecoder();

    ByteArrayOutputStream result_target_ = new ByteArrayOutputStream();

    public VerifyEncodedBytesTest(byte[] dictionary, byte[] target) {
        dictionary_ = dictionary;
        target_ = target;
        hashed_dictionary_ = new HashedDictionary(dictionary_);

        // FIXMR: ok?
        //interleavedCodeTableWriter.Init(dictionary_.length);
        //normalCodeTableWriter.Init(dictionary_.length);

        encoder_ = new BaseVCDiffStreamingEncoder<OutputStream>(
                interleavedCodeTableWriter,
                hashed_dictionary_,
                EnumSet.of(VCD_FORMAT_INTERLEAVED, VCD_FORMAT_CHECKSUM),
                /* look_for_target_matches = */ true
        );

        simple_encoder_ = new VCDiffEncoder<OutputStream>(normalCodeTableWriter, dictionary_);
    }


    protected void SimpleEncode() throws IOException {
        assertTrue(simple_encoder_.Encode(target_, 0, target_.length, delta_));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        assertTrue(simple_decoder_.Decode(dictionary_,
                delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    protected void StreamingEncode() throws  IOException {
        assertTrue(encoder_.StartEncoding(delta_));
        assertTrue(encoder_.EncodeChunk(target_, 0, target_.length, delta_));
        assertTrue(encoder_.FinishEncoding(delta_));
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
            assertEquals(size, VarInt.getInt(buffer));
            delta_index_ += buffer.position();
        }

        protected void ExpectChecksum(int checksum) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(delta_, delta_index_, delta_.length - delta_index_).slice();
            assertEquals(checksum, VarInt.getLong(buffer));
            delta_index_ += buffer.position();
        }
    }
}
