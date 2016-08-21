package com.davidehrmann.vcdiff;

import java.util.Arrays;

import static com.davidehrmann.vcdiff.VCDiffCodeTableData.*;


/**
 * An alternate representation of the data in a VCDiffCodeTableData that
 * optimizes for fast encoding, that is, for taking a delta instruction
 * inst (also known as instruction type), size, and mode and arriving at
 * the corresponding opcode.
 */
public class VCDiffInstructionMap {

    public static final VCDiffInstructionMap DEFAULT_INSTRUCTION_MAP = new VCDiffInstructionMap(
            VCDiffCodeTableData.kDefaultCodeTableData, VCDiffAddressCache.DefaultLastMode());

    private final FirstInstructionMap first_instruction_map_;
    private final SecondInstructionMap second_instruction_map_;

    /**
     * Create a VCDiffInstructionMap from the information in code_table_data.
     * Does not save a pointer to code_table_data after using its contents
     * to create the instruction-&gt;opcode mappings.  The caller *must* have
     * verified that code_table_data-&gt;Validate() returned true before
     * attempting to use this constructor.
     * max_mode is the maximum value for the mode of a COPY instruction.
     */
    public VCDiffInstructionMap(VCDiffCodeTableData code_table_data, byte max_mode) {
        first_instruction_map_ = new FirstInstructionMap(VCD_LAST_INSTRUCTION_TYPE + max_mode + 1, FindMaxSize(code_table_data.size1));
        second_instruction_map_ = new SecondInstructionMap(VCD_LAST_INSTRUCTION_TYPE + max_mode + 1, FindMaxSize(code_table_data.size2));

        // First pass to fill up first_instruction_map_
        for (int opcode = 0; opcode < VCDiffCodeTableData.kCodeTableSize; ++opcode) {
            if (code_table_data.inst2[opcode] == VCD_NOOP) {
                // Single instruction.  If there is more than one opcode for the same
                // inst, mode, and size, then the lowest-numbered opcode will always
                // be used by the encoder, because of the descending loop.
                first_instruction_map_.Add(code_table_data.inst1[opcode],
                        code_table_data.size1[opcode],
                        code_table_data.mode1[opcode],
                        (byte)opcode);
            } else if (code_table_data.inst1[opcode] == VCD_NOOP) {
                // An unusual case where inst1 == NOOP and inst2 == ADD, RUN, or COPY.
                // This is valid under the standard, but unlikely to be used.
                // add it to the first instruction map as if inst1 and inst2 were swapped.
                first_instruction_map_.Add(code_table_data.inst2[opcode],
                        code_table_data.size2[opcode],
                        code_table_data.mode2[opcode],
                        (byte)opcode);
            }
        }
        // Second pass to fill up second_instruction_map_ (depends on first pass)
        for (int opcode = 0; opcode < VCDiffCodeTableData.kCodeTableSize; ++opcode) {
            if ((code_table_data.inst1[opcode] != VCD_NOOP) && (code_table_data.inst2[opcode] != VCD_NOOP)) {
                // Double instruction.  Find the corresponding single instruction opcode
                final short single_opcode = LookupFirstOpcode(code_table_data.inst1[opcode],
                        code_table_data.size1[opcode],
                        code_table_data.mode1[opcode]);

                if (single_opcode == kNoOpcode) {
                    continue;  // No single opcode found
                }

                second_instruction_map_.Add((byte)single_opcode,
                        code_table_data.inst2[opcode],
                        code_table_data.size2[opcode],
                        code_table_data.mode2[opcode],
                        (byte)opcode);
            }
        }
    }

    /**
     * Finds an opcode that has the given inst, size, and mode for its first
     * instruction and NOOP for its second instruction (or vice versa.)
     * Returns kNoOpcode if the code table does not have any matching
     * opcode. Otherwise, returns an opcode value between 0 and 255.
     *
     * If this function returns kNoOpcode for size &gt; 0, the caller will
     * usually want to try again with size == 0 to find an opcode that
     * doesn't have a fixed size value.
     *
     * If this function returns kNoOpcode for size == 0, it is an error condition,
     * because any code table that passed the Validate() check should have a way
     * of expressing all combinations of inst and mode with size=0.
     *
     * @param inst instruction of the opcode
     * @param size size of the opcode
     * @param mode mode of the opcode
     * @return opcode for the given parameters
     */
    public short LookupFirstOpcode(byte inst, byte size, byte mode) {
        return first_instruction_map_.Lookup(inst, size, mode);
    }

    /**
     * Given a first opcode (presumed to have been returned by a previous call to
     * lookupFirstOpcode), finds an opcode that has the same first instruction as
     * the first opcode, and has the given inst, size, and mode for its second
     * instruction.
     *
     * If this function returns kNoOpcode for size &gt; 0, the caller will
     * usually want to try again with size == 0 to find an opcode that
     * doesn't have a fixed size value.
     *
     * @param first_opcode result of {@link #LookupFirstOpcode(byte, byte, byte)}}
     * @param inst instruction of the opcode
     * @param size size of the opcode
     * @param mode mode of the opcode
     * @return opcode for the given parameters
     */
    public short LookupSecondOpcode(byte first_opcode, byte inst, byte size, byte mode) {
        return second_instruction_map_.Lookup(first_opcode, inst, size, mode);
    }

    private static int FindMaxSize(byte[] size_array) {
        int max_size = size_array[0] & 0xff;
        for (int i = 1; i < size_array.length; ++i) {
            if ((size_array[i] & 0xff) > max_size) {
                max_size = (size_array[i] & 0xff);
            }
        }
        return max_size;
    }

