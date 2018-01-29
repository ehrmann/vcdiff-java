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

package com.davidehrmann.vcdiff;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public interface VCDiffStreamingDecoder {

    /**
     * @deprecated use {@link #startDecoding(ByteBuffer)}
     */
    @Deprecated
    void startDecoding(byte[] dictionary);

    /**
     * Resets the dictionary to dictionary parameter
     * and sets up the data structures for decoding.  Note that the dictionary
     * contents are not copied, and the call is responsible for ensuring that
     * dictionary is not modified until finishDecoding is called.
     *
     * @param dictionary dictionary the decoder is initialized with
     */
    void startDecoding(ByteBuffer dictionary);

    /**
     * @deprecated use {@link #decodeChunk(ByteBuffer, OutputStream)}
     *
     * Accepts "data[offset,offset+length-1]" as additional data received in the
     * compressed stream.  If any chunks of data can be fully decoded,
     * they are appended to out.
     *
     * @param data data to decoder
     * @param offset offset in data to decode from
     * @param length number of bytes in data to decide
     * @param out OutputStream to write decoded data to
     * @throws IOException if an error occurred decoding chunk or writing
     * the decoded chunk to out
     */
    @Deprecated
    void decodeChunk(byte[] data, int offset, int length, OutputStream out) throws IOException;

    /**
     * Accepts "data[offset,offset+length-1]" as additional data received in the
     * compressed stream.  If any chunks of data can be fully decoded,
     * they are appended to out.
     *
     * @param data data to decoder
     * @param out OutputStream to write decoded data to
     * @throws IOException if an error occurred decoding chunk or writing
     * the decoded chunk to out
     */
    void decodeChunk(ByteBuffer data, OutputStream out) throws IOException;

    /**
     * Convenience method equivalent to decodeChunk(data, 0, data.length, out)
     *
     * @param data data to decoder
     * @param out OutputStream to write decoded data to
     * @throws IOException if an error occurred decoding chunk or writing
     * the decoded chunk to out
     */
    void decodeChunk(byte[] data, OutputStream out) throws IOException;

    /**
     * Finishes decoding after all data has been received. finishDecoding()
     * must be called for the current target before startDecoding() can be
     * called for a different target.
     *
     * @throws IOException if there's an error finishing decoding
     *
     */
    void finishDecoding() throws IOException;

    /**
     * Specifies the maximum allowable target file size.  If the decoder
     * encounters a delta file that would cause it to create a target file larger
     * than this limit, it will throw an IOException and stop decoding.  If the decoder is
     * applied to delta files whose sizes vary greatly and whose contents can be
     * trusted, then a value larger than the the default value (64 MB) can be
     * specified to allow for maximum flexibility.  On the other hand, if the
     * input data is known never to exceed a particular size, and/or the input
     * data may be maliciously constructed, a lower value can be supplied in order
     * to guard against running out of memory or swapping to disk while decoding
     * an extremely large target file.  The argument must be between 0 and
     * INT32_MAX (2G); if it is within these bounds, the function will set the
     * limit and return true.  Otherwise, the function will return false and will
     * not change the limit.  Setting the limit to 0 will cause all decode
     * operations of non-empty target files to fail.
     *
     * @param newMaximumTargetFileSize maximum size allowed output size
     * @return whether or not the maximum target file size was changed successfully
     */
    boolean setMaximumTargetFileSize(long newMaximumTargetFileSize);

    /**
     * Specifies the maximum allowable target *window* size.  (A target file is
     * composed of zero or more target windows.)  If the decoder encounters a
     * delta window that would cause it to create a target window larger
     * than this limit, it will log an error and stop decoding.
     *
     * @param newMaximumTargetWindowSize maximum size of target windows allowed
     * @return whether or not the maximum target window size was changed successfully
     */
    boolean setMaximumTargetWindowSize(int newMaximumTargetWindowSize);

    /**
     * This interface must be called before startDecoding().  If its argument
     * is true, then the VCD_TARGET flag can be specified to allow the source
     * segment to be chosen from the previously-decoded target data.  (This is the
     * default behavior.)  If it is false, then specifying the VCD_TARGET flag is
     * considered an error, and the decoder does not need to keep in memory any
     * decoded target data prior to the current window.
     *
     * @param allowVcdTarget whether or not to allow the decoder to use decoded VCD
     *                       data as a target
     */
    void setAllowVcdTarget(boolean allowVcdTarget);

}
