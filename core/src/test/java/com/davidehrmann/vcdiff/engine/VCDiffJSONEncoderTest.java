// Copyright 2008-2016 Google Inc., David Ehrmann
// Author: Lincoln Smith, Davin Ehrmann
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

import com.davidehrmann.vcdiff.VCDiffEncoderBuilder;
import com.davidehrmann.vcdiff.VCDiffStreamingEncoder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VCDiffJSONEncoderTest {

    protected static final Charset US_ASCII = Charset.forName("US-ASCII");
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

    protected static final byte[] kDictionary = (
            "\"Just the place for a Snark!\" the Bellman cried,\n" +
                    "As he landed his crew with care;\n" +
                    "Supporting each man on the top of the tide\n" +
                    "By a finger entwined in his hair.\n\0").getBytes(US_ASCII);

    protected static final byte[] kTarget = (
            "\"Just the place for a Snark! I have said it twice:\n" +
                    "That alone should encourage the crew.\n" +
                    "Just the place for a Snark! I have said it thrice:\n" +
                    "What I tell you three times is true.\"\n").getBytes(US_ASCII);

    protected static final String kJSONDiff =
            "[\"\\\"Just the place for a Snark! I have said it twice:\\n" +
                    "That alone should encourage the crew.\\n\"," +
                    "161,44," +
                    "\"hrice:\\nWhat I tell you three times is true.\\\"\\n\"]";

    // NonASCII string "foo\x128".
    protected static final byte[] kNonAscii = {102, 111, 111, (byte) 128, 0};

    protected final VCDiffStreamingEncoder<Appendable> json_encoder_ = VCDiffEncoderBuilder.builder()
            .withDictionary(kDictionary)
            .buildStreamingJson();

    protected final StringBuilder delta = new StringBuilder();
    protected final ByteArrayOutputStream result_target_ = new ByteArrayOutputStream();

    public VCDiffJSONEncoderTest() {

    }

    // Test the encoding with a fixed chunk size.
    protected void TestWithFixedChunkSize(VCDiffStreamingEncoder<Appendable> encoder,
                                          int chunk_size) throws Exception {
        delta.setLength(0);
        encoder.startEncoding(delta);
        for (int chunk_start_index = 0;
             chunk_start_index < kTarget.length;
             chunk_start_index += chunk_size) {
            int this_chunk_size = chunk_size;
            final int bytes_available = kTarget.length - chunk_start_index;
            if (this_chunk_size > bytes_available) {
                this_chunk_size = bytes_available;
            }
            encoder.encodeChunk(
                    kTarget,
                    chunk_start_index,
                    this_chunk_size,
                    delta
            );
        }
        encoder.finishEncoding(delta);
        final int num_windows = (kTarget.length / chunk_size) + 1;
        final int size_of_windows = kTarget.length + (kWindowHeaderSize * num_windows);
        assertTrue(kFileHeaderSize + size_of_windows >= delta.length());
        result_target_.reset();
    }

    @Test
    public void EncodeNothingJSON() throws Exception {
        VCDiffStreamingEncoder<Appendable> nothing_encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(new byte[0])
                .withTargetMatches(false)
                .buildStreamingJson();

        nothing_encoder.startEncoding(delta);
        nothing_encoder.finishEncoding(delta);
        assertEquals("", delta.toString());
    }

    @Test
    public void EncodeSimpleJSON() throws Exception {
        json_encoder_.startEncoding(delta);
        json_encoder_.encodeChunk(kTarget, delta);
        json_encoder_.finishEncoding(delta);
        assertEquals(kJSONDiff, delta.toString());
    }

    @Test
    public void EncodeFixedChunkSizesJSON() throws Exception {
        // There is no JSON decoder; these diffs are created by hand.
        TestWithFixedChunkSize(json_encoder_, 6);
        assertEquals("[\"\\\"Just \",\"the pl\",\"ace fo\",\"r a Sn\",\"ark! I\"," +
                "\" have \",\"said i\",\"t twic\",\"e:\\nTha\",\"t alon\"," +
                "\"e shou\",\"ld enc\",\"ourage\",\" the c\",\"rew.\\nJ\"," +
                "\"ust th\",\"e plac\",\"e for \",\"a Snar\",\"k! I h\"," +
                "\"ave sa\",\"id it \",\"thrice\",\":\\nWhat\",\" I tel\"," +
                "\"l you \",\"three \",\"times \",\"is tru\",\"e.\\\"\\n\"]",
                delta.toString());
        TestWithFixedChunkSize(json_encoder_, 45);
        assertEquals("[\"\\\"Just the place for a Snark! I have said it t\"," +
                "\"wice:\\nThat alone should encourage the crew.\\nJ\"," +
                "\"ust the place for a Snark! I have said it thr\",\"ice:\\n" +
                "What I tell you three times is true.\\\"\\n\"]",
                delta.toString());
        TestWithFixedChunkSize(json_encoder_, 60);
        assertEquals("[\"\\\"Just the place for a Snark! I have said it twice:\\n" +
                "That alon\",\"e should encourage the crew.\\n" +
                "Just the place for a Snark! I h\",\"ave said it thrice:\\n" +
                "What I tell you three times is true.\\\"\\n\"]",
                delta.toString());
    }

    // TODO: disallow non-ASCII in VCDiffJSONEncoder
    @Ignore
    @Test
    public void NonasciiDictionaryWithJSON() throws Exception {
        VCDiffStreamingEncoder<Appendable> json_encoder = VCDiffEncoderBuilder.builder()
                .withDictionary(kNonAscii)
                .buildStreamingJson();

        json_encoder.startEncoding(delta);
        json_encoder.encodeChunk(kTarget, 0, kTarget.length, delta);
    }

    @Ignore
    @Test
    public void NonasciiTargetWithJSON() throws Exception {
        json_encoder_.startEncoding(delta);
        json_encoder_.encodeChunk(kNonAscii,0, kNonAscii.length, delta);
    }
}
