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


package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.VCDiffEncoder;
import com.davidehrmann.vcdiff.VCDiffEncoderBuilder;
import com.davidehrmann.vcdiff.VCDiffStreamingDecoder;
import com.davidehrmann.vcdiff.VCDiffStreamingEncoder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VCDiffEncoderTest extends VerifyEncodedBytesTest {
    protected static final byte[] kDictionary = (
            "\"Just the place for a Snark!\" the Bellman cried,\n" +
                    "As he landed his crew with care;\n" +
                    "Supporting each man on the top of the tide\n" +
                    "By a finger entwined in his hair.\n").getBytes(US_ASCII);

    protected static final byte[] kTarget = (
            "\"Just the place for a Snark! I have said it twice:\n" +
                    "That alone should encourage the crew.\n" +
                    "Just the place for a Snark! I have said it thrice:\n" +
                    "What I tell you three times is true.\"\n").getBytes(US_ASCII);

    // NonASCII string "foo\x128".
    protected static final byte[] kNonAscii = {102, 111, 111, (byte) 128, 0};

    public VCDiffEncoderTest() {
        super(kDictionary, kTarget);
    }

    // Test the encoding and decoding with a fixed chunk size.
    // If decoder is null, only test the encoding.
    protected void TestWithFixedChunkSize(VCDiffStreamingEncoder<OutputStream> encoder,
                                          VCDiffStreamingDecoder decoder,
                                          int chunk_size) throws IOException {
        delta_.reset();
        encoder.startEncoding(delta_);
        for (int chunk_start_index = 0;
             chunk_start_index < target_.length;
             chunk_start_index += chunk_size) {
            int this_chunk_size = chunk_size;
            final int bytes_available = target_.length - chunk_start_index;
            if (this_chunk_size > bytes_available) {
                this_chunk_size = bytes_available;
            }
            encoder.encodeChunk(
                    target_,
                    chunk_start_index,
                    this_chunk_size,
                    delta_
            );
        }
        encoder.finishEncoding(delta_);
        final int num_windows = (target_.length / chunk_size) + 1;
        final int size_of_windows =
                target_.length + (kWindowHeaderSize * num_windows);
        assertTrue(kFileHeaderSize + size_of_windows >= delta_.size());
        result_target_.reset();

        if (decoder == null) {
            return;
        }

        decoder.startDecoding(dictionary_);
        byte[] delta = delta_.toByteArray();
        for (int chunk_start_index = 0;
             chunk_start_index < delta.length;
             chunk_start_index += chunk_size) {
            int this_chunk_size = chunk_size;
            final int bytes_available = delta.length - chunk_start_index;
            if (this_chunk_size > bytes_available) {
                this_chunk_size = bytes_available;
            }
            decoder.decodeChunk(delta, chunk_start_index, this_chunk_size, result_target_);
        }
        decoder.finishDecoding();
        assertArrayEquals(target_, result_target_.toByteArray());

    }

    @Test(expected = IllegalStateException.class)
    public void EncodeBeforeStartEncoding() throws Exception {
        encoder_.encodeChunk(target_, 0, target_.length, delta_);
    }

    @Test(expected = IllegalStateException.class)
    public void FinishBeforeStartEncoding() throws Exception {
        encoder_.finishEncoding(delta_);
    }

    @Test
    public void EncodeDecodeNothing() throws Exception {
        VCDiffStreamingEncoder<OutputStream> nothing_encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(new byte[0])
                .withTargetMatches(false)
                .buildStreaming();

        nothing_encoder.startEncoding(delta_);
        nothing_encoder.finishEncoding(delta_);
        decoder_.startDecoding(new byte[0]);
        decoder_.decodeChunk(delta_.toByteArray(), result_target_);
        decoder_.finishDecoding();
        assertArrayEquals(new byte[0], result_target_.toByteArray());
    }

    // A NULL dictionary pointer is legal as long as the dictionary size is 0.
    // public void EncodeDecodeNullDictionaryPtr();

    @Test
    public void EncodeDecodeSimple() throws Exception {
        simple_encoder_.encode(target_, 0, target_.length, delta_);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        simple_decoder_.decode(dictionary_, delta_.toByteArray(), result_target_);
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeInterleaved() throws Exception {
        VCDiffEncoder<OutputStream> encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(dictionary_)
                .withInterleaving(true)
                .withTargetMatches(true)
                .buildSimple();

        encoder.encode(target_, 0, target_.length, delta_);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        simple_decoder_.decode(dictionary_, delta_.toByteArray(), result_target_);
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeInterleavedChecksum() throws Exception {
        VCDiffEncoder<OutputStream> encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(dictionary_)
                .withChecksum(true)
                .withTargetMatches(true)
                .withInterleaving(true)
                .buildSimple();

        encoder.encode(target_, delta_);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        simple_decoder_.decode(dictionary_, delta_.toByteArray(), result_target_);
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeSingleChunk() throws Exception {
        encoder_.startEncoding(delta_);
        encoder_.encodeChunk(target_, delta_);
        encoder_.finishEncoding(delta_);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_.toByteArray(), result_target_);
        decoder_.finishDecoding();
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeSeparate() throws Exception {
        ByteArrayOutputStream delta_start = new ByteArrayOutputStream();
        ByteArrayOutputStream delta_encode = new ByteArrayOutputStream();
        ByteArrayOutputStream delta_finish = new ByteArrayOutputStream();

        encoder_.startEncoding(delta_start);
        encoder_.encodeChunk(target_, 0, target_.length, delta_encode);
        encoder_.finishEncoding(delta_finish);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >=
                delta_start.size() + delta_encode.size() + delta_finish.size());
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(delta_start.toByteArray(), result_target_);
        decoder_.decodeChunk(delta_encode.toByteArray(), result_target_);
        decoder_.decodeChunk(delta_finish.toByteArray(), result_target_);
        decoder_.finishDecoding();
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeFixedChunkSizes() throws Exception {
        // These specific chunk sizes have failed in the past
        TestWithFixedChunkSize(encoder_, decoder_, 6);
        TestWithFixedChunkSize(encoder_, decoder_, 45);
        TestWithFixedChunkSize(encoder_, decoder_, 60);

        // Now loop through all possible chunk sizes
        for (int chunk_size = 1; chunk_size < target_.length; ++chunk_size) {
            TestWithFixedChunkSize(encoder_, decoder_, chunk_size);
        }
    }

    // If --allowVcdTarget=false is specified, the decoder will throw away some of
    // the internally-stored decoded target beyond the current window.  Try
    // different numbers of encoded window sizes to make sure that this behavior
    // does not affect the results.
    @Test
    public void EncodeDecodeFixedChunkSizesNoVcdTarget() throws Exception {
        decoder_.setAllowVcdTarget(false);
        // Loop through all possible chunk sizes
        for (int chunk_size = 1; chunk_size < target_.length; ++chunk_size) {
            TestWithFixedChunkSize(encoder_, decoder_, chunk_size);
        }
    }

    // Splits the text to be encoded into fixed-size chunks.  Encodes each
    // chunk and puts it into a vector of strings.  Then decodes each string
    // in the vector and appends the result into result_target_.
    private void TestWithEncodedChunkVector(int chunk_size) throws IOException {
        List<byte[]> encoded_chunks = new ArrayList<byte[]>();
        ByteArrayOutputStream this_encoded_chunk = new ByteArrayOutputStream();
        int total_chunk_size = 0;
        encoder_.startEncoding(this_encoded_chunk);
        encoded_chunks.add(this_encoded_chunk.toByteArray());
        total_chunk_size += this_encoded_chunk.size();
        for (int chunk_start_index = 0;
             chunk_start_index < target_.length;
             chunk_start_index += chunk_size) {
            int this_chunk_size = chunk_size;
            final int bytes_available = target_.length - chunk_start_index;
            if (this_chunk_size > bytes_available) {
                this_chunk_size = bytes_available;
            }
            this_encoded_chunk.reset();
            encoder_.encodeChunk(
                    target_,
                    chunk_start_index,
                    this_chunk_size,
                    this_encoded_chunk
            );
            encoded_chunks.add(this_encoded_chunk.toByteArray());
            total_chunk_size += this_encoded_chunk.size();
        }
        this_encoded_chunk.reset();
        encoder_.finishEncoding(this_encoded_chunk);
        encoded_chunks.add(this_encoded_chunk.toByteArray());
        total_chunk_size += this_encoded_chunk.size();
        final int num_windows = (target_.length / chunk_size) + 1;
        final int size_of_windows =
                target_.length + (kWindowHeaderSize * num_windows);
        assertTrue(kFileHeaderSize + size_of_windows >= total_chunk_size);
        result_target_.reset();
        decoder_.startDecoding(dictionary_);
        for (byte[] encoded_chunk : encoded_chunks) {
            decoder_.decodeChunk(encoded_chunk, result_target_);
        }
        decoder_.finishDecoding();
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeStreamOfChunks() throws Exception {
        // Loop through all possible chunk sizes
        for (int chunk_size = 1; chunk_size < target_.length; ++chunk_size) {
            TestWithEncodedChunkVector(chunk_size);
        }
    }

    // Verify that HashedDictionary stores a copy of the dictionary text,
    // rather than just storing a pointer to it.  If the dictionary buffer
    // is overwritten after creating a HashedDictionary from it, it shouldn't
    // affect an encoder that uses that HashedDictionary.
    @Test
    public void DictionaryBufferOverwritten() throws Exception {
        byte[] dictionary_copy = dictionary_.clone();
        VCDiffStreamingEncoder<OutputStream> copy_encoder = VCDiffEncoderBuilder.builder()
                .withInterleaving(true)
                .withDictionary(dictionary_copy)
                .withChecksum(true)
                .withTargetMatches(true)
                .buildStreaming();

        // Produce a reference version of the encoded text.
        ByteArrayOutputStream delta_before = new ByteArrayOutputStream();
        copy_encoder.startEncoding(delta_before);
        copy_encoder.encodeChunk(target_, delta_before);
        copy_encoder.finishEncoding(delta_before);
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_before.size());

        // Overwrite the dictionary text with all 'Q' characters.
        Arrays.fill(dictionary_copy, (byte) 'Q');

        // When the encoder is used on the same target text after overwriting
        // the dictionary, it should produce the same encoded output.
        ByteArrayOutputStream delta_after = new ByteArrayOutputStream();
        copy_encoder.startEncoding(delta_after);
        copy_encoder.encodeChunk(target_, delta_after);
        copy_encoder.finishEncoding(delta_after);
        assertArrayEquals(delta_before.toByteArray(), delta_after.toByteArray());
    }

    // Binary data test part 1: The dictionary and target data should not
    // be treated as NULL-terminated.  An embedded NULL should be handled like
    // any other byte of data.
    @Test
    public void DictionaryHasEmbeddedNULLs() throws Exception {
        final byte[] embedded_null_dictionary_text =
                { 0x00, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD, 0x00, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF, 0x00, 0x03 };
        final byte[] embedded_null_target =
                { (byte) 0xFD, 0x00, (byte) 0xFD, (byte) 0xFE, 0x03, 0x00, 0x01, 0x00 };
        assertEquals(10, embedded_null_dictionary_text.length);
        assertEquals(8, embedded_null_target.length);

        VCDiffStreamingEncoder<OutputStream> embedded_null_encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(embedded_null_dictionary_text)
                .withInterleaving(true)
                .withTargetMatches(true)
                .withChecksum(true)
                .buildStreaming();

        embedded_null_encoder.startEncoding(delta_);
        embedded_null_encoder.encodeChunk(embedded_null_target, delta_);
        embedded_null_encoder.finishEncoding(delta_);
        decoder_.startDecoding(embedded_null_dictionary_text);
        decoder_.decodeChunk(delta_.toByteArray(), result_target_);
        decoder_.finishDecoding();
        assertArrayEquals(embedded_null_target, result_target_.toByteArray());
    }

    // Binary data test part 2: An embedded CR or LF should be handled like
    // any other byte of data.  No text-processing of the data should occur.
    @Test
    public void DictionaryHasEmbeddedNewlines() throws Exception {
        final byte[] embedded_null_dictionary_text =
                { 0x0C, (byte) 0xFF, (byte) 0xFE, 0x0C, 0x00, 0x0A, (byte) 0xFE, (byte) 0xFF, 0x00, 0x0A };
        final byte[] embedded_null_target =
                { 0x0C, 0x00, 0x0A, (byte) 0xFE, 0x03, 0x00, 0x0A, 0x00 };
        assertEquals(10, embedded_null_dictionary_text.length);
        assertEquals(8, embedded_null_target.length);


        // FIXME: ok to remove?
        //interleavedCodeTableWriter.init(embedded_null_dictionary_text.length);
        VCDiffStreamingEncoder<OutputStream> embedded_null_encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(embedded_null_dictionary_text)
                .withInterleaving(true)
                .withChecksum(true)
                .withTargetMatches(true)
                .buildStreaming();

        embedded_null_encoder.startEncoding(delta_);
        embedded_null_encoder.encodeChunk(embedded_null_target, delta_);
        embedded_null_encoder.finishEncoding(delta_);
        decoder_.startDecoding(embedded_null_dictionary_text);
        decoder_.decodeChunk(delta_.toByteArray(), result_target_);
        decoder_.finishDecoding();
        assertArrayEquals(embedded_null_target, result_target_.toByteArray());
    }

    @Test
    public void UsingWideCharacters() throws Exception {
        final byte[] wchar_dictionary_text = (
                "\"Just the place for a Snark!\" the Bellman cried,\n"
                        + "As he landed his crew with care;\n"
                        + "Supporting each man on the top of the tide\n"
                        + "By a finger entwined in his hair.\n").getBytes(UTF_16BE);

        final byte[] wchar_target = (
                "\"Just the place for a Snark! I have said it twice:\n"
                        + "That alone should encourage the crew.\n"
                        + "Just the place for a Snark! I have said it thrice:\n"
                        + "What I tell you three times is true.\"\n").getBytes(UTF_16BE);

        VCDiffStreamingEncoder<OutputStream> wchar_encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(wchar_dictionary_text)
                .withTargetMatches(false)
                .withChecksum(true)
                .withInterleaving(true)
                .buildStreaming();

        wchar_encoder.startEncoding(delta_);
        wchar_encoder.encodeChunk(wchar_target, delta_);
        wchar_encoder.finishEncoding(delta_);
        decoder_.startDecoding(wchar_dictionary_text);
        decoder_.decodeChunk(delta_.toByteArray(), result_target_);
        decoder_.finishDecoding();
        assertEquals(new String(wchar_target, UTF_16BE), new String(result_target_.toByteArray(), UTF_16BE));
    }

    @Test
    public void NonasciiDictionary() throws Exception {
        VCDiffEncoder<OutputStream> encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(kNonAscii)
                .withTargetMatches(true)
                .buildSimple();
        encoder.encode(kTarget, 0, kTarget.length, delta_);
    }

    @Test
    public void NonasciiTarget() throws Exception {
        simple_encoder_.encode(kNonAscii, 0, kNonAscii.length,delta_);
    }

    // TODO: This can be ported once ByteBuffers are used everywhere
    /*
    #if defined(HAVE_MPROTECT) && \
            (defined(HAVE_MEMALIGN) || defined(HAVE_POSIX_MEMALIGN))
    // Bug 1220602: Make sure the encoder doesn't read past the end of the input
    // buffer.
    @Test
    public void ShouldNotReadPastEndOfBuffer() throws Exception {
        final int target_size = target_.length;

        // Allocate two memory pages.
        final int page_size = getpagesize();
        void* two_pages = NULL;
        #ifdef HAVE_POSIX_MEMALIGN
        posix_memalign(&two_pages, page_size, 2 * page_size);
        #else  // !HAVE_POSIX_MEMALIGN
        two_pages = memalign(page_size, 2 * page_size);
        #endif  // HAVE_POSIX_MEMALIGN
        char* const first_page = reinterpret_cast<char*>(two_pages);
        char* const second_page = first_page + page_size;

        // Place the target string at the end of the first page.
        char* const target_with_guard = second_page - target_size;
        memcpy(target_with_guard, target_, target_size);

        // Make the second page unreadable.
        mprotect(second_page, page_size, PROT_NONE);

        // Now perform the encode operation, which will cause a segmentation fault
        // if it reads past the end of the buffer.
        assertTrue(encoder.startEncoding(delta_));
        assertTrue(encoder.encodeChunk(target_with_guard, target_size, delta_));
        assertTrue(encoder.finishEncoding(delta_));

        // Undo the mprotect.
        mprotect(second_page, page_size, PROT_READ|PROT_WRITE);
        free(two_pages);
    }

    @Test
    public void ShouldNotReadPastBeginningOfBuffer() throws Exception {
        final int target_size = target_.length;

        // Allocate two memory pages.
        final int page_size = getpagesize();
        void* two_pages = NULL;
        #ifdef HAVE_POSIX_MEMALIGN
        posix_memalign(&two_pages, page_size, 2 * page_size);
        #else  // !HAVE_POSIX_MEMALIGN
        two_pages = memalign(page_size, 2 * page_size);
        #endif  // HAVE_POSIX_MEMALIGN
        char* const first_page = reinterpret_cast<char*>(two_pages);
        char* const second_page = first_page + page_size;

        // Make the first page unreadable.
        mprotect(first_page, page_size, PROT_NONE);

        // Place the target string at the beginning of the second page.
        char* const target_with_guard = second_page;
        memcpy(target_with_guard, target_, target_size);

        // Now perform the encode operation, which will cause a segmentation fault
        // if it reads past the beginning of the buffer.
        assertTrue(encoder.startEncoding(delta_));
        assertTrue(encoder.encodeChunk(target_with_guard, target_size, delta_));
        assertTrue(encoder.finishEncoding(delta_));

        // Undo the mprotect.
        mprotect(first_page, page_size, PROT_READ|PROT_WRITE);
        free(two_pages);
    }
    #endif  // HAVE_MPROTECT && (HAVE_MEMALIGN || HAVE_POSIX_MEMALIGN)
    */
}
