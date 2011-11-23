package com.googlecode.jvcdiff.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jvcdiff.VarInt;
import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;

public class VCDiffHeaderParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffHeaderParser.class);
	
	public static final short RESULT_SUCCESS = 0;
	public static final short RESULT_END_OF_DATA = -2;
	public static final short RESULT_ERROR = -1;

	public static final byte VCD_DECOMPRESS = 0x01;
	public static final byte VCD_CODETABLE = 0x02;

	
	protected ParseableChunk parseable_chunk_;

	// Contains the result code of the last Parse...() operation that failed
	// (RESULT_ERROR or RESULT_END_OF_DATA).  If no Parse...() method has been
	// called, or if all calls to Parse...() were successful, then this contains
	// RESULT_SUCCESS.
	protected short return_code_;

	// Will be zero until ParseWindowLengths() has been called.  After
	// ParseWindowLengths() has been called successfully, this contains the
	// parsed length of the delta encoding.
	protected int delta_encoding_length_;

	protected ByteBuffer buffer;

	public VCDiffHeaderParser(ByteBuffer buffer) {
		this.parseable_chunk_ = new ParseableChunk();
		this.return_code_ = RESULT_SUCCESS;
		this.delta_encoding_length_ = 0;
		this.buffer = buffer;
	}

	// One of these functions should be called for each element of the header.
	// variable_description is a description of the value that we are attempting
	// to parse, and will only be used to create descriptive error messages.
	// If the function returns true, then the element was parsed successfully
	// and its value has been placed in *value.  If the function returns false,
	// then *value is unchanged, and GetResult() can be called to return the
	// reason that the element could not be parsed, which will be either
	// RESULT_ERROR (an error occurred), or RESULT_END_OF_DATA (the limit data_end
	// was reached before the end of the element to be parsed.)  Once one of these
	// functions has returned false, further calls to any of the Parse...
	// functions will also return false without performing any additional actions.
	// Typical usage is as follows:
	//     int32_t segment_length = 0;
	//     if (!header_parser.ParseInt32("segment length", &segment_length)) {
	//       return header_parser.GetResult();
	//     }
	//
	// The following example takes advantage of the fact that calling a Parse...
	// function after an error or end-of-data condition is legal and does nothing.
	// It can thus parse more than one element in a row and check the status
	// afterwards.  If the first call to ParseInt32() fails, the second will have
	// no effect:
	//
	//     int32_t segment_length = 0, segment_position = 0;
	//     header_parser.ParseInt32("segment length", &segment_length));
	//     header_parser.ParseInt32("segment position", &segment_position));
	//     if (RESULT_SUCCESS != header_parser.GetResult()) {
	//       return header_parser.GetResult();
	//     }
	public Byte ParseByte() {
		if (RESULT_SUCCESS != return_code_) {
			return null;
		}
		if (parseable_chunk_.Empty()) {
			return_code_ = RESULT_END_OF_DATA;
			return null;
		}
		return buffer.get();
	}

	public Integer ParseInt32() {
		if (RESULT_SUCCESS != return_code_) {
			return null;
		}
		int parsed_value = 0;
		try {
			parsed_value = VarInt.getInt(buffer);
		} catch (VarIntParseException e) {
			return null;
		} catch (VarIntEndOfBufferException e) {
			return null;
		}

		switch (parsed_value) {
		case RESULT_ERROR:
			LOGGER.error("Expected " /*+ variable_description*/ + "; found invalid variable-length integer");
			return_code_ = RESULT_ERROR;
			return null;
		case RESULT_END_OF_DATA:
			return_code_ = RESULT_END_OF_DATA;
			return null;
		default:
			return parsed_value;
		}
	}

	public Long ParseUInt32() {
		return null;
		// TODO:
	}

	// Parses the first three elements of the delta window header:
	//
	//     Win_Indicator                            - byte
	//     [Source segment size]                    - integer (VarintBE format)
	//     [Source segment position]                - integer (VarintBE format)
	//
	// Returns true if the values were parsed successfully and the values were
	// found to be acceptable.  Returns false otherwise, in which case
	// GetResult() can be called to return the reason that the two values
	// could not be validated.  This will be either RESULT_ERROR (an error
	// occurred and was logged), or RESULT_END_OF_DATA (the limit data_end was
	// reached before the end of the values to be parsed.)  If return value is
	// true, then *win_indicator, *source_segment_length, and
	// *source_segment_position are populated with the parsed values.  Otherwise,
	// the values of these output arguments are undefined.
	//
	// dictionary_size: The size of the dictionary (source) file.  Used to
	//     validate the limits of source_segment_length and
	//     source_segment_position if the source segment is taken from the
	//     dictionary (i.e., if the parsed *win_indicator equals VCD_SOURCE.)
	// decoded_target_size: The size of the target data that has been decoded
	//     so far, including all target windows.  Used to validate the limits of
	//     source_segment_length and source_segment_position if the source segment
	//     is taken from the target (i.e., if the parsed *win_indicator equals
	//     VCD_TARGET.)
	// allow_vcd_target: If this argument is false, and the parsed *win_indicator
	//     is VCD_TARGET, then an error is produced; if true, VCD_TARGET is
	//     allowed.
	// win_indicator (output): Points to a single unsigned char (not an array)
	//     that will receive the parsed value of Win_Indicator.
	// source_segment_length (output): The parsed length of the source segment.
	// source_segment_position (output): The parsed zero-based index in the
	//     source/target file from which the source segment is to be taken.
	public boolean ParseWinIndicatorAndSourceSegment(int dictionary_size,
			int decoded_target_size,
			boolean allow_vcd_target,
			AtomicInteger win_indicator,
			AtomicInteger source_segment_length,
			AtomicInteger source_segment_position) {
		// TODO
		return false;
	}

	// Parses the following two elements of the delta window header:
	//
	//     Length of the delta encoding             - integer (VarintBE format)
	//     Size of the target window                - integer (VarintBE format)
	//
	// Return conditions and values are the same as for
	// ParseWinIndicatorAndSourceSegment(), above.
	public Integer ParseWindowLengths() {
		// TODO
		return 0;
	}

	// May only be called after ParseWindowLengths() has returned RESULT_SUCCESS.
	// Returns a pointer to the end of the delta window (which might not point to
	// a valid memory location if there is insufficient input data.)
	public int EndOfDeltaWindow() {
		// TODO
		return 0;
	}

	// Parses the following element of the delta window header:
	//
	//     Delta_Indicator                          - byte
	//
	// Because none of the bits in Delta_Indicator are used by this implementation
	// of VCDIFF, this function does not have an output argument to return the
	// value of that field.  It may return RESULT_SUCCESS, RESULT_ERROR, or
	// RESULT_END_OF_DATA as with the other Parse...() functions.
	public boolean ParseDeltaIndicator() {
		// TODO:	
		return false;
	}

	// Parses the following 3 elements of the delta window header:
	//
	//     Length of data for ADDs and RUNs - integer (VarintBE format)
	//     Length of instructions and sizes - integer (VarintBE format)
	//     Length of addresses for COPYs    - integer (VarintBE format)
	//
	// If has_checksum is true, it also looks for the following element:
	//
	//     Adler32 checksum            - unsigned 32-bit integer (VarintBE format)
	//
	// Return conditions and values are the same as for
	// ParseWinIndicatorAndSourceSegment(), above.
	//
	public boolean ParseSectionLengths(boolean has_checksum,
			AtomicInteger add_and_run_data_length,
			AtomicInteger instructions_and_sizes_length,
			AtomicInteger addresses_length,
			AtomicInteger checksum) {
		// TODO
		return false;
	}

	// If one of the Parse... functions returned false, this function
	// can be used to find the result code (RESULT_ERROR or RESULT_END_OF_DATA)
	// describing the reason for the most recent parse failure.  If none of the
	// Parse... functions has returned false, returns RESULT_SUCCESS.
	public short GetResult() {
		return return_code_;
	}

	// The following functions just pass their arguments to the underlying
	// ParseableChunk object.
	public int End() {
		return parseable_chunk_.End();
	}

	public int UnparsedSize() {
		return parseable_chunk_.UnparsedSize();
	}

	public int ParsedSize() {
		return parseable_chunk_.ParsedSize();
	}

	public int UnparsedData() {
		return parseable_chunk_.UnparsedData();
	}

	// Parses two variable-length integers representing the source segment length
	// and source segment position (== offset.)  Checks whether the source segment
	// length and position would cause it to exceed the size of the source file or
	// target file.  Returns true if the values were parsed successfully and the
	// values were found to be acceptable.  Returns false otherwise, in which case
	// GetResult() can be called to return the reason that the two values could
	// not be validated, which will be either RESULT_ERROR (an error occurred and
	// was logged), or RESULT_END_OF_DATA (the limit data_end was reached before
	// the end of the integers to be parsed.)
	// from_size: The requested size of the source segment.
	// from_boundary_name: A NULL-terminated string naming the end of the
	//     source or target file, used in error messages.
	// from_name: A NULL-terminated string naming the source or target file,
	//     also used in error messages.
	// source_segment_length (output): The parsed length of the source segment.
	// source_segment_position (output): The parsed zero-based index in the
	//     source/target file from which the source segment is to be taken.
	//
	protected boolean ParseSourceSegmentLengthAndPosition(int from_size,
			Appendable from_boundary_name,
			Appendable from_name,
			AtomicInteger source_segment_length,
			AtomicInteger source_segment_position) {
		// TODO:
		return false;
	}
}
