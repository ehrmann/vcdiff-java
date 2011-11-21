package com.googlecode.jvcdiff.codec;

import java.io.OutputStream;


public interface VCDiffStreamingDecoder {
	/**
	 * Resets the dictionary contents to "dictionary_ptr[0,dictionary_size-1]"
	 * and sets up the data structures for decoding.  Note that the dictionary
	 * contents are not copied, and the client is responsible for ensuring that
	 * dictionary_ptr is valid until FinishDecoding is called.
	 */
	public abstract void StartDecoding(byte[] dictionary_ptr);

	/**
	 * Accepts "data[0,len-1]" as additional data received in the
	 * compressed stream.  If any chunks of data can be fully decoded,
	 * they are appended to output_string.
	 *
	 * Returns true on success, and false if the data was malformed
	 * or if there was an error in decoding it (e.g. out of memory, etc.).
	 *
	 * Note: we *append*, so the old contents of output_string stick around.
	 * This convention differs from the non-streaming Encode/Decode
	 * interfaces in VCDiffDecoder.
	 *
	 * output_string is guaranteed to be resized no more than once for each
	 * window in the VCDIFF delta file.  This rule is irrespective
	 * of the number of calls to DecodeChunk().
	 */
	public abstract boolean DecodeChunk(byte[] data, int offset, int length, OutputStream output_string);

	/**
	 * Finishes decoding after all data has been received.  Returns true
	 * if decoding of the entire stream was successful.  FinishDecoding()
	 * must be called for the current target before StartDecoding() can be
	 * called for a different target.
	 *
	 * @return
	 */
	public abstract boolean FinishDecoding();

	// *** Adjustable parameters ***

	/**
	 * Specifies the maximum allowable target file size.  If the decoder
	 * encounters a delta file that would cause it to create a target file larger
	 * than this limit, it will log an error and stop decoding.  If the decoder is
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
	 */
	public abstract boolean SetMaximumTargetFileSize(int new_maximum_target_file_size);

	/**
	 * Specifies the maximum allowable target *window* size.  (A target file is
	 * composed of zero or more target windows.)  If the decoder encounters a
	 * delta window that would cause it to create a target window larger
	 * than this limit, it will log an error and stop decoding.
	 */
	public abstract boolean SetMaximumTargetWindowSize(int new_maximum_target_window_size);

	/**
	 * This interface must be called before StartDecoding().  If its argument
	 * is true, then the VCD_TARGET flag can be specified to allow the source
	 * segment to be chosen from the previously-decoded target data.  (This is the
	 * default behavior.)  If it is false, then specifying the VCD_TARGET flag is
	 * considered an error, and the decoder does not need to keep in memory any
	 * decoded target data prior to the current window.
	 */
	public abstract void SetAllowVcdTarget(boolean allow_vcd_target);

}
