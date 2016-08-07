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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static com.davidehrmann.vcdiff.google.VCDiffFormatExtensionFlag.*;
import static org.junit.Assert.*;

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
        assertTrue(encoder.StartEncoding(delta_));
        for (int chunk_start_index = 0;
             chunk_start_index < target_.length;
             chunk_start_index += chunk_size) {
            int this_chunk_size = chunk_size;
            final int bytes_available = target_.length - chunk_start_index;
            if (this_chunk_size > bytes_available) {
                this_chunk_size = bytes_available;
            }
            assertTrue(encoder.EncodeChunk(target_,
                    chunk_start_index,
                    this_chunk_size,
                    delta_));
        }
        assertTrue(encoder.FinishEncoding(delta_));
        final int num_windows = (target_.length / chunk_size) + 1;
        final int size_of_windows =
                target_.length + (kWindowHeaderSize * num_windows);
        assertTrue(kFileHeaderSize + size_of_windows >= delta_.size());
        result_target_.reset();

        if (decoder == null) {
            return;
        }

        decoder.StartDecoding(dictionary_);
        byte[] delta = delta_.toByteArray();
        for (int chunk_start_index = 0;
             chunk_start_index < delta.length;
             chunk_start_index += chunk_size) {
            int this_chunk_size = chunk_size;
            final int bytes_available = delta.length - chunk_start_index;
            if (this_chunk_size > bytes_available) {
                this_chunk_size = bytes_available;
            }
            assertTrue(decoder.DecodeChunk(delta, chunk_start_index,
                    this_chunk_size,
                    result_target_));
        }
        assertTrue(decoder.FinishDecoding());
        assertArrayEquals(target_, result_target_.toByteArray());

    }

    @Test
    public void EncodeBeforeStartEncoding() throws Exception {
        assertFalse(encoder_.EncodeChunk(target_, 0, target_.length, delta_));
    }

    @Test
    public void FinishBeforeStartEncoding() throws Exception {
        assertFalse(encoder_.FinishEncoding(delta_));
    }

    @Test
    public void EncodeDecodeNothing() throws Exception {
        HashedDictionary nothing_dictionary = new HashedDictionary(new byte[0]);
        VCDiffStreamingEncoder<OutputStream> nothing_encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                normalCodeTableWriter,
                nothing_dictionary,
                EnumSet.of(VCD_STANDARD_FORMAT),
                false);
        assertTrue(nothing_encoder.StartEncoding(delta_));
        assertTrue(nothing_encoder.FinishEncoding(delta_));
        decoder_.StartDecoding(new byte[0]);
        assertTrue(decoder_.DecodeChunk(delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(new byte[0], result_target_.toByteArray());
    }

    // A NULL dictionary pointer is legal as long as the dictionary size is 0.
    // public void EncodeDecodeNullDictionaryPtr();

    @Test
    public void EncodeDecodeSimple() throws Exception {
        assertTrue(simple_encoder_.Encode(target_, 0, target_.length, delta_));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        assertTrue(simple_decoder_.Decode(dictionary_,
                delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeInterleaved() throws Exception {
        VCDiffEncoder<OutputStream> encoder = new VCDiffEncoder<OutputStream>(interleavedCodeTableWriter, dictionary_, EnumSet.of(VCD_FORMAT_INTERLEAVED));
        assertTrue(encoder.Encode(target_, 0, target_.length, delta_));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        assertTrue(simple_decoder_.Decode(dictionary_,
                delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeInterleavedChecksum() throws Exception {
        VCDiffEncoder<OutputStream> encoder = new VCDiffEncoder<OutputStream>(
                interleavedCodeTableWriter,
                dictionary_,
                EnumSet.of(VCD_FORMAT_CHECKSUM)
        );

        assertTrue(encoder.Encode(target_,
                0,
                target_.length,
                delta_));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        assertTrue(simple_decoder_.Decode(dictionary_,
                delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeSingleChunk() throws Exception {
        assertTrue(encoder_.StartEncoding(delta_));
        assertTrue(encoder_.EncodeChunk(target_, 0, target_.length, delta_));
        assertTrue(encoder_.FinishEncoding(delta_));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_.size());
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(target_, result_target_.toByteArray());
    }

    @Test
    public void EncodeDecodeSeparate() throws Exception {
        ByteArrayOutputStream delta_start = new ByteArrayOutputStream();
        ByteArrayOutputStream delta_encode = new ByteArrayOutputStream();
        ByteArrayOutputStream delta_finish = new ByteArrayOutputStream();

        assertTrue(encoder_.StartEncoding(delta_start));
        assertTrue(encoder_.EncodeChunk(target_, 0, target_.length, delta_encode));
        assertTrue(encoder_.FinishEncoding(delta_finish));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >=
                delta_start.size() + delta_encode.size() + delta_finish.size());
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_start.toByteArray(),
                0,
                delta_start.size(),
                result_target_));
        assertTrue(decoder_.DecodeChunk(delta_encode.toByteArray(),
                0,
                delta_encode.size(),
                result_target_));
        assertTrue(decoder_.DecodeChunk(delta_finish.toByteArray(),
                0,
                delta_finish.size(),
                result_target_));
        assertTrue(decoder_.FinishDecoding());
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

    // If --allow_vcd_target=false is specified, the decoder will throw away some of
    // the internally-stored decoded target beyond the current window.  Try
    // different numbers of encoded window sizes to make sure that this behavior
    // does not affect the results.
    @Test
    public void EncodeDecodeFixedChunkSizesNoVcdTarget() throws Exception {
        decoder_.SetAllowVcdTarget(false);
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
        assertTrue(encoder_.StartEncoding(this_encoded_chunk));
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
            assertTrue(encoder_.EncodeChunk(target_,
                    chunk_start_index,
                    this_chunk_size,
                    this_encoded_chunk));
            encoded_chunks.add(this_encoded_chunk.toByteArray());
            total_chunk_size += this_encoded_chunk.size();
        }
        this_encoded_chunk.reset();
        assertTrue(encoder_.FinishEncoding(this_encoded_chunk));
        encoded_chunks.add(this_encoded_chunk.toByteArray());
        total_chunk_size += this_encoded_chunk.size();
        final int num_windows = (target_.length / chunk_size) + 1;
        final int size_of_windows =
                target_.length + (kWindowHeaderSize * num_windows);
        assertTrue(kFileHeaderSize + size_of_windows >= total_chunk_size);
        result_target_.reset();
        decoder_.StartDecoding(dictionary_);
        for (byte[] encoded_chunk : encoded_chunks) {
            assertTrue(decoder_.DecodeChunk(encoded_chunk, 0, encoded_chunk.length, result_target_));
        }
        assertTrue(decoder_.FinishDecoding());
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
        HashedDictionary hd_copy = new HashedDictionary(dictionary_copy);
        VCDiffStreamingEncoder<OutputStream> copy_encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                interleavedCodeTableWriter,
                hd_copy,
                EnumSet.of(VCD_FORMAT_INTERLEAVED, VCD_FORMAT_CHECKSUM),
                                      /* look_for_target_matches = */ true);
        // Produce a reference version of the encoded text.
        ByteArrayOutputStream delta_before = new ByteArrayOutputStream();
        assertTrue(copy_encoder.StartEncoding(delta_before));
        assertTrue(copy_encoder.EncodeChunk(target_,
                0,
                target_.length,
                delta_before));
        assertTrue(copy_encoder.FinishEncoding(delta_before));
        assertTrue(target_.length + kFileHeaderSize + kWindowHeaderSize >= delta_before.size());

        // Overwrite the dictionary text with all 'Q' characters.
        Arrays.fill(dictionary_copy, (byte) 'Q');

        // When the encoder is used on the same target text after overwriting
        // the dictionary, it should produce the same encoded output.
        ByteArrayOutputStream delta_after = new ByteArrayOutputStream();
        assertTrue(copy_encoder.StartEncoding(delta_after));
        assertTrue(copy_encoder.EncodeChunk(target_, 0, target_.length, delta_after));
        assertTrue(copy_encoder.FinishEncoding(delta_after));
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
        HashedDictionary embedded_null_dictionary = new HashedDictionary(embedded_null_dictionary_text);
        // FIXME: ok to remove?
        //interleavedCodeTableWriter.Init(embedded_null_dictionary_text.length);
        VCDiffStreamingEncoder<OutputStream> embedded_null_encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                interleavedCodeTableWriter,
                embedded_null_dictionary,
                EnumSet.of(VCD_FORMAT_INTERLEAVED, VCD_FORMAT_CHECKSUM),
                /* look_for_target_matches = */ true);
        assertTrue(embedded_null_encoder.StartEncoding(delta_));
        assertTrue(embedded_null_encoder.EncodeChunk(embedded_null_target,
                0,
                embedded_null_target.length,
                delta_));
        assertTrue(embedded_null_encoder.FinishEncoding(delta_));
        decoder_.StartDecoding(embedded_null_dictionary_text);
        assertTrue(decoder_.DecodeChunk(delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertTrue(decoder_.FinishDecoding());
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
        HashedDictionary embedded_null_dictionary = new HashedDictionary(embedded_null_dictionary_text);
        // FIXME: ok to remove?
        //interleavedCodeTableWriter.Init(embedded_null_dictionary_text.length);
        VCDiffStreamingEncoder<OutputStream> embedded_null_encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                interleavedCodeTableWriter,
                embedded_null_dictionary,
                EnumSet.of(VCD_FORMAT_INTERLEAVED, VCD_FORMAT_CHECKSUM),
                /* look_for_target_matches = */ true);
        assertTrue(embedded_null_encoder.StartEncoding(delta_));
        assertTrue(embedded_null_encoder.EncodeChunk(embedded_null_target,
                0,
                embedded_null_target.length,
                delta_));
        assertTrue(embedded_null_encoder.FinishEncoding(delta_));
        decoder_.StartDecoding(embedded_null_dictionary_text);
        assertTrue(decoder_.DecodeChunk(delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(embedded_null_target, result_target_.toByteArray());
    }

    @Test
    public void UsingWideCharacters() throws Exception {
        final byte[] wchar_dictionary_text = (
                "\"Just the place for a Snark!\" the Bellman cried,\n"
                        + "As he landed his crew with care;\n"
                        + "Supporting each man on the top of the tide\n"
                        + "By a finger entwined in his hair.\n").getBytes(UTF16BE);

        final byte[] wchar_target = (
                "\"Just the place for a Snark! I have said it twice:\n"
                        + "That alone should encourage the crew.\n"
                        + "Just the place for a Snark! I have said it thrice:\n"
                        + "What I tell you three times is true.\"\n").getBytes(UTF16BE);

        HashedDictionary wchar_dictionary = new HashedDictionary(wchar_dictionary_text);
        VCDiffStreamingEncoder<OutputStream> wchar_encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                interleavedCodeTableWriter,
                wchar_dictionary,
                EnumSet.of(VCD_FORMAT_INTERLEAVED, VCD_FORMAT_CHECKSUM),
                /* look_for_target_matches = */ false);
        assertTrue(wchar_encoder.StartEncoding(delta_));
        assertTrue(wchar_encoder.EncodeChunk(wchar_target,
                0,
                wchar_target.length,
                delta_));
        assertTrue(wchar_encoder.FinishEncoding(delta_));
        decoder_.StartDecoding(wchar_dictionary_text);
        assertTrue(decoder_.DecodeChunk(delta_.toByteArray(),
                0,
                delta_.size(),
                result_target_));
        assertTrue(decoder_.FinishDecoding());
        assertEquals(new String(wchar_target, UTF16BE), new String(result_target_.toByteArray(), UTF16BE));
    }

    @Test
    public void NonasciiDictionary() throws Exception {
        VCDiffEncoder<OutputStream> encoder = new VCDiffEncoder<OutputStream>(normalCodeTableWriter, kNonAscii);
        assertTrue(encoder.Encode(kTarget, 0, kTarget.length, delta_));
    }

    @Test
    public void NonasciiTarget() throws Exception {
        assertTrue(simple_encoder_.Encode(kNonAscii, 0, kNonAscii.length,delta_));
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
        assertTrue(encoder_.StartEncoding(delta_));
        assertTrue(encoder_.EncodeChunk(target_with_guard, target_size, delta_));
        assertTrue(encoder_.FinishEncoding(delta_));

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
        assertTrue(encoder_.StartEncoding(delta_));
        assertTrue(encoder_.EncodeChunk(target_with_guard, target_size, delta_));
        assertTrue(encoder_.FinishEncoding(delta_));

        // Undo the mprotect.
        mprotect(first_page, page_size, PROT_READ|PROT_WRITE);
        free(two_pages);
    }
    #endif  // HAVE_MPROTECT && (HAVE_MEMALIGN || HAVE_POSIX_MEMALIGN)
    */
}
