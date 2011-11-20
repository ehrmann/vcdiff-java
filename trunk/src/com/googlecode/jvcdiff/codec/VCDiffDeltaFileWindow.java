package com.googlecode.jvcdiff.codec;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_CHECKSUM;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_TARGET;
import static com.googlecode.jvcdiff.codec.VCDiffHeaderParser.RESULT_ERROR;
import static com.googlecode.jvcdiff.codec.VCDiffHeaderParser.RESULT_SUCCESS;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jvcdiff.VCDiffCodeTableData;
import com.googlecode.jvcdiff.VCDiffCodeTableReader;

public class VCDiffDeltaFileWindow {
	private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffDeltaFileWindow.class);

	public VCDiffDeltaFileWindow(VCDiffStreamingDecoderImpl parent) {
		if (parent == null) {
			throw new NullPointerException();
		}
		this.parent_ = parent;
	}

	// Resets the pointers to the data sections in the current window.
	public void Reset() {
		found_header_ = false;

		// Mark the start of the current target window.
		target_window_start_pos_ = (parent_ != null) ? parent_.decoded_target().size() : 0;
		target_window_length_ = 0;

		source_segment_ptr_ = null;
		source_segment_length_ = 0;

		instructions_and_sizes_ = null;
		data_for_add_and_run_ = null;
		addresses_for_copy_ = null;

		interleaved_bytes_expected_ = 0;

		has_checksum_ = false;
		expected_checksum_ = 0;
	}

	public void UseCodeTable(VCDiffCodeTableData code_table_data, short max_mode) {
		reader_ = new VCDiffCodeTableReader(code_table_data, max_mode);
	}

	// Decodes a single delta window using the input data from *parseable_chunk.
	// Appends the decoded target window to parent_->decoded_target().  Returns
	// RESULT_SUCCESS if an entire window was decoded, or RESULT_END_OF_DATA if
	// the end of input was reached before the entire window could be decoded and
	// more input is expected (only possible if IsInterleaved() is true), or
	// RESULT_ERROR if an error occurred during decoding.  In the RESULT_ERROR
	// case, the value of parseable_chunk->pointer_ is undefined; otherwise,
	// parseable_chunk->Advance() is called to point to the input data position
	// just after the data that has been decoded.
	//
	public int DecodeWindow(ParseableChunk parseable_chunk) {
		return -1;
	}

	public boolean FoundWindowHeader() {
		return found_header_;
	}

	public boolean MoreDataExpected() {
		// When parsing an interleaved-format delta file,
		// every time DecodeBody() exits, interleaved_bytes_expected_
		// will be decremented by the number of bytes parsed.  If it
		// reaches zero, then there is no more data expected because
		// the size of the interleaved section (given in the window
		// header) has been reached.
		return IsInterleaved() && (interleaved_bytes_expected_ > 0);
	}

	public int target_window_start_pos() { return target_window_start_pos_; }

	public void set_target_window_start_pos(int new_start_pos) {
		target_window_start_pos_ = new_start_pos;
	}

	// Returns the number of bytes remaining to be decoded in the target window.
	// If not in the process of decoding a window, returns 0.
	public int TargetBytesRemaining() {
		return -1;
	}

	// Reads the header of the window section as described in RFC sections 4.2 and
	// 4.3, up to and including the value "Length of addresses for COPYs".  If the
	// entire header is found, this function sets up the DeltaWindowSections
	// instructions_and_sizes_, data_for_add_and_run_, and addresses_for_copy_ so
	// that the decoder can begin decoding the opcodes in these sections.  Returns
	// RESULT_ERROR if an error occurred, or RESULT_END_OF_DATA if the end of
	// available data was reached before the entire header could be read.  (The
	// latter may be an error condition if there is no more data available.)
	// Otherwise, returns RESULT_SUCCESS and advances parseable_chunk past the
	// parsed header.
	//
	private int ReadHeader(ParseableChunk parseable_chunk) {
		// Here are the elements of the delta window header to be parsed,
		// from section 4 of the RFC:
		//
		//		     Window1
		//		         Win_Indicator                            - byte
		//		         [Source segment size]                    - integer
		//		         [Source segment position]                - integer
		//		         The delta encoding of the target window
		//		             Length of the delta encoding         - integer
		//		             The delta encoding
		//		                 Size of the target window        - integer
		//		                 Delta_Indicator                  - byte
		//		                 Length of data for ADDs and RUNs - integer
		//		                 Length of instructions and sizes - integer
		//		                 Length of addresses for COPYs    - integer
		//		                 Data section for ADDs and RUNs   - array of bytes
		//		                 Instructions and sizes section   - array of bytes
		//		                 Addresses section for COPYs      - array of bytes
		//
		std::string* decoded_target = parent_->decoded_target();
		VCDiffHeaderParser header_parser(parseable_chunk.UnparsedData(),
				parseable_chunk->End());
		int source_segment_position = 0;
		unsigned char win_indicator = 0;
		if (!header_parser.ParseWinIndicatorAndSourceSegment(
				parent_.dictionary_size(),
				decoded_target.size(),
				parent_.allow_vcd_target(),
				&win_indicator,
				&source_segment_length_,
				&source_segment_position)) {
			return header_parser.GetResult();
		}
		has_checksum_ = parent_.AllowChecksum() && (win_indicator & VCD_CHECKSUM);
		if (!header_parser.ParseWindowLengths(&target_window_length_)) {
			return header_parser.GetResult();
		}
		if (parent_.TargetWindowWouldExceedSizeLimits(target_window_length_)) {
			// An error has been logged by TargetWindowWouldExceedSizeLimits().
			return RESULT_ERROR;
		}
		header_parser.ParseDeltaIndicator();
		int setup_return_code = SetUpWindowSections(&header_parser);
		if (RESULT_SUCCESS != setup_return_code) {
			return setup_return_code;
		}
		// Reserve enough space in the output string for the current target window.
		final int wanted_capacity =
			target_window_start_pos_ + target_window_length_;
		if (decoded_target.capacity() < wanted_capacity) {
			decoded_target.reserve(wanted_capacity);
		}
		// Get a pointer to the start of the source segment.
		if (win_indicator & VCD_SOURCE) {
			source_segment_ptr_ = parent_->dictionary_ptr() + source_segment_position;
		} else if (win_indicator & VCD_TARGET) {
			// This assignment must happen after the reserve().
			// decoded_target should not be resized again while processing this window,
			// so source_segment_ptr_ should remain valid.
			source_segment_ptr_ = decoded_target.data() + source_segment_position;
		}
		// The whole window header was found and parsed successfully.
		found_header_ = true;
		parseable_chunk.Advance(header_parser.ParsedSize());
		parent_.AddToTotalTargetWindowSize(target_window_length_);
		return RESULT_SUCCESS;
	}

	// After the window header has been parsed as far as the Delta_Indicator,
	// this function is called to parse the following delta window header fields:
	//
	//     Length of data for ADDs and RUNs - integer (VarintBE format)
	//     Length of instructions and sizes - integer (VarintBE format)
	//     Length of addresses for COPYs    - integer (VarintBE format)
	//
	// If has_checksum_ is true, it also looks for the following element:
	//
	//     Adler32 checksum            - unsigned 32-bit integer (VarintBE format)
	//
	// It sets up the DeltaWindowSections instructions_and_sizes_,
	// data_for_add_and_run_, and addresses_for_copy_.  If the interleaved format
	// is being used, all three sections will include the entire window body; if
	// the standard format is used, three non-overlapping window sections will be
	// defined.  Returns RESULT_ERROR if an error occurred, or RESULT_END_OF_DATA
	// if standard format is being used and there is not enough input data to read
	// the entire window body.  Otherwise, returns RESULT_SUCCESS.
	private int SetUpWindowSections(VCDiffHeaderParser header_parser) {
		int add_and_run_data_length = 0;
		int instructions_and_sizes_length = 0;
		int addresses_length = 0;
		if (!header_parser.ParseSectionLengths(has_checksum_,
				&add_and_run_data_length,
				&instructions_and_sizes_length,
				&addresses_length,
				&expected_checksum_)) {
			return header_parser.GetResult();
		}
		if (parent_.AllowInterleaved() &&
				(add_and_run_data_length == 0) &&
				(addresses_length == 0)) {
			// The interleaved format is being used.
			interleaved_bytes_expected_ =
				static_cast<int>(instructions_and_sizes_length);
				UpdateInterleavedSectionPointers(header_parser.UnparsedData(),
						header_parser.End());
		} else {
			// If interleaved format is not used, then the whole window contents
			// must be available before decoding can begin.  If only part of
			// the current window is available, then report end of data
			// and re-parse the whole header when DecodeChunk() is called again.
			if (header_parser.UnparsedSize() < (add_and_run_data_length +
					instructions_and_sizes_length +
					addresses_length)) {
				return RESULT_END_OF_DATA;
			}
			data_for_add_and_run_.Init(header_parser.UnparsedData(),
					add_and_run_data_length);
			instructions_and_sizes_.Init(data_for_add_and_run_.End(),
					instructions_and_sizes_length);
			addresses_for_copy_.Init(instructions_and_sizes_.End(), addresses_length);
			if (addresses_for_copy_.End() != header_parser.EndOfDeltaWindow()) {
				LOGGER.error("The end of the instructions section does not match the end of the delta window");
				return RESULT_ERROR;
			}
		}
		reader_.Init(instructions_and_sizes_.UnparsedDataAddr(),
				instructions_and_sizes_.End());
		return RESULT_SUCCESS;
	}

	// Decodes the body of the window section as described in RFC sections 4.3,
	// including the sections "Data section for ADDs and RUNs", "Instructions
	// and sizes section", and "Addresses section for COPYs".  These sections
	// must already have been set up by ReadWindowHeader().  Returns a
	// non-negative value on success, or RESULT_END_OF_DATA if the end of input
	// was reached before the entire window could be decoded (only possible if
	// IsInterleaved() is true), or RESULT_ERROR if an error occurred during
	// decoding.  Appends as much of the decoded target window as possible to
	// parent->decoded_target().
	//
	private int DecodeBody(ParseableChunk parseable_chunk) {
		return -1;
	}

	// Returns the number of bytes already decoded into the target window.
	private int TargetBytesDecoded() {
		return -1;
	}

	// Decodes a single ADD instruction, updating parent_->decoded_target_.
	private int DecodeAdd(int size) {
		return -1;
	}

	// Decodes a single RUN instruction, updating parent_->decoded_target_.
	private int DecodeRun(int size) {
		return -1;
	}

	// Decodes a single COPY instruction, updating parent_->decoded_target_.
	private int DecodeCopy(int size, short mode) {
		return -1;
	}

	// When using the interleaved format, this function is called both on parsing
	// the header and on resuming after a RESULT_END_OF_DATA was returned from a
	// previous call to DecodeBody().  It sets up all three section pointers to
	// reference the same interleaved stream of instructions, sizes, addresses,
	// and data.  These pointers must be reset every time that work resumes on a
	// delta window,  because the input data string may have been changed or
	// resized since DecodeBody() last returned.
	private void UpdateInterleavedSectionPointers(byte[] data, int offset, int length) {
		// Don't read past the end of currently-available data
		if (length > interleaved_bytes_expected_) {
			instructions_and_sizes_ = ByteBuffer.wrap(data, offset, interleaved_bytes_expected_);
		} else {
			instructions_and_sizes_ = ByteBuffer.wrap(data, offset, length);
		}

		data_for_add_and_run_ = instructions_and_sizes_;
		addresses_for_copy_ = instructions_and_sizes_;
	}

	// If true, the interleaved format described in AllowInterleaved() is used
	// for the current delta file.  Only valid after ReadWindowHeader() has been
	// called and returned a positive number (i.e., the whole header was parsed),
	// but before the window has finished decoding.
	//
	private boolean IsInterleaved() {
		// If the sections are interleaved, both addresses_for_copy_ and
		// data_for_add_and_run_ should point at instructions_and_sizes_.
		return (addresses_for_copy_ == instructions_and_sizes_ && data_for_add_and_run_ == instructions_and_sizes_);
	}

	// Executes a single COPY or ADD instruction, appending data to
	// parent_->decoded_target().
	private void CopyBytes(byte[] data, int offset, int size) {

	}

	// Executes a single RUN instruction, appending data to
	// parent_->decoded_target().
	private void RunByte(byte b, int size) {

	}

	// Advance *parseable_chunk to point to the current position in the
	// instructions/sizes section.  If interleaved format is used, then
	// decrement the number of expected bytes in the instructions/sizes section
	// by the number of instruction/size bytes parsed.
	private void UpdateInstructionPointer(ParseableChunk parseable_chunk) {

	}

	// The parent object which was passed to Init().
	private VCDiffStreamingDecoderImpl parent_;

	// This value will be true if VCDiffDeltaFileWindow::ReadDeltaWindowHeader()
	// has been called and succeeded in parsing the delta window header, but the
	// entire window has not yet been decoded.
	private boolean found_header_;

	// Contents and length of the current source window.  source_segment_ptr_
	// will be non-NULL if (a) the window section header for the current window
	// has been read, but the window has not yet finished decoding; or
	// (b) the window did not specify a source segment.
	//private const char* source_segment_ptr_;
	private int source_segment_length_;

	// The delta encoding window sections as defined in RFC section 4.3.
	// The pointer for each section will be incremented as data is consumed and
	// decoded from that section.  If the interleaved format is used,
	// data_for_add_and_run_ and addresses_for_copy_ will both point to
	// instructions_and_sizes_; otherwise, they will be separate data sections.
	//
	private ByteBuffer instructions_and_sizes_;
	private ByteBuffer data_for_add_and_run_;
	private ByteBuffer addresses_for_copy_;

	// The expected bytes left to decode in instructions_and_sizes_.  Only used
	// for the interleaved format.
	private int interleaved_bytes_expected_;

	// The expected length of the target window once it has been decoded.
	private int target_window_length_;

	// The index in decoded_target at which the first byte of the current
	// target window was/will be written.
	private int target_window_start_pos_;

	// If has_checksum_ is true, then expected_checksum_ contains an Adler32
	// checksum of the target window data.  This is an extension included in the
	// VCDIFF 'S' (SDCH) format, but is not part of the RFC 3284 draft standard.
	private boolean has_checksum_;
	private int expected_checksum_;

	private VCDiffCodeTableReader reader_;
}
