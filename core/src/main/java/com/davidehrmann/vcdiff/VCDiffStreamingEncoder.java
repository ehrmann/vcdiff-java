// Copyright 2007-2016 Google Inc., David Ehrmann
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

package com.davidehrmann.vcdiff;

import java.io.IOException;

public interface VCDiffStreamingEncoder<OUT> {
    // The client should use these routines as follows:
    //    HashedDictionary hd(dictionary, dictionarySize);
    //    if (!hd.init()) {
    //      HandleError();
    //      return;
    //    }
    //    string output_string;
    //    VCDiffStreamingEncoder v(hd, false, false);
    //    if (!v.startEncoding(&output_string)) {
    //      HandleError();
    //      return;  // No need to call finishEncoding()
    //    }
    //    Process(output_string.data(), output_string.size());
    //    output_string.clear();
    //    while (get data_buf) {
    //      if (!v.encodeChunk(data_buf, data_len, &output_string)) {
    //        HandleError();
    //        return;  // No need to call finishEncoding()
    //      }
    //      // The encoding is appended to output_string at each call,
    //      // so clear output_string once its contents have been processed.
    //      Process(output_string.data(), output_string.size());
    //      output_string.clear();
    //    }
    //    if (!v.finishEncoding(&output_string)) {
    //      HandleError();
    //      return;
    //    }
    //    Process(output_string.data(), output_string.size());
    //    output_string.clear();
    //
    // I.e., the allowed pattern of calls is
    //    startEncoding encodeChunk* finishEncoding
    //
    // The size of the encoded output depends on the sizes of the chunks
    // passed in (i.e. the chunking boundary affects compression).
    // However the decoded output is independent of chunk boundaries.

    // Sets up the data structures for encoding.
    // Writes a VCDIFF delta file header (as defined in RFC section 4.1)
    // to *output_string.
    //
    // Note: we *append*, so the old contents of *output_string stick around.
    // This convention differs from the non-streaming encode/decode
    // interfaces in VCDiffEncoder.
    //
    // If an error occurs, this function returns false; otherwise it returns true.
    // If this function returns false, the caller does not need to call
    // finishEncoding or to do any cleanup except destroying the
    // VCDiffStreamingEncoder object.
    void startEncoding(OUT out) throws IOException;

    // Appends compressed encoding for "data" (one complete VCDIFF delta window)
    // to *output_string.
    // If an error occurs (for example, if startEncoding was not called
    // earlier or startEncoding returned false), this function returns false;
    // otherwise it returns true.  The caller does not need to call finishEncoding
    // or do any cleanup except destroying the VCDiffStreamingEncoder
    // if this function returns false.
    void encodeChunk(byte[] data, int offset, int length, OUT out) throws IOException;

    void encodeChunk(byte[] data, OUT out) throws IOException;

    // Finishes encoding and appends any leftover encoded data to *output_string.
    // If an error occurs (for example, if startEncoding was not called
    // earlier or startEncoding returned false), this function returns false;
    // otherwise it returns true.  The caller does not need to
    // do any cleanup except destroying the VCDiffStreamingEncoder
    // if this function returns false.
    void finishEncoding(OUT out) throws IOException;
}
