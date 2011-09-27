package com.googlecode.jvcdiff;

import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_COPY;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_NOOP;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class VCDiffCodeTableWriterTest {

	// This value is designed so that the total number of inst values and modes
	// will equal 8 (VCD_NOOP, VCD_ADD, VCD_RUN, VCD_COPY modes 0 - 4).
	// Eight combinations of inst and mode, times two possible size values,
	// squared (because there are two instructions per opcode), makes
	// exactly 256 possible instruction combinations, which fits kCodeTableSize
	// (the number of opcodes in the table.)
	protected static final short kLastExerciseMode = 4;

	protected static final Charset US_ASCII = Charset.forName("US-ASCII");

	// Destination for VCDiffCodeTableWriter::Output()
	protected final ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

	// A code table that exercises as many combinations as possible:
	// 2 instructions, each is a NOOP, ADD, RUN, or one of 5 copy modes
	// (== 8 total combinations of inst and mode), and each has
	// size == 0 or 255 (2 possibilities.)
	protected final VCDiffCodeTableData g_exercise_code_table_ = new VCDiffCodeTableData();

	// The code table writer for standard encoding, default code table.
	protected final VCDiffCodeTableWriter standard_writer = new VCDiffCodeTableWriter(false);

	// The code table writer for interleaved encoding, default code table.
	protected final VCDiffCodeTableWriter interleaved_writer = new VCDiffCodeTableWriter(true);

	// The code table writer corresponding to g_exercise_code_table_ (interleaved encoding).
	protected final VCDiffCodeTableWriter exercise_writer = new VCDiffCodeTableWriter(true, VCDiffAddressCache.kDefaultNearCacheSize, VCDiffAddressCache.kDefaultSameCacheSize, g_exercise_code_table_, kLastExerciseMode);

	@Before
	public void SetUpTestCase() {
		int opcode = 0;
		for (byte inst_mode1 = 0; inst_mode1 <= VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode1) {
			byte inst1 = inst_mode1;
			byte mode1 = 0;

			if (inst_mode1 > VCD_COPY) {
				inst1 = VCD_COPY;
				mode1 = (byte)(inst_mode1 - VCD_COPY);
			}

			for (byte inst_mode2 = 0; inst_mode2 <= VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode2) {
				byte inst2 = inst_mode2;
				byte mode2 = 0;

				if (inst_mode2 > VCD_COPY) {
					inst2 = VCD_COPY;
					mode2 = (byte)(inst_mode2 - VCD_COPY);
				}

				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)0, inst2, mode2, (byte)0, opcode++);
				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)0, inst2, mode2, (byte)255, opcode++);
				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)255, inst2, mode2, (byte)0, opcode++);
				AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte)255, inst2, mode2, (byte)255, opcode++);
			}
		}
		// This is a CHECK rather than an EXPECT because it validates only
		// the logic of the test, not of the code being tested.
		assertEquals(VCDiffCodeTableData.kCodeTableSize, opcode);

		assertTrue(g_exercise_code_table_.Validate(kLastExerciseMode));
	}

	static void AddExerciseOpcode(VCDiffCodeTableData codeTable, byte inst1, byte mode1, byte size1, byte inst2, byte mode2, byte size2, int opcode) {
		codeTable.inst1[opcode] = inst1;
		codeTable.mode1[opcode] = mode1;
		codeTable.size1[opcode] = (inst1 == VCD_NOOP) ? 0 : size1;
		codeTable.inst2[opcode] = inst2;
		codeTable.mode2[opcode] = mode2;
		codeTable.size2[opcode] = (inst2 == VCD_NOOP) ? 0 : size2;
	}

	@Test
	public void WriterOutputWithoutInit() throws IOException {
		standard_writer.Output(out);
		assertEquals(0, out.size());
	}

	@Test
	public void WriterEncodeNothing() throws IOException {
		standard_writer.Init(0);
		standard_writer.Output(out);

		// The writer should know not to append a delta file window
		// if nothing was encoded.
		assertEquals(0, out.size());

		interleaved_writer.Init(0x10);
		interleaved_writer.Output(out);
		assertEquals(0, out.size());

		exercise_writer.Init(0x20);
		exercise_writer.Output(out);
		assertEquals(0, out.size());
	}

	@Test
	public void StandardWriterEncodeAdd() throws IOException {
		standard_writer.Init(0x11);
		standard_writer.Add("foo".getBytes(US_ASCII), 0, 3);
		standard_writer.Output(out);

		assertTrue(Arrays.equals(new byte[] {
				VCD_SOURCE, // Win_Indicator: VCD_SOURCE (dictionary)
				0x11,  		// Source segment size: dictionary length
				0x00,  		// Source segment position: start of dictionary
				0x09,  		// Length of the delta encoding
				0x03,  		// Size of the target window
				0x00,  		// Delta_indicator (no compression)
				0x03,  		// length of data for ADDs and RUNs
				0x01,  		// length of instructions section
				0x00,  		// length of addresses for COPYs
				'f', 'o', 'o',
				0x04,  		// ADD(3) opcode
		}, out.toByteArray()));


	}
}
