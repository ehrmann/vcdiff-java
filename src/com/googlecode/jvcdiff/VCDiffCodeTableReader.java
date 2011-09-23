package com.googlecode.jvcdiff;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.googlecode.jvcdiff.VarInt.VarIntEndOfBufferException;
import com.googlecode.jvcdiff.VarInt.VarIntParseException;

public class VCDiffCodeTableReader {
	// A pointer to the code table.  This is the object that will be used
	// to interpret opcodes in GetNextInstruction().
	protected final VCDiffCodeTableData code_table_data_;

	protected final short kNoOpcode = 0x100; // outside the opcode range 0x00 - 0xFF

	protected ByteBuffer instructions_and_sizes_;

	int last_instruction_start_ = -1;

	protected short pending_second_instruction_;
	protected short last_pending_second_instruction_;

	// Puts a single instruction back onto the front of the
	// instruction stream.  The next call to GetNextInstruction()
	// will return the same value that was returned by the last
	// call.  Calling UnGetInstruction() more than once before calling
	// GetNextInstruction() will have no additional effect; you can
	// only rewind one instruction.
	public void UnGetInstruction() {
		if (last_instruction_start_ >= 0) {
			if (last_instruction_start_ > instructions_and_sizes_.position()) {
				// VCD_DFATAL << "Internal error: last_instruction_start past end of "
				//             "instructions_and_sizes in UnGetInstruction" << VCD_ENDL;
			}
			instructions_and_sizes_.position(last_instruction_start_);
			if ((pending_second_instruction_ != kNoOpcode) &&
					(last_pending_second_instruction_ != kNoOpcode)) {
				//VCD_DFATAL << "Internal error: two pending instructions in a row "
				//             "in UnGetInstruction" << VCD_ENDL;
			}
			pending_second_instruction_ = last_pending_second_instruction_;
		}
	}


	public VCDiffCodeTableReader() {
		this.code_table_data_ = VCDiffCodeTableData.kDefaultCodeTableData;
	}

	// Sets up a non-standard code table.  The caller
	// may free the memory occupied by the argument code table after
	// passing it to this method, because the argument code table
	// allocates space to store a copy of it.
	// UseCodeTable() may be called either before or after calling Init().
	// Returns true if the code table was accepted, or false if the
	// argument did not appear to be a valid code table.
	public VCDiffCodeTableReader(VCDiffCodeTableData codeTableData, short maxMode) {
		if (codeTableData == null) {
			throw new NullPointerException();
		}

		if (!codeTableData.Validate(maxMode)) {
			throw new IllegalArgumentException("Invalid code table data.");
		}

		this.code_table_data_ = codeTableData;
	}

	// Defines the buffer containing the instructions and sizes.
	// This method must be called before GetNextInstruction() may be used.
	// Init() may be called any number of times to reset the state of
	// the object.
	//
	void Init(ByteBuffer instructions_and_sizes) {
		instructions_and_sizes_ = instructions_and_sizes;
		last_instruction_start_ = -1;
		pending_second_instruction_ = kNoOpcode;
		last_pending_second_instruction_ = kNoOpcode;
	}

	// Updates the pointers to the buffer containing the instructions and sizes,
	// but leaves the rest of the reader state intact, so that (for example)
	// any pending second instruction or unread instruction will still be
	// read when requested.  NOTE: UnGetInstruction() will not work immediately
	// after using UpdatePointers(); GetNextInstruction() must be called first.
	void UpdatePointers(ByteBuffer instructions_and_sizes) {
		instructions_and_sizes_ = instructions_and_sizes;
		last_instruction_start_ = -1;
		// pending_second_instruction_ is unchanged
		last_pending_second_instruction_ = pending_second_instruction_;
	}

	// Returns the next instruction from the stream of opcodes,
	// or VCD_INSTRUCTION_END_OF_DATA if the end of the opcode stream is reached,
	// or VCD_INSTRUCTION_ERROR if an error occurred.
	// In the first of these cases, increments *instructions_and_sizes_
	// past the values it reads, and populates *size
	// with the corresponding size for the returned instruction;
	// otherwise, the value of *size is undefined, and is not
	// guaranteed to be preserved.
	// If the instruction returned is VCD_COPY, *mode will
	// be populated with the copy mode; otherwise, the value of *mode
	// is undefined, and is not guaranteed to be preserved.
	// Any occurrences of VCD_NOOP in the opcode stream
	// are skipped over and ignored, not returned.
	// If Init() was not called before calling this method, then
	// VCD_INSTRUCTION_ERROR will be returned.
	public byte GetNextInstruction(AtomicInteger size, AtomicInteger mode) {
		if (instructions_and_sizes_ == null) {
			// VCD_ERROR << "Internal error: GetNextInstruction() called before Init()"
			return VCDiffCodeTableData.VCD_INSTRUCTION_ERROR;
		}

		last_instruction_start_ = instructions_and_sizes_.position();
		last_pending_second_instruction_ = pending_second_instruction_;
		byte opcode = 0;
		byte instruction_type = VCDiffCodeTableData.VCD_NOOP;
		int instruction_size = 0;
		byte instruction_mode = 0;

		do {
			if (pending_second_instruction_ != kNoOpcode) {
				// There is a second instruction left over
				// from the most recently processed opcode.
				opcode = (byte)pending_second_instruction_;
				pending_second_instruction_ = kNoOpcode;
				instruction_type = code_table_data_.inst2[opcode];
				instruction_size = code_table_data_.size2[opcode];
				instruction_mode = code_table_data_.mode2[opcode];
				break;
			}
			if (!instructions_and_sizes_.hasRemaining()) {
				// Ran off end of instruction stream
				return VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA;
			}

			opcode = instructions_and_sizes_.get();
			if (code_table_data_.inst2[opcode] != VCDiffCodeTableData.VCD_NOOP) {
				// This opcode contains two instructions; process the first one now, and
				// save a pointer to the second instruction, which should be returned
				// by the next call to GetNextInstruction
				pending_second_instruction_ = opcode;
			}

			instruction_type = code_table_data_.inst1[opcode];
			instruction_size = code_table_data_.size1[opcode];
			instruction_mode = code_table_data_.mode1[opcode];
			// This do-while loop is necessary in case inst1 == VCD_NOOP for an opcode
			// that was actually used in the encoding.  That case is unusual, but it
			// is not prohibited by the standard.
		} while (instruction_type == VCDiffCodeTableData.VCD_NOOP);
		if (instruction_size == 0) {
			// Parse the size as a Varint in the instruction stream.
			try {
				size.set(VarInt.getInt(instructions_and_sizes_));
			} catch (VarIntParseException e) {
				//VCD_ERROR << "Instruction size is not a valid variable-length integer"
				//         << VCD_ENDL;
				return VCDiffCodeTableData.VCD_INSTRUCTION_ERROR;
			} catch (VarIntEndOfBufferException e) {
				UnGetInstruction();  // Rewind to instruction start
				return VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA;
			}
		} else {
			size.set(instruction_size);
		}
		mode.set(instruction_mode);

		return instruction_type;
	}
}
