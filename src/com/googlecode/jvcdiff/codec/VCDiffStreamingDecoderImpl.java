package com.googlecode.jvcdiff.codec;

import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jvcdiff.VCDiffAddressCache;
import com.googlecode.jvcdiff.VCDiffCodeTableData;

public class VCDiffStreamingDecoderImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffStreamingDecoderImpl.class);
	
	// The default maximum target file size (and target window size) if
	// SetMaximumTargetFileSize() is not called.
	public static final int kDefaultMaximumTargetFileSize = 67108864;  // 64 MB

	// The largest value that can be passed to SetMaximumTargetWindowSize().
	// Using a larger value will result in an error.
	public static final int kTargetSizeLimit = Integer.MAX_VALUE;

	// A constant that is the default value for planned_target_file_size_,
	// indicating that the decoder does not have an expected length
	// for the target data.
	public static final int kUnlimitedBytes = -3;

	public VCDiffStreamingDecoderImpl() {

	}

	// Resets all member variables to their initial states.
	public void Reset() {
		// TODO:
	}

	// These functions are identical to their counterparts
	// in VCDiffStreamingDecoder.
	//
	public void StartDecoding(byte[] dictionary_ptr, int dictionary_size) {
		// TODO:
	}

	public boolean DecodeChunk(byte[] data, int len, OutputStream output_string) {

	}

	public boolean FinishDecoding() {
		// TODO:
	}

	// If true, the version of VCDIFF used in the current delta file allows
	// for the interleaved format, in which instructions, addresses and data
	// are all sent interleaved in the instructions section of each window
	// rather than being sent in separate sections.  This is not part of
	// the VCDIFF draft standard, so we've defined a special version code
	// 'S' which implies that this feature is available.  Even if interleaving
	// is supported, it is not mandatory; interleaved format will be implied
	// if the address and data sections are both zero-length.
	//
	public boolean AllowInterleaved() { return vcdiff_version_code_ == 'S'; }

	// If true, the version of VCDIFF used in the current delta file allows
	// each delta window to contain an Adler32 checksum of the target window data.
	// If the bit 0x08 (VCD_CHECKSUM) is set in the Win_Indicator flags, then
	// this checksum will appear as a variable-length integer, just after the
	// "length of addresses for COPYs" value and before the window data sections.
	// It is possible for some windows in a delta file to use the checksum feature
	// and for others not to use it (and leave the flag bit set to 0.)
	// Just as with AllowInterleaved(), this extension is not part of the draft
	// standard and is only available when the version code 'S' is specified.
	//
	public boolean AllowChecksum() { return vcdiff_version_code_ == 'S'; }

	public boolean SetMaximumTargetFileSize(int new_maximum_target_file_size) {
		maximum_target_file_size_ = new_maximum_target_file_size;
		return true;
	}

	public boolean SetMaximumTargetWindowSize(int new_maximum_target_window_size) {
		if (new_maximum_target_window_size > kTargetSizeLimit) {
			LOGGER.error("Specified maximum target window size {} exceeds limit of {} bytes",
					new_maximum_target_window_size, kTargetSizeLimit);
			return false;
		}
		maximum_target_window_size_ = new_maximum_target_window_size;
		return true;
	}

	// See description of planned_target_file_size_, below.
	public boolean HasPlannedTargetFileSize() {
		return planned_target_file_size_ != kUnlimitedBytes;
	}

	public void SetPlannedTargetFileSize(int planned_target_file_size) {
		planned_target_file_size_ = planned_target_file_size;
	}

	public void AddToTotalTargetWindowSize(int window_size) {
		total_of_target_window_sizes_ += window_size;
	}

	// Checks to see whether the decoded target data has reached its planned size.
	public boolean ReachedPlannedTargetFileSize() {
		if (!HasPlannedTargetFileSize()) {
			return false;
		}
		// The planned target file size should not have been exceeded.
		// TargetWindowWouldExceedSizeLimits() ensures that the advertised size of
		// each target window would not make the target file exceed that limit, and
		// DecodeBody() will return RESULT_ERROR if the actual decoded output ever
		// exceeds the advertised target window size.
		if (total_of_target_window_sizes_ > planned_target_file_size_) {
			LOGGER.error("Internal error: Decoded data size {} exceeds planned target file size {}",
					total_of_target_window_sizes_, planned_target_file_size_);
			return true;
		}
		return total_of_target_window_sizes_ == planned_target_file_size_;
	}

	// Checks to see whether adding a new target window of the specified size
	// would exceed the planned target file size, the maximum target file size,
	// or the maximum target window size.  If so, logs an error and returns true;
	// otherwise, returns false.
	public boolean TargetWindowWouldExceedSizeLimits(int window_size) {
		// TODO:
	}

	// Returns the amount of input data passed to the last DecodeChunk()
	// that was not consumed by the decoder.  This is essential if
	// SetPlannedTargetFileSize() is being used, in order to preserve the
	// remaining input data stream once the planned target file has been decoded.
	public int GetUnconsumedDataSize() {
		return unparsed_bytes_.size();
	}

	// This function will return true if the decoder has parsed a complete delta
	// file header plus zero or more delta file windows, with no data left over.
	// It will also return true if no delta data at all was decoded.  If these
	// conditions are not met, then FinishDecoding() should not be called.
	public boolean IsDecodingComplete() {
		if (!FoundFileHeader()) {
			// No complete delta file header has been parsed yet.  DecodeChunk()
			// may have received some data that it hasn't yet parsed, in which case
			// decoding is incomplete.
			return unparsed_bytes_.empty();
		} else if (custom_code_table_decoder_.get()) {
			// The decoder is in the middle of parsing a custom code table.
			return false;
		} else if (delta_window_.FoundWindowHeader()) {
			// The decoder is in the middle of parsing an interleaved format delta
			// window.
			return false;
		} else if (ReachedPlannedTargetFileSize()) {
			// The decoder found exactly the planned number of bytes.  In this case
			// it is OK for unparsed_bytes_ to be non-empty; it contains the leftover
			// data after the end of the delta file.
			return true;
		} else {
			// No complete delta file window has been parsed yet.  DecodeChunk()
			// may have received some data that it hasn't yet parsed, in which case
			// decoding is incomplete.
			return unparsed_bytes_.empty();
		}
	}

	public const char* dictionary_ptr() { return dictionary_ptr_; }

	public int dictionary_size() { return dictionary_size_; }

	public VCDiffAddressCache addr_cache() { return addr_cache_; }

	public string* decoded_target() { return &decoded_target_; }

	public boolean allow_vcd_target() { return allow_vcd_target_; }

	public void SetAllowVcdTarget(boolean allow_vcd_target) {
		if (start_decoding_was_called_) {
			LOGGER.error("SetAllowVcdTarget() called after StartDecoding()");
			return;
		}
		allow_vcd_target_ = allow_vcd_target;
	}

	// Reads the VCDiff delta file header section as described in RFC section 4.1,
	// except the custom code table data.  Returns RESULT_ERROR if an error
	// occurred, or RESULT_END_OF_DATA if the end of available data was reached
	// before the entire header could be read.  (The latter may be an error
	// condition if there is no more data available.)  Otherwise, advances
	// data->position_ past the header and returns RESULT_SUCCESS.
	//
	private int ReadDeltaFileHeader(ParseableChunk data) {
		
	}

	// Indicates whether or not the header has already been read.
	private boolean FoundFileHeader() { return addr_cache_ != null; }

	// If ReadDeltaFileHeader() finds the VCD_CODETABLE flag set within the delta
	// file header, this function parses the custom cache sizes and initializes
	// a nested VCDiffStreamingDecoderImpl object that will be used to parse the
	// custom code table in ReadCustomCodeTable().  Returns RESULT_ERROR if an
	// error occurred, or RESULT_END_OF_DATA if the end of available data was
	// reached before the custom cache sizes could be read.  Otherwise, returns
	// the number of bytes read.
	//
	private int InitCustomCodeTable(const char* data_start, const char* data_end);

	// If a custom code table was specified in the header section that was parsed
	// by ReadDeltaFileHeader(), this function makes a recursive call to another
	// VCDiffStreamingDecoderImpl object (custom_code_table_decoder_), since the
	// custom code table is expected to be supplied as an embedded VCDIFF
	// encoding that uses the standard code table.  Returns RESULT_ERROR if an
	// error occurs, or RESULT_END_OF_DATA if the end of available data was
	// reached before the entire custom code table could be read.  Otherwise,
	// returns RESULT_SUCCESS and sets *data_ptr to the position after the encoded
	// custom code table.  If the function returns RESULT_SUCCESS or
	// RESULT_END_OF_DATA, it advances data->position_ past the parsed bytes.
	//
	private VCDiffResult ReadCustomCodeTable(ParseableChunk* data);

	// Called after the decoder exhausts all input data.  This function
	// copies from decoded_target_ into output_string all the data that
	// has not yet been output.  It sets decoded_target_output_position_
	// to mark the start of the next data that needs to be output.
	private void AppendNewOutputText(OutputStream output_string) {
		// TODO:
	}

	// Appends to output_string the portion of decoded_target_ that has
	// not yet been output, then clears decoded_target_.  This function is
	// called after each complete target window has been decoded if
	// allow_vcd_target is false.  In that case, there is no need to retain
	// target data from any window except the current window.
	private void FlushDecodedTarget(OutputStream output_string) {
		// TODO:
	}

	// Contents and length of the source (dictionary) data.
	private const char* dictionary_ptr_;
	private int dictionary_size_;

	// This string will be used to store any unparsed bytes left over when
	// DecodeChunk() reaches the end of its input and returns RESULT_END_OF_DATA.
	// It will also be used to concatenate those unparsed bytes with the data
	// supplied to the next call to DecodeChunk(), so that they appear in
	// contiguous memory.
	private string unparsed_bytes_;

	// The portion of the target file that has been decoded so far.  This will be
	// used to fill the output string for DecodeChunk(), and will also be used to
	// execute COPY instructions that reference target data.  Since the source
	// window can come from a range of addresses in the previously decoded target
	// data, the entire target file needs to be available to the decoder, not just
	// the current target window.
	private string decoded_target_;

	// The VCDIFF version byte (also known as "header4") from the
	// delta file header.
	private byte vcdiff_version_code_;

	private VCDiffDeltaFileWindow delta_window_;

	private VCDiffAddressCache addr_cache_;

	// Will be NULL unless a custom code table has been defined.
	private VCDiffCodeTableData custom_code_table_;

	// Used to receive the decoded custom code table.
	private string custom_code_table_string_;

	// If a custom code table is specified, it will be expressed
	// as an embedded VCDIFF delta file which uses the default code table
	// as the source file (dictionary).  Use a child decoder object
	// to decode that delta file.
	private VCDiffStreamingDecoderImpl custom_code_table_decoder_;

	// If set, then the decoder is expecting *exactly* this number of
	// target bytes to be decoded from one or more delta file windows.
	// If this number is exceeded while decoding a window, but was not met
	// before starting on that window, an error will be reported.
	// If FinishDecoding() is called before this number is met, an error
	// will also be reported.  This feature is used for decoding the
	// embedded code table data within a VCDIFF delta file; we want to
	// stop processing the embedded data once the entire code table has
	// been decoded, and treat the rest of the available data as part
	// of the enclosing delta file.
	private int planned_target_file_size_;

	private int maximum_target_file_size_;

	private int maximum_target_window_size_;

	// Contains the sum of the decoded sizes of all target windows seen so far,
	// including the expected total size of the current target window in progress
	// (even if some of the current target window has not yet been decoded.)
	private int total_of_target_window_sizes_;

	// Contains the byte position within decoded_target_ of the first data that
	// has not yet been output by AppendNewOutputText().
	private int decoded_target_output_position_;

	// This value is used to ensure the correct order of calls to the interface
	// functions, i.e., a single call to StartDecoding(), followed by zero or
	// more calls to DecodeChunk(), followed by a single call to
	// FinishDecoding().
	private boolean start_decoding_was_called_;

	// If this value is true then the VCD_TARGET flag can be specified to allow
	// the source segment to be chosen from the previously-decoded target data.
	// (This is the default behavior.)  If it is false, then specifying the
	// VCD_TARGET flag is considered an error, and the decoder does not need to
	// keep in memory any decoded target data prior to the current window.
	private boolean allow_vcd_target_;
}
