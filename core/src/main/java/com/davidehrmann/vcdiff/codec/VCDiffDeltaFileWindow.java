package com.davidehrmann.vcdiff.codec;

import com.davidehrmann.vcdiff.VCDiffCodeTableData;
import com.davidehrmann.vcdiff.VCDiffCodeTableReader;
import com.davidehrmann.vcdiff.VarInt;
import com.davidehrmann.vcdiff.ZeroInitializedAdler32;
import com.davidehrmann.vcdiff.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Adler32;

import static com.davidehrmann.vcdiff.VCDiffCodeTableData.*;
import static com.davidehrmann.vcdiff.VCDiffCodeTableWriter.*;
import static com.davidehrmann.vcdiff.codec.VCDiffHeaderParser.RESULT_END_OF_DATA;
import static com.davidehrmann.vcdiff.codec.VCDiffHeaderParser.RESULT_SUCCESS;

@SuppressWarnings("UnnecessaryInitCause")
public class VCDiffDeltaFileWindow {
    private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffDeltaFileWindow.class);

    public VCDiffDeltaFileWindow(VCDiffStreamingDecoderImpl parent) {
        this.parent = Objects.requireNotNull(parent, "parent was null");
        Reset();
    }

    // Resets the pointers to the data sections in the current window.
    public void Reset() {
        foundHeader = false;

        // Mark the start of the current target window.
        targetWindowStartPos = (parent != null) ? parent.decodedTarget().size() : 0;
        targetWindowLength = 0;

        sourceSegment = null;
        sourceSegmentLength.set(0);

        instructionsAndSizes = null;
        dataForAddAndRun = null;
        addressesForCopy = null;

        interleavedBytesExpected = 0;

        hasChecksum = false;
        expectedChecksum.set(0);
    }

    public void useCodeTable(VCDiffCodeTableData code_table_data, short max_mode) {
        reader = new VCDiffCodeTableReader(code_table_data, max_mode);
    }

    // Decodes a single delta window using the input data from *parseable_chunk.
    // Appends the decoded target window to parent->decodedTarget().  Returns
    // RESULT_SUCCESS if an entire window was decoded, or RESULT_END_OF_DATA if
    // the end of input was reached before the entire window could be decoded and
    // more input is expected (only possible if isInterleaved() is true), or
    // RESULT_ERROR if an error occurred during decoding.  In the RESULT_ERROR
    // case, the value of parseable_chunk->pointer_ is undefined; otherwise,
    // parseable_chunk->Advance() is called to point to the input data position
    // just after the data that has been decoded.
    //
    public int DecodeWindow(ByteBuffer parseable_chunk) throws IOException {
        if (!foundHeader) {
            if (readHeader(parseable_chunk) == RESULT_END_OF_DATA) {
                return RESULT_END_OF_DATA;
            }
            // reset address cache between windows (RFC section 5.1)
            parent.addrCache().Init();
        } else {
            // We are resuming a window that was partially decoded before a
            // RESULT_END_OF_DATA was returned.  This can only happen on the first
            // loop iteration, and only if the interleaved format is enabled and used.
            if (!isInterleaved()) {
                throw new IOException("Internal error: Resumed decoding of a delta file window when interleaved format is not being used");
            }
            // FIXME: ?
            updateInterleavedSectionPointers(parseable_chunk);
            reader.UpdatePointers(instructionsAndSizes);
        }
        switch (decodeBody(parseable_chunk)) {
        case RESULT_END_OF_DATA:
            if (moreDataExpected()) {
                return RESULT_END_OF_DATA;
            } else {
                throw new IOException("End of data reached while decoding VCDIFF delta file");
            }
        default:
            break;  // decodeBody succeeded
        }
        // Get ready to read a new delta window
        Reset();
        return RESULT_SUCCESS;
    }

    public boolean FoundWindowHeader() {
        return foundHeader;
    }

    public boolean moreDataExpected() {
        // When parsing an interleaved-format delta file,
        // every time decodeBody() exits, interleavedBytesExpected
        // will be decremented by the number of bytes parsed.  If it
        // reaches zero, then there is no more data expected because
        // the size of the interleaved section (given in the window
        // header) has been reached.
        return isInterleaved() && (interleavedBytesExpected > 0);
    }

    public int targetWindowStartPos() { return targetWindowStartPos; }

    public void setTargetWindowStartPos(int new_start_pos) {
        targetWindowStartPos = new_start_pos;
    }

    // Returns the number of bytes remaining to be decoded in the target window.
    // If not in the process of decoding a window, returns 0.
    public int targetBytesRemaining() {
        if (targetWindowLength == 0) {
            // There is no window being decoded at present
            return 0;
        } else {
            return targetWindowLength - targetBytesDecoded();
        }
    }

    // Reads the header of the window section as described in RFC sections 4.2 and
    // 4.3, up to and including the value "Length of addresses for COPYs".  If the
    // entire header is found, this function sets up the DeltaWindowSections
    // instructionsAndSizes, dataForAddAndRun, and addressesForCopy so
    // that the decoder can begin decoding the opcodes in these sections.  Returns
    // RESULT_ERROR if an error occurred, or RESULT_END_OF_DATA if the end of
    // available data was reached before the entire header could be read.  (The
    // latter may be an error condition if there is no more data available.)
    // Otherwise, returns RESULT_SUCCESS and advances parseableChunk past the
    // parsed header.
    //
    private int readHeader(ByteBuffer parseableChunk) throws IOException {
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
        VCDiffStreamingDecoderImpl.DecoratedByteArrayOutputStream decoded_target = parent.decodedTarget();
        VCDiffHeaderParser header_parser = new VCDiffHeaderParser(parseableChunk.slice());

        VCDiffHeaderParser.DeltaWindowHeader deltaWindowHeader = header_parser.parseWinIndicatorAndSourceSegment(
                parent.dictionarySize(),
                decoded_target.size(),
                parent.allowVcdTarget()
        );

        if (deltaWindowHeader == null) {
            return header_parser.getResult();
        }

        this.sourceSegmentLength.set(deltaWindowHeader.source_segment_length);

        hasChecksum = parent.allowChecksum() && ((deltaWindowHeader.win_indicator & VCD_CHECKSUM) != 0);
        if ((targetWindowLength = header_parser.ParseWindowLengths()) == null) {
            return header_parser.getResult();
        }

        // Throws an exception if targetWindowWouldExceedSizeLimits
        parent.targetWindowWouldExceedSizeLimits(targetWindowLength);

        header_parser.parseDeltaIndicator();
        int setup_return_code = setUpWindowSections(header_parser);
        if (RESULT_SUCCESS != setup_return_code) {
            return setup_return_code;
        }
        /*
        // Reserve enough space in the output string for the current target window.
        final int wanted_capacity = targetWindowStartPos + targetWindowLength;
        if (decodedTarget.capacity() < wanted_capacity) {
            decodedTarget.reserve(wanted_capacity);
        }
        */

        // Get a pointer to the start of the source segment.
        if ((deltaWindowHeader.win_indicator & VCD_SOURCE) != 0) {
            sourceSegment = ByteBuffer.wrap(parent.dictionary_ptr());
            sourceSegment.position(deltaWindowHeader.source_segment_position);
        } else if ((deltaWindowHeader.win_indicator & VCD_TARGET) != 0) {
            // This assignment must happen after the reserve().
            // decodedTarget should not be resized again while processing this window,
            // so sourceSegment should remain valid.
            sourceSegment = decoded_target.toByteBuffer();
            sourceSegment.position(deltaWindowHeader.source_segment_position);
        }
        // The whole window header was found and parsed successfully.
        foundHeader = true;

        parseableChunk.position(parseableChunk.position() + header_parser.unparsedData().position());
        parent.addToTotalTargetWindowSize(targetWindowLength);
        return RESULT_SUCCESS;
    }

    // After the window header has been parsed as far as the Delta_Indicator,
    // this function is called to parse the following delta window header fields:
    //
    //     Length of data for ADDs and RUNs - integer (VarintBE format)
    //     Length of instructions and sizes - integer (VarintBE format)
    //     Length of addresses for COPYs    - integer (VarintBE format)
    //
    // If hasChecksum is true, it also looks for the following element:
    //
    //     Adler32 checksum            - unsigned 32-bit integer (VarintBE format)
    //
    // It sets up the DeltaWindowSections instructionsAndSizes,
    // dataForAddAndRun, and addressesForCopy.  If the interleaved format
    // is being used, all three sections will include the entire window body; if
    // the standard format is used, three non-overlapping window sections will be
    // defined.  Returns RESULT_ERROR if an error occurred, or RESULT_END_OF_DATA
    // if standard format is being used and there is not enough input data to read
    // the entire window body.  Otherwise, returns RESULT_SUCCESS.
    private int setUpWindowSections(VCDiffHeaderParser header_parser) throws IOException {
        VCDiffHeaderParser.SectionLengths sectionLengths = header_parser.parseSectionLengths(hasChecksum);
        if (sectionLengths == null) {
            return header_parser.getResult();
        }

        // KLUDGE: this code knows what the structure of the data looks like
        int parsed_delta_encoding_length =
                VarInt.calculateIntLength(targetWindowLength)
                        + 1
                        + VarInt.calculateIntLength(sectionLengths.add_and_run_data_length)
                        + VarInt.calculateIntLength(sectionLengths.addresses_length)
                        + VarInt.calculateIntLength(sectionLengths.instructions_and_sizes_length)
                        + sectionLengths.add_and_run_data_length
                        + sectionLengths.addresses_length
                        + sectionLengths.instructions_and_sizes_length;

        if (hasChecksum) {
            this.expectedChecksum.set(sectionLengths.checksum);
            parsed_delta_encoding_length += VarInt.calculateIntLength(sectionLengths.checksum);
        }

        if (parent.allowInterleaved() &&
                (sectionLengths.add_and_run_data_length == 0) &&
                (sectionLengths.addresses_length == 0)) {
            // The interleaved format is being used.
            interleavedBytesExpected = sectionLengths.instructions_and_sizes_length;
            updateInterleavedSectionPointers(header_parser.unparsedData());
        } else {
            // If interleaved format is not used, then the whole window contents
            // must be available before decoding can begin.  If only part of
            // the current window is available, then report end of data
            // and re-parse the whole header when decodeChunk() is called again.
            if (header_parser.unparsedData().remaining() < (sectionLengths.add_and_run_data_length +
                    sectionLengths.instructions_and_sizes_length +
                    sectionLengths.addresses_length)) {
                return RESULT_END_OF_DATA;
            }

            dataForAddAndRun = header_parser.unparsedData().slice();
            dataForAddAndRun.position(sectionLengths.add_and_run_data_length);

            instructionsAndSizes = dataForAddAndRun.slice();
            instructionsAndSizes.position(sectionLengths.instructions_and_sizes_length);

            addressesForCopy = instructionsAndSizes.slice();
            addressesForCopy.position(sectionLengths.addresses_length);

            dataForAddAndRun.flip();
            instructionsAndSizes.flip();
            addressesForCopy.flip();

            if (header_parser.deltaEncodingLength != parsed_delta_encoding_length) {
                throw new IOException("The end of the instructions section does not match the end of the delta window");
            }
        }

        reader.init(instructionsAndSizes);
        return RESULT_SUCCESS;
    }

    // Decodes the body of the window section as described in RFC sections 4.3,
    // including the sections "Data section for ADDs and RUNs", "Instructions
    // and sizes section", and "Addresses section for COPYs".  These sections
    // must already have been set up by ReadWindowHeader().  Returns a
    // non-negative value on success, or RESULT_END_OF_DATA if the end of input
    // was reached before the entire window could be decoded (only possible if
    // isInterleaved() is true), or RESULT_ERROR if an error occurred during
    // decoding.  Appends as much of the decoded target window as possible to
    // parent->decodedTarget().
    //
    private int decodeBody(ByteBuffer parseable_chunk) throws IOException {
        // TODO: this was originally pointer comparison between instructionsAndSizes and parseable_chunk
        if (isInterleaved() && false) {
            throw new IllegalStateException("Internal error: interleaved format is used, but the input pointer does not point to the instructions section");
        }
        while (targetBytesDecoded() < targetWindowLength) {
            final AtomicInteger decoded_size = new AtomicInteger(0);
            final AtomicInteger mode = new AtomicInteger(0);
            int instruction = reader.GetNextInstruction(decoded_size, mode);
            switch (instruction) {
            case VCD_INSTRUCTION_END_OF_DATA:
                updateInstructionPointer(parseable_chunk);
                return RESULT_END_OF_DATA;
            default:
                break;
            }
            final int size = decoded_size.get();
            // The value of "size" itself could be enormous (say, INT32_MAX)
            // so check it individually against the limit to protect against
            // overflow when adding it to something else.
            if ((size > targetWindowLength) ||
                    ((size + targetBytesDecoded()) > targetWindowLength)) {
                throw new IOException(String.format(
                        "%s with size %d plus existing %d bytes of target data exceeds length of target window (%d bytes)",
                        VCDiffCodeTableData.VCDiffInstructionName(instruction), size, targetBytesDecoded(), targetWindowLength
                ));
            }
            int result;
            switch (instruction) {
            case VCD_ADD:
                result = decodeAdd(size);
                break;
            case VCD_RUN:
                result = decodeRun(size);
                break;
            case VCD_COPY:
                result = decodeCopy(size, (short)mode.get());
                break;
            default:
                throw new IOException("Unexpected instruction type " + instruction + " in opcode stream");
            }
            switch (result) {
            case RESULT_END_OF_DATA:
                reader.UnGetInstruction();
                updateInstructionPointer(parseable_chunk);
                return RESULT_END_OF_DATA;
            case RESULT_SUCCESS:
                break;
            }
        }
        if (targetBytesDecoded() != targetWindowLength) {
            throw new IOException(String.format(
                    "Decoded target window size (%d bytes) does not match expected size (%d bytes)",
                    targetBytesDecoded(), targetWindowLength
            ));
        }

        if (hasChecksum) {
            adler32.update(parent.decodedTarget().getBuffer(), targetWindowStartPos, targetWindowLength);
            int checksum = (int)adler32.getValue();
            adler32.reset();

            if (checksum != expectedChecksum.get()) {
                throw new IOException("Target data does not match checksum; this could mean that the wrong dictionary was used");
            }
        }
        if (instructionsAndSizes.hasRemaining()) {
            throw new IOException("Excess instructions and sizes left over after decoding target window");
        }
        if (!isInterleaved()) {
            // Standard format is being used, with three separate sections for the
            // instructions, data, and addresses.
            if (dataForAddAndRun.hasRemaining()) {
                throw new IOException("Excess ADD/RUN data left over after decoding target window");
            }
            if (addressesForCopy.hasRemaining()) {
                throw new IOException("Excess COPY addresses left over after decoding target window");
            }
            // Reached the end of the window.  Update the ParseableChunk to point to the
            // end of the addresses section, which is the last section in the window.

            parseable_chunk.position(
                    parseable_chunk.position() +
                            instructionsAndSizes.limit() +
                            dataForAddAndRun.limit() +
                            addressesForCopy.limit());
        } else {
            // Interleaved format is being used.
            updateInstructionPointer(parseable_chunk);
        }
        return RESULT_SUCCESS;
    }

    // Returns the number of bytes already decoded into the target window.
    private int targetBytesDecoded() {
        return parent.decodedTarget().size() - targetWindowStartPos;
    }

    // Decodes a single ADD instruction, updating parent->decoded_target_.
    private int decodeAdd(int size) {
        if (size > dataForAddAndRun.remaining()) {
            return RESULT_END_OF_DATA;
        }
        // Write the next "size" data bytes
        copyBytes(dataForAddAndRun, size);
        return RESULT_SUCCESS;
    }

    // Decodes a single RUN instruction, updating parent->decoded_target_.
    private int decodeRun(int size) {
        if (!dataForAddAndRun.hasRemaining()) {
            return RESULT_END_OF_DATA;
        }
        // Write "size" copies of the next data byte
        runByte(dataForAddAndRun.get(), size);
        return RESULT_SUCCESS;
    }

    // Decodes a single COPY instruction, updating parent->decoded_target_.
    private int decodeCopy(int size, short mode) throws IOException {
        // Keep track of the number of target bytes decoded as a local variable
        // to avoid recalculating it each time it is needed.
        int target_bytes_decoded = targetBytesDecoded();
        final int here_address = sourceSegmentLength.get() + target_bytes_decoded;
        final int decodedAddress;
        try {
            decodedAddress = parent.addrCache().DecodeAddress(
                    here_address,
                    mode,
                    addressesForCopy
            );
        } catch (IOException e) {
            IOException rethrown = new IOException("Unable to decode address for COPY");
            rethrown.initCause(e);
            throw e;
        }

        if (decodedAddress == RESULT_END_OF_DATA) {
            return RESULT_END_OF_DATA;
        }

        if ((decodedAddress < 0) || (decodedAddress > here_address)) {
            throw new IllegalStateException(String.format(
                    "Internal error: unexpected address %d returned from DecodeAddress, with here_address = %d",
                    decodedAddress, here_address
            ));
        }

        // TODO: source_segment_length should be sourceSegment.remaining()
        int address = decodedAddress;
        if ((address + size) <= sourceSegmentLength.get()) {
            // copy all data from source segment
            copyBytes((ByteBuffer) sourceSegment.slice().position(address), size);
            return RESULT_SUCCESS;
        }
        // copy some data from target window...
        if (address < sourceSegmentLength.get()) {
            // ... plus some data from source segment
            final int partial_copy_size = sourceSegmentLength.get() - address;
            copyBytes((ByteBuffer) sourceSegment.slice().position(address), partial_copy_size);
            target_bytes_decoded += partial_copy_size;
            address += partial_copy_size;
            size -= partial_copy_size;
        }
        address -= sourceSegmentLength.get();
        // address is now based at start of target window
        // const char* const target_segment_ptr = parent.decodedTarget().data() + targetWindowStartPos;

        ByteBuffer targetSegment = parent.decodedTarget().toByteBuffer();
        targetSegment.position(targetWindowStartPos);

        while (size > (target_bytes_decoded - address)) {
            // Recursive copy that extends into the yet-to-be-copied target data
            final int partial_copy_size = target_bytes_decoded - address;
            // TODO: does targetSegment need to be recreated each time?
            copyBytes((ByteBuffer) targetSegment.slice().position(address), partial_copy_size);
            target_bytes_decoded += partial_copy_size;
            address += partial_copy_size;
            size -= partial_copy_size;

            // This might not be needed, but refresh targetSegment since bytes were just copied into target
            targetSegment = parent.decodedTarget().toByteBuffer();
            targetSegment.position(targetWindowStartPos);
        }
        copyBytes((ByteBuffer) targetSegment.slice().position(address), size);
        return RESULT_SUCCESS;
    }

    // When using the interleaved format, this function is called both on parsing
    // the header and on resuming after a RESULT_END_OF_DATA was returned from a
    // previous call to decodeBody().  It sets up all three section pointers to
    // reference the same interleaved stream of instructions, sizes, addresses,
    // and data.  These pointers must be reset every time that work resumes on a
    // delta window,  because the input data string may have been changed or
    // resized since decodeBody() last returned.
    private void updateInterleavedSectionPointers(ByteBuffer data) {
        instructionsAndSizes = data.slice();

        // Don't read past the end of currently-available data
        if (instructionsAndSizes.remaining() > interleavedBytesExpected) {
            instructionsAndSizes.limit(interleavedBytesExpected);
        }

        dataForAddAndRun = instructionsAndSizes;
        addressesForCopy = instructionsAndSizes;
    }

    // If true, the interleaved format described in allowInterleaved() is used
    // for the current delta file.  Only valid after ReadWindowHeader() has been
    // called and returned a positive number (i.e., the whole header was parsed),
    // but before the window has finished decoding.
    //
    private boolean isInterleaved() {
        // If the sections are interleaved, both addressesForCopy and
        // dataForAddAndRun should point at instructionsAndSizes.
        return addressesForCopy == instructionsAndSizes && dataForAddAndRun == instructionsAndSizes;
    }

    // Executes a single COPY or ADD instruction, appending data to
    // parent->decodedTarget().
    private void copyBytes(ByteBuffer buffer, int size) {
        // TODO: optimize
        while (size-- > 0) {
            parent.decodedTarget().write(buffer.get());
        }
    }

    // Executes a single RUN instruction, appending data to
    // parent->decodedTarget().
    private void runByte(byte b, int size) {
        for (int i = 0; i < size; i++) {
            parent.decodedTarget().write(b);
        }
    }

    // Advance *parseableChunk to point to the current position in the
    // instructions/sizes section.  If interleaved format is used, then
    // decrement the number of expected bytes in the instructions/sizes section
    // by the number of instruction/size bytes parsed.
    private void updateInstructionPointer(ByteBuffer parseableChunk) {
        if (isInterleaved()) {
            int bytes_parsed = instructionsAndSizes.position();
            // Reduce expected instruction segment length by bytes parsed
            interleavedBytesExpected -= bytes_parsed;
            parseableChunk.position(parseableChunk.position() + bytes_parsed);
        }
    }

    // The parent object which was passed to init().
    private final VCDiffStreamingDecoderImpl parent;

    // This value will be true if VCDiffDeltaFileWindow::ReadDeltaWindowHeader()
    // has been called and succeeded in parsing the delta window header, but the
    // entire window has not yet been decoded.
    private boolean foundHeader;

    // Contents and length of the current source window.  sourceSegment
    // will be non-NULL if (a) the window section header for the current window
    // has been read, but the window has not yet finished decoding; or
    // (b) the window did not specify a source segment.
    private ByteBuffer sourceSegment;
    private final AtomicInteger sourceSegmentLength = new AtomicInteger(0);

    // The delta encoding window sections as defined in RFC section 4.3.
    // The pointer for each section will be incremented as data is consumed and
    // decoded from that section.  If the interleaved format is used,
    // dataForAddAndRun and addressesForCopy will both point to
    // instructionsAndSizes; otherwise, they will be separate data sections.
    private ByteBuffer instructionsAndSizes;
    private ByteBuffer dataForAddAndRun;
    private ByteBuffer addressesForCopy;

    // The expected bytes left to decode in instructionsAndSizes.  Only used
    // for the interleaved format.
    private int interleavedBytesExpected;

    // The expected length of the target window once it has been decoded.
    private Integer targetWindowLength;

    // The index in decodedTarget at which the first byte of the current
    // target window was/will be written.
    private int targetWindowStartPos;

    // If hasChecksum is true, then expectedChecksum contains an Adler32
    // checksum of the target window data.  This is an extension included in the
    // VCDIFF 'S' (SDCH) format, but is not part of the RFC 3284 draft standard.
    private boolean hasChecksum;
    private final AtomicInteger expectedChecksum = new AtomicInteger(0);

    private final Adler32 adler32 = new ZeroInitializedAdler32();

    private VCDiffCodeTableReader reader = new VCDiffCodeTableReader();
}
