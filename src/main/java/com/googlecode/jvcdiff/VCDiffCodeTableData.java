package com.googlecode.jvcdiff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VCDiffCodeTableData implements Cloneable {

	private static final Logger LOGGER = LoggerFactory.getLogger(VCDiffCodeTableData.class);

	public static final int kCodeTableSize = 256;

	public static final byte VCD_NOOP = 0;
	public static final byte VCD_ADD  = 1;
	public static final byte VCD_RUN  = 2;
	public static final byte VCD_COPY = 3;
	protected static final byte VCD_LAST_INSTRUCTION_TYPE = VCD_COPY;

	// Single-letter abbreviations that make it easier to read the default code table data.
	private static final byte N = VCD_NOOP;
	private static final byte A = VCD_ADD;
	private static final byte R = VCD_RUN;
	private static final byte C = VCD_COPY;

	// The following values are not true instruction types, but rather
	// special condition values for functions that return VCDiffInstructionType.
	public static final byte VCD_INSTRUCTION_ERROR = 4;
	public static final byte VCD_INSTRUCTION_END_OF_DATA = 5;

	protected static final short kNoOpcode = 0x100;  // outside the opcode range 0x00 - 0xFF

	public static final VCDiffCodeTableData kDefaultCodeTableData = new VCDiffCodeTableData(
			// inst1
			new byte[] {
					R,  // opcode 0
					A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 1-18
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 19-34
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 35-50
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 51-66
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 67-82
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 83-98
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 99-114
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 115-130
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 131-146
					C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 147-162
					A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 163-174
					A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 175-186
					A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 187-198
					A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 199-210
					A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 211-222
					A, A, A, A, A, A, A, A, A, A, A, A,  // opcodes 223-234
					A, A, A, A,  // opcodes 235-238
					A, A, A, A,  // opcodes 239-242
					A, A, A, A,  // opcodes 243-246
					C, C, C, C, C, C, C, C, C   // opcodes 247-255	
			},

			// inst2
			new byte[] {
					N,  // opcode 0
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 1-18
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 19-34
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 35-50
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 51-66
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 67-82
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 83-98
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 99-114
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 115-130
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 131-146
					N, N, N, N, N, N, N, N, N, N, N, N, N, N, N, N,  // opcodes 147-162
					C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 163-174
					C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 175-186
					C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 187-198
					C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 199-210
					C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 211-222
					C, C, C, C, C, C, C, C, C, C, C, C,  // opcodes 223-234
					C, C, C, C,  // opcodes 235-238
					C, C, C, C,  // opcodes 239-242
					C, C, C, C,  // opcodes 243-246
					A, A, A, A, A, A, A, A, A   // opcodes 247-2	
			},

			// size1
			new byte[] {
					0,  // opcode 0
					0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,  // 1-18
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 19-34
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 35-50
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 51-66
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 67-82
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 83-98
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 99-114
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 115-130
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 131-146
					0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  // 147-162
					1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4,  // opcodes 163-174
					1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4,  // opcodes 175-186
					1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4,  // opcodes 187-198
					1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4,  // opcodes 199-210
					1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4,  // opcodes 211-222
					1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4,  // opcodes 223-234
					1, 2, 3, 4,  // opcodes 235-238
					1, 2, 3, 4,  // opcodes 239-242
					1, 2, 3, 4,  // opcodes 243-246
					4, 4, 4, 4, 4, 4, 4, 4, 4   // opcodes 247-255
			},

			// size2
			new byte[] {
					0,  // opcode 0
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 1-18
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 19-34
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 35-50
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 51-66
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 67-82
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 83-98
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 99-114
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 115-130
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 131-146
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 147-162
					4, 5, 6, 4, 5, 6, 4, 5, 6, 4, 5, 6,  // opcodes 163-174
					4, 5, 6, 4, 5, 6, 4, 5, 6, 4, 5, 6,  // opcodes 175-186
					4, 5, 6, 4, 5, 6, 4, 5, 6, 4, 5, 6,  // opcodes 187-198
					4, 5, 6, 4, 5, 6, 4, 5, 6, 4, 5, 6,  // opcodes 199-210
					4, 5, 6, 4, 5, 6, 4, 5, 6, 4, 5, 6,  // opcodes 211-222
					4, 5, 6, 4, 5, 6, 4, 5, 6, 4, 5, 6,  // opcodes 223-234
					4, 4, 4, 4,  // opcodes 235-238
					4, 4, 4, 4,  // opcodes 239-242
					4, 4, 4, 4,  // opcodes 243-246
					1, 1, 1, 1, 1, 1, 1, 1, 1  // opcodes 247-255
			},

			// mode1
			new byte[] {
					0,  // opcode 0
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 1-18
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 19-34
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,  // opcodes 35-50
					2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,  // opcodes 51-66
					3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,  // opcodes 67-82
					4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,  // opcodes 83-98
					5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,  // opcodes 99-114
					6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,  // opcodes 115-130
					7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,  // opcodes 131-146
					8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,  // opcodes 147-162
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 163-174
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 175-186
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 187-198
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 199-210
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 211-222
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 223-234
					0, 0, 0, 0,  // opcodes 235-238
					0, 0, 0, 0,  // opcodes 239-242
					0, 0, 0, 0,  // opcodes 243-246
					0, 1, 2, 3, 4, 5, 6, 7, 8   // opcodes 247-255
			},

			// mode2
			new byte[] {
					0,  // opcode 0
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 1-18
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 19-34
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 35-50
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 51-66
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 67-82
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 83-98
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 99-114
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 115-130
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 131-146
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 147-162
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // opcodes 163-174
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,  // opcodes 175-186
					2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,  // opcodes 187-198
					3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,  // opcodes 199-210
					4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,  // opcodes 211-222
					5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,  // opcodes 223-234
					6, 6, 6, 6,  // opcodes 235-238
					7, 7, 7, 7,  // opcodes 239-242
					8, 8, 8, 8,  // opcodes 243-246
					0, 0, 0, 0, 0, 0, 0, 0, 0  // opcodes 247-255
			}
	);
	
	public static final int SERIALIZED_BYTE_SIZE = kDefaultCodeTableData.getBytes().length;

	// FIXME: improve access modifiers
	public final byte inst1[] = new byte[kCodeTableSize];  // from enum VCDiffInstructionType
	public final byte inst2[] = new byte[kCodeTableSize];  // from enum VCDiffInstructionType
	public final byte size1[] = new byte[kCodeTableSize];
	public final byte size2[] = new byte[kCodeTableSize];
	public final byte mode1[] = new byte[kCodeTableSize];  // from enum VCDiffModes
	public final byte mode2[] = new byte[kCodeTableSize];  // from enum VCDiffModes

	private volatile byte[] bytes = null;
	
	public VCDiffCodeTableData() {

	}

	public VCDiffCodeTableData(byte[] bytes) {
		if (bytes.length != inst1.length + inst2.length + size1.length + size2.length + mode1.length + mode2.length) {
			throw new IllegalArgumentException();
		}

		int srcPos = 0;

		System.arraycopy(bytes, srcPos, inst1, 0, inst1.length);
		srcPos += inst1.length;

		System.arraycopy(bytes, srcPos, inst2, 0, inst2.length);
		srcPos += inst2.length;

		System.arraycopy(bytes, srcPos, size1, 0, size1.length);
		srcPos += size1.length;

		System.arraycopy(bytes, srcPos, size2, 0, size2.length);
		srcPos += size2.length;

		System.arraycopy(bytes, srcPos, mode1, 0, mode1.length);
		srcPos += mode1.length;

		System.arraycopy(bytes, srcPos, mode2, 0, mode2.length);
		srcPos += mode2.length;
	}

	public VCDiffCodeTableData(byte[] inst1, byte[] inst2, byte[] size1, byte[] size2, byte[] mode1, byte[] mode2) {
		if (inst1.length != kCodeTableSize || inst2.length != kCodeTableSize ||
				size1.length != kCodeTableSize || size2.length != kCodeTableSize ||
				mode1.length != kCodeTableSize || mode2.length != kCodeTableSize) {
			throw new IllegalArgumentException();
		}

		System.arraycopy(inst1, 0, this.inst1, 0, kCodeTableSize);
		System.arraycopy(inst2, 0, this.inst2, 0, kCodeTableSize);
		System.arraycopy(size1, 0, this.size1, 0, kCodeTableSize);
		System.arraycopy(size2, 0, this.size2, 0, kCodeTableSize);
		System.arraycopy(mode1, 0, this.mode1, 0, kCodeTableSize);
		System.arraycopy(mode2, 0, this.mode2, 0, kCodeTableSize);
	}

	public VCDiffCodeTableData clone() {
		return new VCDiffCodeTableData(this.inst1, this.inst2, this.size1, this.size2, this.mode1, this.mode2);
	}

	public static String VCDiffInstructionName(int inst) {
		switch (inst) {
		case VCD_NOOP:
			return "NOOP";
		case VCD_ADD:
			return "ADD";
		case VCD_RUN:
			return "RUN";
		case VCD_COPY:
			return "COPY";
		default:
			return "";
		}
	}

	protected static boolean ValidateOpcode(
			int opcode,
			short inst,
			short size,
			short mode,
			short max_mode,
			String first_or_second) {

		boolean no_errors_found = true;

		// Check upper and lower limits of inst and mode.
		if (inst > VCD_LAST_INSTRUCTION_TYPE || inst < 0) {
			LOGGER.warn("VCDiff: Bad code table; opcode {} has invalid {} instruction type {}", opcode, first_or_second, inst);
			no_errors_found = false;
		}
		if (mode > max_mode || mode < 0) {
			LOGGER.warn("VCDiff: Bad code table; opcode {} has invalid {} mode {}", opcode, first_or_second, mode);
			no_errors_found = false;
		}
		// A NOOP instruction must have size 0
		// (and mode 0, which is included in the next rule)
		if ((inst == VCD_NOOP) && (size != 0)) {
			LOGGER.warn("VCDiff: Bad code table; opcode {} has {} instruction NOOP with nonzero size {}", opcode, first_or_second, size);
			no_errors_found = false;
		}
		// Size is less than 0
		if (size < 0) {
			LOGGER.warn("VCDiff: Bad code table; opcode {} has {} instruction with size less than zero {}", opcode, first_or_second, size);
			no_errors_found = false;
		}
		// A nonzero mode can only be used with a COPY instruction
		if ((inst != VCD_COPY) && (mode != 0)) {
			LOGGER.warn("VCDiff: Bad code table; opcode {} has non-COPY {} instruction with nonzero mode {}", opcode, first_or_second, mode);
			no_errors_found = false;
		}

		return no_errors_found;
	}

	protected boolean Validate(short max_mode) {
		final int kNumberOfTypesAndModes = VCD_LAST_INSTRUCTION_TYPE + max_mode + 1;
		boolean[] hasOpcodeForTypeAndMode = new boolean[VCD_LAST_INSTRUCTION_TYPE + VCDiffAddressCache.VCD_MAX_MODES];
		boolean no_errors_found = true;
		for (int i = 0; i < kNumberOfTypesAndModes; ++i) {
			hasOpcodeForTypeAndMode[i] = false;
		}
		for (int i = 0; i < kCodeTableSize; ++i) {
			no_errors_found =
				ValidateOpcode(i, (short)(inst1[i] & 0xff), (short)(size1[i] & 0xff), (short)(mode1[i] & 0xff), max_mode, "first")
				&& no_errors_found;  // use as 2nd operand to avoid short-circuit
			no_errors_found =
				ValidateOpcode(i, (short)(inst2[i] & 0xff), (short)(size2[i] & 0xff), (short)(mode2[i] & 0xff), max_mode, "second")
				&& no_errors_found;

			// A valid code table must have an opcode to encode every possible
			// combination of inst and mode with size=0 as its first instruction,
			// and NOOP as its second instruction.  If this condition fails,
			// then there exists a set of input instructions that cannot be encoded.
			if (size1[i] == 0 && inst2[i] == VCD_NOOP &&
					((inst1[i] & 0xff) + (mode1[i] & 0xff)) < kNumberOfTypesAndModes) {
				hasOpcodeForTypeAndMode[(inst1[i] & 0xff) + (mode1[i] & 0xff)] = true;
			}
		}
		for (int i = 0; i < kNumberOfTypesAndModes; ++i) {
			if (i == VCD_NOOP) continue;
			if (!hasOpcodeForTypeAndMode[i])  {
				if (i >= VCD_COPY) {
					LOGGER.warn("VCDiff: Bad code table; there is no opcode for inst COPY, size 0, mode {}", (i - VCD_COPY));
				} else {
					LOGGER.warn("VCDiff: Bad code table; there is no opcode for inst {}, size -,  mode 0", VCDiffInstructionName(i));
				}
				no_errors_found = false;
			}
		}
		return no_errors_found;
	}

	public byte[] getBytes() {
		if (bytes == null) {
			synchronized (this) {
				if (bytes == null) {
					bytes = new byte[inst1.length + inst2.length + size1.length + size2.length + mode1.length + mode2.length];

					int destPos = 0;

					System.arraycopy(inst1, 0, bytes, destPos, inst1.length);
					destPos += inst1.length;

					System.arraycopy(inst2, 0, bytes, destPos, inst2.length);
					destPos += inst2.length;

					System.arraycopy(size1, 0, bytes, destPos, size1.length);
					destPos += size1.length;

					System.arraycopy(size2, 0, bytes, destPos, size2.length);
					destPos += size2.length;

					System.arraycopy(mode1, 0, bytes, destPos, mode1.length);
					destPos += mode1.length;

					System.arraycopy(mode2, 0, bytes, destPos, mode2.length);
					destPos += mode2.length;
				}
			}
		}

		return bytes;
	}

	protected boolean Validate() {
		return Validate(VCDiffAddressCache.DefaultLastMode());
	}
}
