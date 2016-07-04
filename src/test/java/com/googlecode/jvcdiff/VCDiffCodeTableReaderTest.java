package com.googlecode.jvcdiff;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

public class VCDiffCodeTableReaderTest {

	// The buffer size (in bytes) needed to store kCodeTableSize opcodes plus
	// up to kCodeTableSize VarintBE-encoded size values.
	static final int instruction_buffer_size = VCDiffCodeTableData.kCodeTableSize * 5;

	// This value is designed so that the total number of inst values and modes
	// will equal 8 (VCD_NOOP, VCD_ADD, VCD_RUN, VCD_COPY modes 0 - 4).
	// Eight combinations of inst and mode, times two possible size values,
	// squared (because there are two instructions per opcode), makes
	// exactly 256 possible instruction combinations, which fits kCodeTableSize
	// (the number of opcodes in the table.)
	static final short kLastExerciseMode = 4;

	protected VCDiffCodeTableReader reader_ = new VCDiffCodeTableReader();

	// A code table that exercises as many combinations as possible:
	// 2 instructions, each is a NOOP, ADD, RUN, or one of 5 copy modes
	// (== 8 total combinations of inst and mode), and each has
	// size == 0 or 255 (2 possibilities.)
	protected VCDiffCodeTableData g_exercise_code_table_;

	// The buffer pointer used by the VCDiffCodeTableReader.
	protected ByteBuffer instructions_and_sizes_ptr_ = ByteBuffer.allocate(instruction_buffer_size);

	static void AddExerciseOpcode(
			VCDiffCodeTableData g_exercise_code_table_, 
			byte inst1, byte mode1, byte size1, byte inst2, byte mode2, byte size2, int opcode) {
		g_exercise_code_table_.inst1[opcode] = inst1;
		g_exercise_code_table_.mode1[opcode] = mode1;
		g_exercise_code_table_.size1[opcode] = (inst1 == VCDiffCodeTableData.VCD_NOOP) ? 0 : size1;
		g_exercise_code_table_.inst2[opcode] = inst2;
		g_exercise_code_table_.mode2[opcode] = mode2;
		g_exercise_code_table_.size2[opcode] = (inst2 == VCDiffCodeTableData.VCD_NOOP) ? 0 : size2;
	}

