// Copyright 2008 Google Inc.
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

package com.googlecode.jvcdiff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_TARGET;

// Used to parse the bytes and Varints that make up the delta file header
// or delta window header.
public class VCDiffHeaderParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffHeaderParser.class);

    // The possible values for the Delta_Indicator field, as described
    // in section 4.3 of the RFC:
    //
    //    "Delta_Indicator:
    //     This byte is a set of bits, as shown:
    //
    //      7 6 5 4 3 2 1 0
    //     +-+-+-+-+-+-+-+-+
    //     | | | | | | | | |
    //     +-+-+-+-+-+-+-+-+
    //                ^ ^ ^
    //                | | |
    //                | | +-- VCD_DATACOMP
    //                | +---- VCD_INSTCOMP
    //                +------ VCD_ADDRCOMP
    //
    //          VCD_DATACOMP:   bit value 1.
    //          VCD_INSTCOMP:   bit value 2.
    //          VCD_ADDRCOMP:   bit value 4.
    //
    //     [...] If the bit VCD_DECOMPRESS (Section 4.1) was on, each of these
    //     sections may have been compressed using the specified secondary
    //     compressor.  The bit positions 0 (VCD_DATACOMP), 1
    //     (VCD_INSTCOMP), and 2 (VCD_ADDRCOMP) respectively indicate, if
    //     non-zero, that the corresponding parts are compressed."
    // [Secondary compressors are not supported, so open-vcdiff decoding will fail
    //  if these bits are not all zero.]
    //
    public static final byte VCD_DATACOMP = 0x01;
    public static final byte VCD_INSTCOMP = 0x02;
    public static final byte VCD_ADDRCOMP = 0x04;

    private final ByteBuffer buffer;
    private long delta_encoding_length_;

    private ByteBuffer delta_encoding_start_;

    private ParseException parseException = null;
    private BufferUnderflowException bufferUnderflowException = null;

    public VCDiffHeaderParser(ByteBuffer buffer) {
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
    //
    public byte ParseByte() throws BufferUnderflowException, ParseException {
        throwExistingException();
        return buffer.get();
    }

    public int ParseInt32(String variable_description) throws BufferUnderflowException, ParseException {
        throwExistingException();
        buffer.mark();
        try {
            return VarInt.getInt(buffer);
        } catch (VarInt.VarIntEndOfBufferException e) {
            buffer.reset();
            bufferUnderflowException = new BufferUnderflowException();
            throw bufferUnderflowException;
        } catch (VarInt.VarIntParseException e) {
            parseException = new ParseException();
            buffer.reset();
            throw parseException;
        }
    }

    // When an unsigned 32-bit integer is expected, parse a signed 64-bit value
    // instead, then check the value limit.  The uint32_t type can't be parsed
    // directly because two negative values are given special meanings (RESULT_ERROR
    // and RESULT_END_OF_DATA) and could not be expressed in an unsigned format.
    public int ParseUInt32(String variable_description) throws BufferUnderflowException, ParseException {
        throwExistingException();
        buffer.mark();

        try {
            long parsedValue = VarInt.getLong(buffer);
            if ((parsedValue & 0xffffffff00000000L) != 0) {
                LOGGER.error("Value of {} ({}) is too large for unsigned 32-bit integer", variable_description, parsedValue);
                parseException = new ParseException();
                buffer.reset();
                throw parseException;
            }
            return (int) parsedValue;
        } catch (VarInt.VarIntEndOfBufferException e) {
            bufferUnderflowException = new BufferUnderflowException();
            buffer.reset();
            throw bufferUnderflowException;
        } catch (VarInt.VarIntParseException e) {
            parseException = new ParseException();
            buffer.reset();
            throw parseException;
        }
    }

    public int ParseChecksum(String variable_description) throws BufferUnderflowException, ParseException {
        return ParseUInt32(variable_description);
    }

    public int ParseSize(String variable_description) throws BufferUnderflowException, ParseException {
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
    public DeltaWindowHeader ParseWinIndicatorAndSourceSegment(long dictionary_size,
                                                               long decoded_target_size,
                                                               boolean allow_vcd_target)
            throws BufferUnderflowException, ParseException {
        byte win_indicator = this.ParseByte();
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
                    throw new ParseException();
                }
                return ParseSourceSegmentLengthAndPosition(
                        decoded_target_size,
                        win_indicator,
                        "current target position",
                        "target file"
                );
            case VCD_SOURCE | VCD_TARGET:
                LOGGER.error("Win_Indicator must not have both VCD_SOURCE and VCD_TARGET set");
                throw new ParseException();
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
    //
    public long ParseWindowLengths() throws BufferUnderflowException, ParseException {
        if (delta_encoding_start_ != null) {
            LOGGER.error("Internal error: VCDiffHeaderParser.ParseWindowLengths was called twice for the same delta window");
            throw new IllegalStateException("ParseWindowLengths was called twice for the same delta window");
        }

        delta_encoding_length_ = ParseSize("length of the delta encoding");

        delta_encoding_start_ = buffer.duplicate();
        return ParseSize("size of the target window");
    }

    // May only be called after ParseWindowLengths() has returned RESULT_SUCCESS.
    // Returns a pointer to the end of the delta window (which might not point to
    // a valid memory location if there is insufficient input data.)
    //
    public long EndOfDeltaWindow() throws BufferUnderflowException, ParseException {
        if (delta_encoding_start_ == null) {
            LOGGER.error("Internal error: VCDiffHeaderParser.GetDeltaWindowEnd was called before ParseWindowLengths");
            throw new IllegalStateException("GetDeltaWindowEnd was called before ParseWindowLengths");
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
    //
    public void ParseDeltaIndicator() throws BufferUnderflowException, ParseException {
        if ((ParseByte() & (VCD_DATACOMP | VCD_INSTCOMP | VCD_ADDRCOMP)) != 0) {
            LOGGER.error("Secondary compression of delta file sections is not supported");
            throw new ParseException();
        }
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
    public SectionLengths ParseSectionLengths(boolean has_checksum) throws BufferUnderflowException, ParseException {
        long add_and_run_data_length = ParseSize("length of data for ADDs and RUNs");
        long instructions_and_sizes_length = ParseSize("length of instructions section");
        long addresses_length = ParseSize("length of addresses for COPYs");

        int checksum = 0;
        if (has_checksum) {
            checksum = ParseChecksum("Adler32 checksum value");
        }

        if (delta_encoding_start_ == null) {
            LOGGER.error("Internal error: VCDiffHeaderParser.ParseSectionLengths was called before ParseWindowLengths");
            throw new IllegalStateException("ParseSectionLengths was called before ParseWindowLengths");
        }

        long delta_encoding_header_length = buffer.position() - delta_encoding_start_.position();
        long expected_delta_encoding_length = delta_encoding_header_length + add_and_run_data_length +
                instructions_and_sizes_length + addresses_length;

        if (delta_encoding_length_ != expected_delta_encoding_length) {
            LOGGER.error("The length of the delta encoding does not match the size of the header plus the sizes of the data sections");
            throw new ParseException();
        }

        return new SectionLengths(add_and_run_data_length, instructions_and_sizes_length, addresses_length, checksum);
    }

    protected ByteBuffer unparsedData() {
        return buffer.asReadOnlyBuffer();
    }

    private void throwExistingException() throws BufferUnderflowException, ParseException {
        if (bufferUnderflowException != null) {
            throw bufferUnderflowException;
        } else if (parseException != null) {
            throw parseException;
        }
    }

    private DeltaWindowHeader ParseSourceSegmentLengthAndPosition(long from_size, byte win_indicator,
                                                                  String from_boundary_name,
                                                                  String from_name) throws BufferUnderflowException, ParseException {
        return null;

    }

    public static class ParseException extends Exception {

    }

    public static final class DeltaWindowHeader {
        public final byte win_indicator;
        public final long source_segment_length;
        public final long source_segment_position;

        public DeltaWindowHeader(byte win_indicator, long source_segment_length, long source_segment_position) {
            this.win_indicator = win_indicator;
            this.source_segment_length = source_segment_length;
            this.source_segment_position = source_segment_position;
        }
    }

    public static final class SectionLengths {
        public final long add_and_run_data_length;
        public final long instructions_and_sizes_length;
        public final long addresses_length;
        public final int checksum;

        public SectionLengths(long add_and_run_data_length, long instructions_and_sizes_length, long addresses_length, int checksum) {
            this.add_and_run_data_length = add_and_run_data_length;
            this.instructions_and_sizes_length = instructions_and_sizes_length;
            this.addresses_length = addresses_length;
            this.checksum = checksum;
        }
    }
}
