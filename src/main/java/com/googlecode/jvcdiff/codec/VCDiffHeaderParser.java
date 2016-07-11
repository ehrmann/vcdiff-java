package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.VarInt;
import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_TARGET;

public class VCDiffHeaderParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffHeaderParser.class);
	
	public static final short RESULT_SUCCESS = 0;
	public static final short RESULT_END_OF_DATA = -2;
	public static final short RESULT_ERROR = -1;

	public static final byte VCD_DECOMPRESS = 0x01;
	public static final byte VCD_CODETABLE = 0x02;

    public static final byte VCD_DATACOMP = 0x01;
    public static final byte VCD_INSTCOMP = 0x02;
    public static final byte VCD_ADDRCOMP = 0x04;


	// Contains the result code of the last Parse...() operation that failed
	// (RESULT_ERROR or RESULT_END_OF_DATA).  If no Parse...() method has been
	// called, or if all calls to Parse...() were successful, then this contains
	// RESULT_SUCCESS.
	protected short return_code_;

	// Will be zero until ParseWindowLengths() has been called.  After
	// ParseWindowLengths() has been called successfully, this contains the
	// parsed length of the delta encoding.
	protected Integer delta_encoding_length_;

	protected final ByteBuffer buffer;
    protected ByteBuffer delta_encoding_start_;

	public VCDiffHeaderParser(ByteBuffer buffer) {
		this.return_code_ = RESULT_SUCCESS;
		this.delta_encoding_length_ = 0;
		this.buffer = buffer.duplicate();
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
		if (!buffer.hasRemaining()) {
			return_code_ = RESULT_END_OF_DATA;
			return null;
		}
		return buffer.get();
	}

	public Integer ParseInt32(String variable_description) {
		if (RESULT_SUCCESS != return_code_) {
			return null;
		}

        buffer.mark();
		try {
			return VarInt.getInt(buffer);
		} catch (VarIntParseException e) {
            return_code_ = RESULT_ERROR;
            buffer.reset();
			return null;
		} catch (VarIntEndOfBufferException e) {
            LOGGER.error("Expected {}; found invalid variable-length integer", variable_description);
            return_code_ = RESULT_END_OF_DATA;
            buffer.reset();
			return null;
		}
	}

	// When an unsigned 32-bit integer is expected, parse a signed 64-bit value
	// instead, then check the value limit.  The uint32_t type can't be parsed
	// directly because two negative values are given special meanings (RESULT_ERROR
	// and RESULT_END_OF_DATA) and could not be expressed in an unsigned format.
	public Integer ParseUInt32(String variable_description) {
        if (RESULT_SUCCESS != return_code_) {
            return null;
        }
		try {
            buffer.mark();
			long parsedValue = VarInt.getLong(buffer);
			if ((parsedValue & 0xffffffff00000000L) != 0) {
				LOGGER.error("Value of {} ({}) is too large for unsigned 32-bit integer", variable_description, parsedValue);
				return_code_ = RESULT_ERROR;
				buffer.reset();
			} else {
                return (int) parsedValue;
            }
		} catch (VarInt.VarIntEndOfBufferException e) {
			return_code_ = RESULT_END_OF_DATA;
			buffer.reset();
		} catch (VarInt.VarIntParseException e) {
            return_code_ = RESULT_ERROR;
            buffer.reset();
		}

        return null;
	}

	public Integer ParseChecksum(String variable_description) {
		return ParseUInt32(variable_description);
	}

	public Integer ParseSize(String variable_description) {
		return ParseInt32(variable_description);
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
    public DeltaWindowHeader ParseWinIndicatorAndSourceSegment(int dictionary_size,
                                                               int decoded_target_size,
                                                               boolean allow_vcd_target) {
        Byte win_indicator = this.ParseByte();
        if (win_indicator == null) {
            return null;
        }

        int source_target_flags = win_indicator & (VCD_SOURCE | VCD_TARGET);
        switch (source_target_flags) {
            case VCD_SOURCE:
                return ParseSourceSegmentLengthAndPosition(
                        dictionary_size,
                        win_indicator,
                        "end of dictionary",
                        "dictionary"
                );
            case VCD_TARGET:
                if (!allow_vcd_target) {
                    LOGGER.error("Delta file contains VCD_TARGET flag, which is not allowed by current decoder settings");
                    return_code_ = RESULT_ERROR;
                    return null;
                }
                return ParseSourceSegmentLengthAndPosition(
                        decoded_target_size,
                        win_indicator,
                        "current target position",
                        "target file"
                );
            case VCD_SOURCE | VCD_TARGET:
                LOGGER.error("Win_Indicator must not have both VCD_SOURCE and VCD_TARGET set");
                return_code_ = RESULT_ERROR;
                return null;
            default:
                return new DeltaWindowHeader(win_indicator, -1, -1);
        }
    }

	// Parses the following two elements of the delta window header:
	//
	//     Length of the delta encoding             - integer (VarintBE format)
	//     Size of the target window                - integer (VarintBE format)
	//
	// Return conditions and values are the same as for
	// ParseWinIndicatorAndSourceSegment(), above.
	public Integer ParseWindowLengths() {
        if (delta_encoding_start_ != null) {
            LOGGER.error("Internal error: VCDiffHeaderParser.ParseWindowLengths was called twice for the same delta window");
            return_code_ = RESULT_ERROR;
            return null;
        }

        delta_encoding_length_ = ParseSize("length of the delta encoding");
        if (delta_encoding_length_ == null) {
            return null;
        }

        delta_encoding_start_ = buffer.duplicate();
        return ParseSize("size of the target window");
	}

	// May only be called after ParseWindowLengths() has returned RESULT_SUCCESS.
	// Returns a pointer to the end of the delta window (which might not point to
	// a valid memory location if there is insufficient input data.)
	public Integer EndOfDeltaWindow() {
        if (delta_encoding_start_ == null) {
            LOGGER.error("Internal error: VCDiffHeaderParser.GetDeltaWindowEnd was called before ParseWindowLengths");
            return null;
        }
        return delta_encoding_start_.position() + delta_encoding_length_;
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
        Byte deltaIndicator = ParseByte();
        if (deltaIndicator == null) {
            return false;
        }
        if ((deltaIndicator & (VCD_DATACOMP | VCD_INSTCOMP | VCD_ADDRCOMP)) != 0) {
            LOGGER.error("Secondary compression of delta file sections is not supported");
            return_code_ = RESULT_ERROR;
            return false;
        }
        return true;
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
    public SectionLengths ParseSectionLengths(boolean has_checksum) {
        Integer add_and_run_data_length = ParseSize("length of data for ADDs and RUNs");
        Integer instructions_and_sizes_length = ParseSize("length of instructions section");
        Integer addresses_length = ParseSize("length of addresses for COPYs");
        Integer checksum = null;
        if (has_checksum) {
            checksum = ParseChecksum("Adler32 checksum value");
        }
        if (RESULT_SUCCESS != return_code_) {
            return null;
        }
        if (delta_encoding_start_ == null) {
            LOGGER.error("Internal error: VCDiffHeaderParser.ParseSectionLengths was called before ParseWindowLengths");
            return_code_ = RESULT_ERROR;
            return null;
        }

        long delta_encoding_header_length = buffer.position() - delta_encoding_start_.position();
        long expected_delta_encoding_length = delta_encoding_header_length + add_and_run_data_length +
                instructions_and_sizes_length + addresses_length;

        if (delta_encoding_length_ != expected_delta_encoding_length) {
            LOGGER.error("The length of the delta encoding does not match the size of the header plus the sizes of the data sections");
            return_code_ = RESULT_ERROR;
            return null;
        }

        return new SectionLengths(add_and_run_data_length, instructions_and_sizes_length, addresses_length, checksum != null ? checksum : 0);
    }

	// If one of the Parse... functions returned false, this function
	// can be used to find the result code (RESULT_ERROR or RESULT_END_OF_DATA)
	// describing the reason for the most recent parse failure.  If none of the
	// Parse... functions has returned false, returns RESULT_SUCCESS.
	public short GetResult() {
		return return_code_;
	}

	public ByteBuffer unparsedData() {
		return buffer.duplicate().asReadOnlyBuffer();
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
    private DeltaWindowHeader ParseSourceSegmentLengthAndPosition(long from_size, byte win_indicator,
                                                                  String from_boundary_name,
                                                                  String from_name) {
        // Verify the length and position values
        Integer source_segment_length = ParseSize("source segment length");
        if (source_segment_length == null) {
            return null;
        }
        // Guard against overflow by checking source length first
        if (source_segment_length > from_size) {
            LOGGER.error( "Source segment length ({}) is larger than {} ({})",
                    source_segment_length, from_name, from_size);
            return_code_ = RESULT_ERROR;
            return null;
        }

        Integer source_segment_position = ParseSize("source segment position");
        if (source_segment_position == null) {
            return null;
        }
        if ((source_segment_position >= from_size) && (source_segment_length > 0)) {
            LOGGER.error("Source segment position ({}) is past {} ({})",
                    source_segment_position, from_boundary_name, from_size);
            return_code_ = RESULT_ERROR;
            return null;
        }
        int source_segment_end = source_segment_position + source_segment_length;
        if (source_segment_end > from_size) {
            LOGGER.error("Source segment end position ({}) is past {} ({})",
                    source_segment_end, from_boundary_name, from_size);
            return_code_ = RESULT_ERROR;
            return null;
        }
        return new DeltaWindowHeader(win_indicator, source_segment_length, source_segment_position);
    }

    public static final class DeltaWindowHeader {
        public final byte win_indicator;
        public final int source_segment_length;
        public final int source_segment_position;

        public DeltaWindowHeader(byte win_indicator, int source_segment_length, int source_segment_position) {
            this.win_indicator = win_indicator;
            this.source_segment_length = source_segment_length;
            this.source_segment_position = source_segment_position;
        }
    }

    public static final class SectionLengths {
        public final int add_and_run_data_length;
        public final int instructions_and_sizes_length;
        public final int addresses_length;
        public final int checksum;

        public SectionLengths(int add_and_run_data_length, int instructions_and_sizes_length, int addresses_length, int checksum) {
            this.add_and_run_data_length = add_and_run_data_length;
            this.instructions_and_sizes_length = instructions_and_sizes_length;
            this.addresses_length = addresses_length;
            this.checksum = checksum;
        }
    }
}