	public VCDiffCodeTableReaderTest() {
		g_exercise_code_table_ = new VCDiffCodeTableData();
		int opcode = 0;
		for (byte inst_mode1 = 0; inst_mode1 <= VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode1) {
			byte inst1 = inst_mode1;
			byte mode1 = 0;

			if (inst_mode1 > VCDiffCodeTableData.VCD_COPY) {
				inst1 = VCDiffCodeTableData.VCD_COPY;
				mode1 = (byte)(inst_mode1 - VCDiffCodeTableData.VCD_COPY);
			}

			for (byte inst_mode2 = 0; inst_mode2 <= VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode2) {
				byte inst2 = inst_mode2;
				byte mode2 = 0;

				if (inst_mode2 > VCDiffCodeTableData.VCD_COPY) {
					inst2 = VCDiffCodeTableData.VCD_COPY;
					mode2 = (byte)(inst_mode2 - VCDiffCodeTableData.VCD_COPY);
				}

				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)0, inst2, mode2, (byte)0, opcode++);
				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)0, inst2, mode2, (byte)255, opcode++);
				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)255, inst2, mode2, (byte)0, opcode++);
				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)255, inst2, mode2, (byte)255, opcode++);
			}
		}
		Assert.assertEquals(VCDiffCodeTableData.kCodeTableSize, opcode);
		Assert.assertTrue(VCDiffCodeTableData.kDefaultCodeTableData.Validate());
		Assert.assertTrue(g_exercise_code_table_.Validate(kLastExerciseMode));
	}

	void VerifyInstModeSize(byte inst, byte mode, byte size, byte opcode) {
		if (inst == VCDiffCodeTableData.VCD_NOOP) return;  // GetNextInstruction skips NOOPs

		AtomicInteger found_size = new AtomicInteger(0);
		AtomicInteger found_mode = new AtomicInteger(0);

		byte found_inst = reader_.GetNextInstruction(found_size, found_mode);

		Assert.assertEquals(inst, found_inst);
		Assert.assertEquals(mode, found_mode.get());

		if (size == 0) {
			Assert.assertEquals(1000 + (opcode & 0xff), found_size.get());
		} else {
			Assert.assertEquals(size, found_size.get());
		}
	}

	void VerifyInstModeSize1(byte inst, byte mode, byte size, byte opcode) {
		if (inst == VCDiffCodeTableData.VCD_NOOP) {
			size = 0;
		}

		Assert.assertEquals(g_exercise_code_table_.inst1[opcode & 0xff], inst);
		Assert.assertEquals(g_exercise_code_table_.mode1[opcode & 0xff], mode);
		Assert.assertEquals(g_exercise_code_table_.size1[opcode & 0xff], size);

		VerifyInstModeSize(inst, mode, size, opcode);
	}

	void VerifyInstModeSize2(byte inst, byte mode, byte size, byte opcode) {
		if (inst == VCDiffCodeTableData.VCD_NOOP) {
			size = 0;
		}

		Assert.assertEquals(g_exercise_code_table_.inst2[opcode & 0xff], inst);
		Assert.assertEquals(g_exercise_code_table_.mode2[opcode & 0xff], mode);
		Assert.assertEquals(g_exercise_code_table_.size2[opcode & 0xff], size);

		VerifyInstModeSize(inst, mode, size, opcode);
	}

	@Test
	public void ReadAddTest() {
		instructions_and_sizes_ptr_.put((byte)1);
		VarInt.putInt(instructions_and_sizes_ptr_, 257);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(257, size.get());
		Assert.assertEquals(0, mode.get());
	}

	@Test
	public void ReadRunTest() {
		instructions_and_sizes_ptr_.put((byte)0);
		VarInt.putInt(instructions_and_sizes_ptr_, 111);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);
		Assert.assertEquals(VCDiffCodeTableData.VCD_RUN, found_inst);
		Assert.assertEquals(111, size.get());
		Assert.assertEquals(0, mode.get());
	}

	@Test
	public void ReadCopyTest() {
		instructions_and_sizes_ptr_.put((byte)58);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(10, size.get());
		Assert.assertEquals(2, mode.get());
	}

	@Test
	public void ReadAddCopyTest() {
		instructions_and_sizes_ptr_.put((byte)175);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());

		found_inst = reader_.GetNextInstruction(size, mode);
		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(1, mode.get());
	}

	@Test
	public void ReadCopyAdd() {
		instructions_and_sizes_ptr_.put((byte)255);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());

		mode.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);
		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());
	}

	@Test
	public void UnGetAdd() {
		instructions_and_sizes_ptr_.put((byte)1);
		VarInt.putInt(instructions_and_sizes_ptr_, 257);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(257, size.get());
		Assert.assertEquals(0, mode.get());

		reader_.UnGetInstruction();
		size.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);
		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(257, size.get());
		Assert.assertEquals(0, mode.get());
	}

	@Test
	public void UnGetCopy() {
		instructions_and_sizes_ptr_.put((byte)58);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.put((byte)255);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(10, size.get());
		Assert.assertEquals(2, mode.get());

		reader_.UnGetInstruction();
		size.set(0);
		mode.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(10, size.get());
		Assert.assertEquals(2, mode.get());
	}

	@Test
	public void UnGetCopyAddTest() {
		instructions_and_sizes_ptr_.put((byte)255);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());

		reader_.UnGetInstruction();
		size.set(0);
		mode.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());

		size.set(0);
		mode.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());
	}

	@Test
	public void UnGetTwiceTest() {
		instructions_and_sizes_ptr_.put((byte)255);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());

		reader_.UnGetInstruction();
		reader_.UnGetInstruction();

		mode.set(0);
		size.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());

		mode.set(0);
		size.set(0);

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());
	}

	@Test
	public void UnGetBeforeGet() {
		instructions_and_sizes_ptr_.put((byte)255);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		reader_.UnGetInstruction();

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());
	}

	@Test
	public void UnGetAddCopyTest() {
		instructions_and_sizes_ptr_.put((byte)175);
		instructions_and_sizes_ptr_.put((byte)0);
		instructions_and_sizes_ptr_.flip();

		reader_.Init(instructions_and_sizes_ptr_);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		byte found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());

		reader_.UnGetInstruction();

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, found_inst);
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());

		found_inst = reader_.GetNextInstruction(size, mode);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, found_inst);
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(1, mode.get());
	}

	@Test
	public void ReReadIncomplete() {
		instructions_and_sizes_ptr_.put((byte)175);	// Add(1) + Copy1(4)
		instructions_and_sizes_ptr_.put((byte)1);	// Add(0)
		instructions_and_sizes_ptr_.put((byte)111);	// with size 111
		instructions_and_sizes_ptr_.put((byte)255);	// Copy8(4) + Add(1)

		// 0 bytes available
		instructions_and_sizes_ptr_.limit(0);

		AtomicInteger size = new AtomicInteger();
		AtomicInteger mode = new AtomicInteger();

		reader_.Init(instructions_and_sizes_ptr_);

		Assert.assertEquals(VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(0, instructions_and_sizes_ptr_.position());

		// 1 more byte available
		instructions_and_sizes_ptr_.limit(1);
		reader_.Init(instructions_and_sizes_ptr_);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(1, mode.get());
		Assert.assertEquals(VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(1, instructions_and_sizes_ptr_.position());

		// 1 more byte available
		instructions_and_sizes_ptr_.limit(instructions_and_sizes_ptr_.position() + 1);
		reader_.Init(instructions_and_sizes_ptr_);

		// The opcode is available, but the separately encoded size is not
		Assert.assertEquals(VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(1, instructions_and_sizes_ptr_.position());

		// 2 more bytes available
		instructions_and_sizes_ptr_.limit(instructions_and_sizes_ptr_.position() + 2);
		reader_.Init(instructions_and_sizes_ptr_);

		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(111, size.get());
		Assert.assertEquals(0, mode.get());
		Assert.assertEquals(VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(3, instructions_and_sizes_ptr_.position());

		// 1 more byte available
		instructions_and_sizes_ptr_.limit(instructions_and_sizes_ptr_.position() + 1);
		reader_.Init(instructions_and_sizes_ptr_);

		Assert.assertEquals(VCDiffCodeTableData.VCD_COPY, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(4, size.get());
		Assert.assertEquals(8, mode.get());
		Assert.assertEquals(VCDiffCodeTableData.VCD_ADD, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(1, size.get());
		Assert.assertEquals(0, mode.get());
		Assert.assertEquals(VCDiffCodeTableData.VCD_INSTRUCTION_END_OF_DATA, reader_.GetNextInstruction(size, mode));
		Assert.assertEquals(4, instructions_and_sizes_ptr_.position());
	}

	@Test
	public void ExerciseCodeTableReaderTest() {
		for (int opcode = 0; opcode < VCDiffCodeTableData.kCodeTableSize; ++opcode) {
			instructions_and_sizes_ptr_.put((byte)opcode);
			if ((g_exercise_code_table_.inst1[opcode] != VCDiffCodeTableData.VCD_NOOP) && (g_exercise_code_table_.size1[opcode] == 0)) {
				// A separately-encoded size value
				int startPos = instructions_and_sizes_ptr_.position();
				VarInt.putInt(instructions_and_sizes_ptr_, 1000 + opcode);
				Assert.assertTrue(startPos < instructions_and_sizes_ptr_.position());
			}
			if ((g_exercise_code_table_.inst2[opcode] != VCDiffCodeTableData.VCD_NOOP) && (g_exercise_code_table_.size2[opcode] == 0)) {
				int startPos = instructions_and_sizes_ptr_.position();
				VarInt.putInt(instructions_and_sizes_ptr_, 1000 + opcode);
				Assert.assertTrue(startPos < instructions_and_sizes_ptr_.position());
			}
		}
		
		instructions_and_sizes_ptr_.flip();
		
		reader_ = new VCDiffCodeTableReader(g_exercise_code_table_, kLastExerciseMode);
		reader_.Init(instructions_and_sizes_ptr_);
		
		int opcode = 0;
		
		// This loop has the same bounds as the one in SetUpTestCase.
		// Iterate over the instruction types and make sure that the opcodes,
		// interpreted in order, return exactly those instruction types.
		for (byte inst_mode1 = 0; inst_mode1 <= VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode1) {
			byte inst1 = inst_mode1;
			byte mode1 = 0;
			if (inst_mode1 > VCDiffCodeTableData.VCD_COPY) {
				inst1 = VCDiffCodeTableData.VCD_COPY;
				mode1 = (byte) (inst_mode1 - VCDiffCodeTableData.VCD_COPY);
			}
			for (byte inst_mode2 = 0; inst_mode2 <= VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode2) {
				byte inst2 = inst_mode2;
				byte mode2 = 0;
				if (inst_mode2 > VCDiffCodeTableData.VCD_COPY) {
					inst2 = VCDiffCodeTableData.VCD_COPY;
					mode2 = (byte) (inst_mode2 - VCDiffCodeTableData.VCD_COPY);
				}
				VerifyInstModeSize1(inst1, mode1, (byte)0, (byte)opcode);
				VerifyInstModeSize2(inst2, mode2, (byte)0, (byte)opcode);
				++opcode;
				VerifyInstModeSize1(inst1, mode1, (byte)0, (byte)opcode);
				VerifyInstModeSize2(inst2, mode2, (byte)255, (byte)opcode);
				++opcode;
				VerifyInstModeSize1(inst1, mode1, (byte)255, (byte)opcode);
				VerifyInstModeSize2(inst2, mode2, (byte)0, (byte)opcode);
				++opcode;
				VerifyInstModeSize1(inst1, mode1, (byte)255, (byte)opcode);
				VerifyInstModeSize2(inst2, mode2, (byte)255, (byte)opcode);
				++opcode;
			}
		}

		Assert.assertEquals(VCDiffCodeTableData.kCodeTableSize, opcode);
	}
}
