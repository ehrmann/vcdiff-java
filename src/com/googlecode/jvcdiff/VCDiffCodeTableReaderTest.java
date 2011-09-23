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
			Assert.assertEquals(1000 + opcode, found_size.get());
		} else {
			Assert.assertEquals(opcode, found_size.get());
		}
	}

	void VerifyInstModeSize1(byte inst, byte mode, byte size, byte opcode) {
		if (inst == VCDiffCodeTableData.VCD_NOOP) {
			size = 0;
		}

		Assert.assertEquals(g_exercise_code_table_.inst1[opcode], inst);
		Assert.assertEquals(g_exercise_code_table_.mode1[opcode], mode);
		Assert.assertEquals(g_exercise_code_table_.size1[opcode], size);

		VerifyInstModeSize(inst, mode, size, opcode);
	}

	void VerifyInstModeSize2(byte inst, byte mode, byte size, byte opcode) {
		if (inst == VCDiffCodeTableData.VCD_NOOP) {
			size = 0;
		}

		Assert.assertEquals(g_exercise_code_table_.inst2[opcode], inst);
		Assert.assertEquals(g_exercise_code_table_.mode2[opcode], mode);
		Assert.assertEquals(g_exercise_code_table_.size2[opcode], size);

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
	public void ReadCopy() {
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
}
