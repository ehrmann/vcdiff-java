package com.davidehrmann.vcdiff.codec;

import com.davidehrmann.vcdiff.VCDiffCodeTableData;
import com.davidehrmann.vcdiff.VCDiffCodeTableReader;
import com.davidehrmann.vcdiff.VarInt;
import com.davidehrmann.vcdiff.ZeroInitializedAdler32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Adler32;

import static com.davidehrmann.vcdiff.VCDiffCodeTableData.*;
import static com.davidehrmann.vcdiff.VCDiffCodeTableWriter.*;
import static com.davidehrmann.vcdiff.codec.VCDiffHeaderParser.*;

public class VCDiffDeltaFileWindow {
    private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffDeltaFileWindow.class);

    public VCDiffDeltaFileWindow(VCDiffStreamingDecoderImpl parent) {
        if (parent == null) {
            throw new NullPointerException();
        }
        this.parent_ = parent;
        Reset();
    }

    // Resets the pointers to the data sections in the current window.
    public void Reset() {
        found_header_ = false;

        // Mark the start of the current target window.
        target_window_start_pos_ = (parent_ != null) ? parent_.decoded_target().size() : 0;
        target_window_length_ = 0;

        source_segment_ptr_ = null;
        source_segment_length_.set(0);

        instructions_and_sizes_ = null;
        data_for_add_and_run_ = null;
        addresses_for_copy_ = null;

        interleaved_bytes_expected_ = 0;

        has_checksum_ = false;
        expected_checksum_.set(0);
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
    public int DecodeWindow(ByteBuffer parseable_chunk) {
        if (!found_header_) {
            switch (ReadHeader(parseable_chunk)) {
            case RESULT_END_OF_DATA:
                return RESULT_END_OF_DATA;
            case RESULT_ERROR:
                return RESULT_ERROR;
            default:
                // Reset address cache between windows (RFC section 5.1)
                parent_.addr_cache().Init();
            }
        } else {
            // We are resuming a window that was partially decoded before a
            // RESULT_END_OF_DATA was returned.  This can only happen on the first
            // loop iteration, and only if the interleaved format is enabled and used.
            if (!IsInterleaved()) {
                LOGGER.error("Internal error: Resumed decoding of a delta file window when interleaved format is not being used");
                return RESULT_ERROR;
            }
            // FIXME: ?
            UpdateInterleavedSectionPointers(parseable_chunk);
            reader_.UpdatePointers(instructions_and_sizes_);
        }
        switch (DecodeBody(parseable_chunk)) {
        case RESULT_END_OF_DATA:
            if (MoreDataExpected()) {
                return RESULT_END_OF_DATA;
            } else {
                LOGGER.error("End of data reached while decoding VCDIFF delta file");
                // fall through to RESULT_ERROR case
            }
        case RESULT_ERROR:
            return RESULT_ERROR;
        default:
            break;  // DecodeBody succeeded
        }
        // Get ready to read a new delta window
        Reset();
        return RESULT_SUCCESS;
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
        if (target_window_length_ == 0) {
            // There is no window being decoded at present
            return 0;
        } else {
            return target_window_length_ - TargetBytesDecoded();
        }
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
    private int ReadHeader(ByteBuffer parseable_chunk) {
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
        VCDiffStreamingDecoderImpl.DecoratedByteArrayOutputStream decoded_target = parent_.decoded_target();
        VCDiffHeaderParser header_parser = new VCDiffHeaderParser(parseable_chunk.slice());

        VCDiffHeaderParser.DeltaWindowHeader deltaWindowHeader = header_parser.ParseWinIndicatorAndSourceSegment(
                parent_.dictionary_size(),
                decoded_target.size(),
                parent_.allow_vcd_target()
        );

        if (deltaWindowHeader == null) {
            return header_parser.GetResult();
        }

        this.source_segment_length_.set(deltaWindowHeader.source_segment_length);

        has_checksum_ = parent_.AllowChecksum() && ((deltaWindowHeader.win_indicator & VCD_CHECKSUM) != 0);
        if ((target_window_length_ = header_parser.ParseWindowLengths()) == null) {
            return header_parser.GetResult();
        }
        if (parent_.TargetWindowWouldExceedSizeLimits(target_window_length_)) {
            // An error has been logged by TargetWindowWouldExceedSizeLimits().
            return RESULT_ERROR;
        }
        header_parser.ParseDeltaIndicator();
        int setup_return_code = SetUpWindowSections(header_parser);
        if (RESULT_SUCCESS != setup_return_code) {
            return setup_return_code;
        }
        /*
        // Reserve enough space in the output string for the current target window.
        final int wanted_capacity = target_window_start_pos_ + target_window_length_;
        if (decoded_target.capacity() < wanted_capacity) {
            decoded_target.reserve(wanted_capacity);
        }
        */

        // Get a pointer to the start of the source segment.
        if ((deltaWindowHeader.win_indicator & VCD_SOURCE) != 0) {
            source_segment_ptr_ = ByteBuffer.wrap(parent_.dictionary_ptr());
            source_segment_ptr_.position(deltaWindowHeader.source_segment_position);
        } else if ((deltaWindowHeader.win_indicator & VCD_TARGET) != 0) {
            // This assignment must happen after the reserve().
            // decoded_target should not be resized again while processing this window,
            // so source_segment_ptr_ should remain valid.
            source_segment_ptr_ = decoded_target.toByteBuffer();
            source_segment_ptr_.position(deltaWindowHeader.source_segment_position);
        }
        // The whole window header was found and parsed successfully.
        found_header_ = true;

        parseable_chunk.position(parseable_chunk.position() + header_parser.unparsedData().position());
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
        VCDiffHeaderParser.SectionLengths sectionLengths = header_parser.ParseSectionLengths(has_checksum_);
        if (sectionLengths == null) {
            return header_parser.GetResult();
        }

        // KLUDGE: this code knows what the structure of the data looks like
        int parsed_delta_encoding_length =
                VarInt.calculateIntLength(target_window_length_)
                        + 1
                        + VarInt.calculateIntLength(sectionLengths.add_and_run_data_length)
                        + VarInt.calculateIntLength(sectionLengths.addresses_length)
                        + VarInt.calculateIntLength(sectionLengths.instructions_and_sizes_length)
                        + sectionLengths.add_and_run_data_length
                        + sectionLengths.addresses_length
                        + sectionLengths.instructions_and_sizes_length;

        if (has_checksum_) {
            this.expected_checksum_.set(sectionLengths.checksum);
            parsed_delta_encoding_length += VarInt.calculateIntLength(sectionLengths.checksum);
        }

        if (parent_.AllowInterleaved() &&
                (sectionLengths.add_and_run_data_length == 0) &&
                (sectionLengths.addresses_length == 0)) {
            // The interleaved format is being used.
            interleaved_bytes_expected_ = sectionLengths.instructions_and_sizes_length;
            UpdateInterleavedSectionPointers(header_parser.unparsedData());
        } else {
            // If interleaved format is not used, then the whole window contents
            // must be available before decoding can begin.  If only part of
            // the current window is available, then report end of data
            // and re-parse the whole header when DecodeChunk() is called again.
            if (header_parser.unparsedData().remaining() < (sectionLengths.add_and_run_data_length +
                    sectionLengths.instructions_and_sizes_length +
                    sectionLengths.addresses_length)) {
                return RESULT_END_OF_DATA;
            }

            data_for_add_and_run_ = header_parser.unparsedData().slice();
            data_for_add_and_run_.position(sectionLengths.add_and_run_data_length);

            instructions_and_sizes_ = data_for_add_and_run_.slice();
            instructions_and_sizes_.position(sectionLengths.instructions_and_sizes_length);

            addresses_for_copy_ = instructions_and_sizes_.slice();
            addresses_for_copy_.position(sectionLengths.addresses_length);

            data_for_add_and_run_.flip();
            instructions_and_sizes_.flip();
            addresses_for_copy_.flip();

            if (header_parser.delta_encoding_length_ != parsed_delta_encoding_length) {
                LOGGER.error("The end of the instructions section does not match the end of the delta window");
                return RESULT_ERROR;
            }
        }

        reader_.Init(instructions_and_sizes_);
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
    private int DecodeBody(ByteBuffer parseable_chunk) {
        // TODO: this was originally pointer comparison between instructions_and_sizes_ and parseable_chunk
        if (IsInterleaved() && false) {
            LOGGER.error("Internal error: interleaved format is used, but the input pointer does not point to the instructions section");
            return RESULT_ERROR;
        }
        while (TargetBytesDecoded() < target_window_length_) {
            final AtomicInteger decoded_size = new AtomicInteger(VCD_INSTRUCTION_ERROR);
            final AtomicInteger mode = new AtomicInteger(0);
            int instruction = reader_.GetNextInstruction(decoded_size, mode);
            switch (instruction) {
            case VCD_INSTRUCTION_END_OF_DATA:
                UpdateInstructionPointer(parseable_chunk);
                return RESULT_END_OF_DATA;
            case VCD_INSTRUCTION_ERROR:
                return RESULT_ERROR;
            default:
                break;
            }
            final int size = decoded_size.get();
            // The value of "size" itself could be enormous (say, INT32_MAX)
            // so check it individually against the limit to protect against
            // overflow when adding it to something else.
            if ((size > target_window_length_) ||
                    ((size + TargetBytesDecoded()) > target_window_length_)) {
                LOGGER.error("{} with size {} plus existing {} bytes of target data exceeds length of target window ({} bytes)",
                        VCDiffCodeTableData.VCDiffInstructionName(instruction), size, TargetBytesDecoded(), target_window_length_);
                return RESULT_ERROR;
            }
            int result = RESULT_SUCCESS;
            switch (instruction) {
            case VCD_ADD:
                result = DecodeAdd(size);
                break;
            case VCD_RUN:
                result = DecodeRun(size);
                break;
            case VCD_COPY:
                result = DecodeCopy(size, (short)mode.get());
                break;
            default:
                LOGGER.error("Unexpected instruction type {} in opcode stream", instruction);
                return RESULT_ERROR;
            }
            switch (result) {
            case RESULT_END_OF_DATA:
                reader_.UnGetInstruction();
                UpdateInstructionPointer(parseable_chunk);
                return RESULT_END_OF_DATA;
            case RESULT_ERROR:
                return RESULT_ERROR;
            case RESULT_SUCCESS:
                break;
            }
        }
        if (TargetBytesDecoded() != target_window_length_) {
            LOGGER.error("Decoded target window size ({}bytes) does not match expected size ({} bytes)",
                    TargetBytesDecoded(), target_window_length_);
            return RESULT_ERROR;
        }

        if (has_checksum_) {
            adler32.update(parent_.decoded_target().getBuffer(), target_window_start_pos_, target_window_length_);
            int checksum = (int)adler32.getValue();
            adler32.reset();

            if (checksum != expected_checksum_.get()) {
                LOGGER.error("Target data does not match checksum; this could mean that the wrong dictionary was used");
                return RESULT_ERROR;
            }
        }
        if (instructions_and_sizes_.hasRemaining()) {
            LOGGER.error("Excess instructions and sizes left over after decoding target window");
            return RESULT_ERROR;
        }
        if (!IsInterleaved()) {
            // Standard format is being used, with three separate sections for the
            // instructions, data, and addresses.
            if (data_for_add_and_run_.hasRemaining()) {
                LOGGER.error("Excess ADD/RUN data left over after decoding target window");
                return RESULT_ERROR;
            }
            if (addresses_for_copy_.hasRemaining()) {
                LOGGER.error("Excess COPY addresses left over after decoding target window");
                return RESULT_ERROR;
            }
            // Reached the end of the window.  Update the ParseableChunk to point to the
            // end of the addresses section, which is the last section in the window.

            parseable_chunk.position(
                    parseable_chunk.position() +
                            instructions_and_sizes_.limit() +
                            data_for_add_and_run_.limit() +
                            addresses_for_copy_.limit());
        } else {
            // Interleaved format is being used.
            UpdateInstructionPointer(parseable_chunk);
        }
        return RESULT_SUCCESS;
    }

    // Returns the number of bytes already decoded into the target window.
    private int TargetBytesDecoded() {
        return parent_.decoded_target().size() - target_window_start_pos_;
    }

    // Decodes a single ADD instruction, updating parent_->decoded_target_.
    private int DecodeAdd(int size) {
        if (size > data_for_add_and_run_.remaining()) {
            return RESULT_END_OF_DATA;
        }
        // Write the next "size" data bytes
        CopyBytes(data_for_add_and_run_, size);
        return RESULT_SUCCESS;
    }

    // Decodes a single RUN instruction, updating parent_->decoded_target_.
    private int DecodeRun(int size) {
        if (!data_for_add_and_run_.hasRemaining()) {
            return RESULT_END_OF_DATA;
        }
        // Write "size" copies of the next data byte
        RunByte(data_for_add_and_run_.get(), size);
        return RESULT_SUCCESS;
    }

    // Decodes a single COPY instruction, updating parent_->decoded_target_.
    private int DecodeCopy(int size, short mode) {
        // Keep track of the number of target bytes decoded as a local variable
        // to avoid recalculating it each time it is needed.
        int target_bytes_decoded = TargetBytesDecoded();
        final int here_address = source_segment_length_.get() + target_bytes_decoded;
        final int decoded_address = parent_.addr_cache().DecodeAddress(
                here_address,
                mode,
                addresses_for_copy_);
        switch (decoded_address) {
        case RESULT_ERROR:
            LOGGER.error("Unable to decode address for COPY");
            return RESULT_ERROR;
        case RESULT_END_OF_DATA:
            return RESULT_END_OF_DATA;
        default:
            if ((decoded_address < 0) || (decoded_address > here_address)) {
                LOGGER.error("Internal error: unexpected address {} returned from DecodeAddress, with here_address = {}",
                        decoded_address, here_address);
                return RESULT_ERROR;
            }
            break;
        }

        // TODO: source_segment_length should be source_segment_ptr_.remaining()
        int address = decoded_address;
        if ((address + size) <= source_segment_length_.get()) {
            // Copy all data from source segment
            CopyBytes((ByteBuffer) source_segment_ptr_.slice().position(address), size);
            return RESULT_SUCCESS;
        }
        // Copy some data from target window...
        if (address < source_segment_length_.get()) {
            // ... plus some data from source segment
            final int partial_copy_size = source_segment_length_.get() - address;
            CopyBytes((ByteBuffer) source_segment_ptr_.slice().position(address), partial_copy_size);
            target_bytes_decoded += partial_copy_size;
            address += partial_copy_size;
            size -= partial_copy_size;
        }
        address -= source_segment_length_.get();
        // address is now based at start of target window
        // const char* const target_segment_ptr = parent_.decoded_target().data() + target_window_start_pos_;

        ByteBuffer target_segment = parent_.decoded_target().toByteBuffer();
        target_segment.position(target_window_start_pos_);

        while (size > (target_bytes_decoded - address)) {
            // Recursive copy that extends into the yet-to-be-copied target data
            final int partial_copy_size = target_bytes_decoded - address;
            // TODO: does target_segment need to be recreated each time?
            CopyBytes((ByteBuffer) target_segment.slice().position(address), partial_copy_size);
            target_bytes_decoded += partial_copy_size;
            address += partial_copy_size;
            size -= partial_copy_size;

            // This might not be needed, but refresh target_segment since bytes were just copied into target
            target_segment = parent_.decoded_target().toByteBuffer();
            target_segment.position(target_window_start_pos_);
        }
        CopyBytes((ByteBuffer) target_segment.slice().position(address), size);
        return RESULT_SUCCESS;
    }

    // When using the interleaved format, this function is called both on parsing
    // the header and on resuming after a RESULT_END_OF_DATA was returned from a
    // previous call to DecodeBody().  It sets up all three section pointers to
    // reference the same interleaved stream of instructions, sizes, addresses,
    // and data.  These pointers must be reset every time that work resumes on a
    // delta window,  because the input data string may have been changed or
    // resized since DecodeBody() last returned.
    private void UpdateInterleavedSectionPointers(ByteBuffer data) {
        instructions_and_sizes_ = data.slice();

        // Don't read past the end of currently-available data
        if (instructions_and_sizes_.remaining() > interleaved_bytes_expected_) {
            instructions_and_sizes_.limit(interleaved_bytes_expected_);
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
        return addresses_for_copy_ == instructions_and_sizes_ && data_for_add_and_run_ == instructions_and_sizes_;
    }

    // Executes a single COPY or ADD instruction, appending data to
    // parent_->decoded_target().
    private void CopyBytes(ByteBuffer buffer, int size) {
        // TODO: optimize
        while (size-- > 0) {
            parent_.decoded_target().write(buffer.get());
        }
    }

    // Executes a single RUN instruction, appending data to
    // parent_->decoded_target().
    private void RunByte(byte b, int size) {
        for (int i = 0; i < size; i++) {
            parent_.decoded_target().write(b);
        }
    }

    // Advance *parseable_chunk to point to the current position in the
    // instructions/sizes section.  If interleaved format is used, then
    // decrement the number of expected bytes in the instructions/sizes section
    // by the number of instruction/size bytes parsed.
    private void UpdateInstructionPointer(ByteBuffer parseable_chunk) {
        if (IsInterleaved()) {
            int bytes_parsed = instructions_and_sizes_.position();
            // Reduce expected instruction segment length by bytes parsed
            interleaved_bytes_expected_ -= bytes_parsed;
            parseable_chunk.position(parseable_chunk.position() + bytes_parsed);
        }
    }

    // The parent object which was passed to Init().
    private final VCDiffStreamingDecoderImpl parent_;

    // This value will be true if VCDiffDeltaFileWindow::ReadDeltaWindowHeader()
    // has been called and succeeded in parsing the delta window header, but the
    // entire window has not yet been decoded.
    private boolean found_header_;

    // Contents and length of the current source window.  source_segment_ptr_
    // will be non-NULL if (a) the window section header for the current window
    // has been read, but the window has not yet finished decoding; or
    // (b) the window did not specify a source segment.
    //private const char* source_segment_ptr_;
    private ByteBuffer source_segment_ptr_;
    private final AtomicInteger source_segment_length_ = new AtomicInteger(0);

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
    private Integer target_window_length_;

    // The index in decoded_target at which the first byte of the current
    // target window was/will be written.
    private int target_window_start_pos_;

    // If has_checksum_ is true, then expected_checksum_ contains an Adler32
    // checksum of the target window data.  This is an extension included in the
    // VCDIFF 'S' (SDCH) format, but is not part of the RFC 3284 draft standard.
    private boolean has_checksum_;
    private final AtomicInteger expected_checksum_ = new AtomicInteger(0);

    private final Adler32 adler32 = new ZeroInitializedAdler32();

    private VCDiffCodeTableReader reader_ = new VCDiffCodeTableReader();
}