    /**
     * Data structure used to implement LookupFirstOpcode efficiently.
     */
    private static class FirstInstructionMap {
        // The number of possible combinations of inst (a VCDiffInstructionType) and
        // mode.  Since the mode is only used for COPY instructions, this number
        // is not (number of VCDiffInstructionType values) * (number of modes), but
        // rather (number of VCDiffInstructionType values other than VCD_COPY)
        // + (number of COPY modes).
        //
        // Compressing inst and mode into a single integer relies on
        // VCD_COPY being the last instruction type.  The inst+mode values are:
        // 0 (NOOP), 1 (ADD), 2 (RUN), 3 (COPY mode 0), 4 (COPY mode 1), ...
        private	final int num_instruction_type_modes_;

        // The maximum value of a size1 element in code_table_data
        private final int max_size_1_;


        /**
         *
         * There are two levels to first_opcodes_:
         * 1) A dynamically-allocated pointer array of size
         *    num_instruction_type_modes_ (one element for each combination of inst
         *    and mode.)  Every element of this array is non-NULL and contains
         *    a pointer to:
         * 2) A dynamically-allocated array of OpcodeOrNone values, with one element
         *    for each possible first instruction size (size1) in the code table.
         *    (In the default code table, for example, the maximum size used is 18,
         *    so these arrays would have 19 elements representing values 0
         *    through 18.)
         */
        private final short[][] first_opcodes_;

        public FirstInstructionMap(int num_insts_and_modes, int max_size_1) {
            this.num_instruction_type_modes_ = num_insts_and_modes;
            this.max_size_1_ = max_size_1;

            // There must be at least (max_size_1_ + 1) elements in first_opcodes_
            // because the element first_opcodes[max_size_1_] will be referenced.
            first_opcodes_ = new short[num_instruction_type_modes_][max_size_1 + 1];

            for (short[] array : first_opcodes_) {
                Arrays.fill(array, kNoOpcode);
            }
        }

        public void Add(final byte inst, final byte size, final byte mode, final byte opcode) {
            if (first_opcodes_[(inst & 0xff) + (mode & 0xff)][size & 0xff] == kNoOpcode) {
                first_opcodes_[(inst & 0xff) + (mode & 0xff)][size & 0xff] = (short)(opcode & 0xff);
            }
        }

        // See comments for LookupFirstOpcode, above.
        public short Lookup(final byte inst, final byte size, final byte mode) {
            int inst_mode = (inst == VCD_COPY) ? ((inst & 0xff) + (mode & 0xff)) : (inst & 0xff);
            if ((size & 0xff) > max_size_1_) {
                return kNoOpcode;
            }
            // Lookup specific-sized opcode
            return first_opcodes_[inst_mode][size & 0xff];
        }
    }

    /**
     * Data structure used to implement LookupSecondOpcode efficiently.
     */
    private static class SecondInstructionMap {
        // See the member of the same name in FirstInstructionMap.
        private final int num_instruction_type_modes_;

        // The maximum value of a size2 element in code_table_data
        private final int max_size_2_;

        // There are three levels to second_opcodes_:
        // 1) A statically-allocated pointer array with one element
        //    for each possible opcode.  Each element can be NULL, or can point to:
        // 2) A dynamically-allocated pointer array of size
        //    num_instruction_type_modes_ (one element for each combination of inst
        //    and mode.)  Each element can be NULL, or can point to:
        // 3) A dynamically-allocated array with one element for each possible
        //    second instruction size in the code table.  (In the default code
        //    table, for example, the maximum size used is 6, so these arrays would
        //    have 7 elements representing values 0 through 6.)
        private final short[][][] second_opcodes_ = new short[VCDiffCodeTableData.kCodeTableSize][][];

        public SecondInstructionMap(int num_insts_and_modes, int max_size_2) {
            num_instruction_type_modes_ = num_insts_and_modes;
            max_size_2_ = max_size_2;
        }

        public void Add(final byte first_opcode, final byte inst, final byte size, final byte mode, final byte second_opcode) {
            if (second_opcodes_[first_opcode & 0xff] == null) {
                second_opcodes_[first_opcode & 0xff] = new short[num_instruction_type_modes_][];
            }

            if (second_opcodes_[first_opcode & 0xff][(inst & 0xff) + (mode & 0xff)] == null) {
                second_opcodes_[first_opcode & 0xff][(inst & 0xff) + (mode & 0xff)] = new short[max_size_2_ + 1];
                Arrays.fill(second_opcodes_[first_opcode & 0xff][(inst & 0xff) + (mode & 0xff)], kNoOpcode);
            }

            if (second_opcodes_[first_opcode & 0xff][(inst & 0xff) + (mode & 0xff)][size & 0xff] == kNoOpcode) {
                second_opcodes_[first_opcode & 0xff][(inst & 0xff) + (mode & 0xff)][size & 0xff] = (short)(second_opcode & 0xff);
            }
        }

        // See comments for LookupSecondOpcode, above.
        public short Lookup(final byte first_opcode, final byte inst, final byte size, final byte mode) {
            if ((size & 0xff) > max_size_2_) {
                return kNoOpcode;
            }

            if (second_opcodes_[first_opcode & 0xff] == null) {
                return kNoOpcode;
            }

            int inst_mode = (inst == VCD_COPY) ? ((inst & 0xff) + (mode & 0xff)) : (inst & 0xff);
            if (second_opcodes_[first_opcode & 0xff][inst_mode] == null) {
                return kNoOpcode;
            }

            return second_opcodes_[first_opcode & 0xff][inst_mode][size & 0xff];
        }
    }
}
