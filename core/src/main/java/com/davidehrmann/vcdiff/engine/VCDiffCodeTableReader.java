package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.util.Objects;
import com.davidehrmann.vcdiff.util.VarInt;
import com.davidehrmann.vcdiff.util.VarInt.VarIntEndOfBufferException;
import com.davidehrmann.vcdiff.util.VarInt.VarIntParseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class VCDiffCodeTableReader {

    protected static final short NO_OPCODE = 0x100; // outside the opcode range 0x00 - 0xFF

    // A pointer to the code table.  This is the object that will be used
    // to interpret opcodes in getNextInstruction().
    private final VCDiffCodeTableData codeTableData;

    private ByteBuffer instructionsAndSizes;

    private int lastInstructionStart = -1;

    private short pendingSecondInstruction;
    private short lastPendingSecondInstruction;

    public VCDiffCodeTableReader() {
        this.codeTableData = VCDiffCodeTableData.kDefaultCodeTableData;
    }

    // Sets up a non-standard code table.  The caller
    // may free the memory occupied by the argument code table after
    // passing it to this method, because the argument code table
    // allocates space to store a copy of it.
    // useCodeTable() may be called either before or after calling init().
    // Returns true if the code table was accepted, or false if the
    // argument did not appear to be a valid code table.
    public VCDiffCodeTableReader(VCDiffCodeTableData codeTableData, short maxMode) {
        if (!codeTableData.Validate(maxMode)) {
            throw new IllegalArgumentException("Invalid code table data.");
        }
        this.codeTableData = Objects.requireNotNull(codeTableData, "codeTableData was null");
    }

    // Defines the buffer containing the instructions and sizes.
    // This method must be called before getNextInstruction() may be used.
    // init() may be called any number of times to reset the state of
    // the object.
    //
    public void init(ByteBuffer instructions_and_sizes) {
        instructionsAndSizes = instructions_and_sizes;
        lastInstructionStart = -1;
        pendingSecondInstruction = NO_OPCODE;
        lastPendingSecondInstruction = NO_OPCODE;
    }

    // Returns the next instruction from the stream of opcodes,
    // or VCD_INSTRUCTION_END_OF_DATA if the end of the opcode stream is reached,
    // or VCD_INSTRUCTION_ERROR if an error occurred.
    // In the first of these cases, increments *instructionsAndSizes
    // past the values it reads, and populates *size
    // with the corresponding size for the returned instruction;
    // otherwise, the value of *size is undefined, and is not
    // guaranteed to be preserved.
    // If the instruction returned is VCD_COPY, *mode will
    // be populated with the copy mode; otherwise, the value of *mode
    // is undefined, and is not guaranteed to be preserved.
    // Any occurrences of VCD_NOOP in the opcode stream
    // are skipped over and ignored, not returned.
    // If init() was not called before calling this method, then
    // VCD_INSTRUCTION_ERROR will be returned.
    public byte getNextInstruction(AtomicInteger size, AtomicInteger mode) throws IOException {
        if (instructionsAndSizes == null) {
            throw new IllegalStateException("Internal error: getNextInstruction() called before init()");
        }

        lastInstructionStart = instructionsAndSizes.position();
        lastPendingSecondInstruction = pendingSecondInstruction;
        byte opcode;
        byte instruction_type;
        int instruction_size;
        byte instruction_mode;

        do {
            if (pendingSecondInstruction != NO_OPCODE) {
                // There is a second instruction left over
                // from the most recently processed opcode.
                opcode = (byte) pendingSecondInstruction;
                pendingSecondInstruction = NO_OPCODE;
                instruction_type = codeTableData.inst2[opcode & 0xff];
                instruction_size = codeTableData.size2[opcode & 0xff];
                instruction_mode = codeTableData.mode2[opcode & 0xff];
                break;
            }
            if (!instructionsAndSizes.hasRemaining()) {
                // Ran off end of instruction stream
                return VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA;
            }

            opcode = instructionsAndSizes.get();
            if (codeTableData.inst2[opcode & 0xff] != VCDiffCodeTableData.VCD_NOOP) {
                // This opcode contains two instructions; process the first one now, and
                // save a pointer to the second instruction, which should be returned
                // by the next call to getNextInstruction
                pendingSecondInstruction = opcode;
            }

            instruction_type = codeTableData.inst1[opcode & 0xff];
            instruction_size = codeTableData.size1[opcode & 0xff];
            instruction_mode = codeTableData.mode1[opcode & 0xff];
            // This do-while loop is necessary in case inst1 == VCD_NOOP for an opcode
            // that was actually used in the encoding.  That case is unusual, but it
            // is not prohibited by the standard.
        } while (instruction_type == VCDiffCodeTableData.VCD_NOOP);
        if (instruction_size == 0) {
            // Parse the size as a Varint in the instruction stream.
            try {
                size.set(VarInt.getInt(instructionsAndSizes));
            } catch (VarIntParseException e) {
                throw new IOException("Instruction size is not a valid variable-length integer");
            } catch (VarIntEndOfBufferException e) {
                unGetInstruction();  // Rewind to instruction start
                return VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA;
            }
        } else {
            size.set(instruction_size);
        }
        mode.set(instruction_mode);

        return instruction_type;
    }

    // Puts a single instruction back onto the front of the
    // instruction stream.  The next call to getNextInstruction()
    // will return the same value that was returned by the last
    // call.  Calling unGetInstruction() more than once before calling
    // getNextInstruction() will have no additional effect; you can
    // only rewind one instruction.
    public void unGetInstruction() {
        if (lastInstructionStart >= 0) {
            if (lastInstructionStart > instructionsAndSizes.position()) {
                throw new IllegalStateException("Internal error: last_instruction_start past end of instructions_and_sizes in unGetInstruction");
            }
            instructionsAndSizes.position(lastInstructionStart);
            if ((pendingSecondInstruction != NO_OPCODE) &&
                    (lastPendingSecondInstruction != NO_OPCODE)) {
                throw new IllegalStateException("Internal error: two pending instructions in a row in unGetInstruction");
            }
            pendingSecondInstruction = lastPendingSecondInstruction;
        }
    }

    // Updates the pointers to the buffer containing the instructions and sizes,
    // but leaves the rest of the reader state intact, so that (for example)
    // any pending second instruction or unread instruction will still be
    // read when requested.  NOTE: unGetInstruction() will not work immediately
    // after using updatePointers(); getNextInstruction() must be called first.
    void updatePointers(ByteBuffer instructions_and_sizes) {
        instructionsAndSizes = instructions_and_sizes;
        lastInstructionStart = -1;
        // pendingSecondInstruction is unchanged
        lastPendingSecondInstruction = pendingSecondInstruction;
    }
}
