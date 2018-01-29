package com.davidehrmann.vcdiff.engine;

import org.junit.Assert;
import org.junit.Test;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableData.*;
import static org.junit.Assert.assertEquals;


public class VCDiffInstructionMapTest {

    private static void AddExerciseOpcode(VCDiffCodeTableData g_exercise_code_table_,
                                          byte inst1, byte mode1, byte size1, byte inst2, byte mode2, byte size2, int opcode) {
        g_exercise_code_table_.inst1[opcode] = inst1;
        g_exercise_code_table_.mode1[opcode] = mode1;
        g_exercise_code_table_.size1[opcode] = (inst1 == VCD_NOOP) ? 0 : size1;
        g_exercise_code_table_.inst2[opcode] = inst2;
        g_exercise_code_table_.mode2[opcode] = mode2;
        g_exercise_code_table_.size2[opcode] = (inst2 == VCD_NOOP) ? 0 : size2;
    }

    private static void VerifyExerciseFirstInstruction(VCDiffInstructionMap exercise_map, VCDiffCodeTableData g_exercise_code_table_,
                                                       byte expected_opcode, byte inst, byte size, byte mode) {

        int found_opcode = exercise_map.LookupFirstOpcode(inst, size, mode);
        if (g_exercise_code_table_.inst1[found_opcode] == VCD_NOOP) {
            // The opcode is backwards: (VCD_NOOP, [instruction])
            Assert.assertTrue((expected_opcode & 0xff) >= found_opcode);

            assertEquals(inst, g_exercise_code_table_.inst2[found_opcode]);
            assertEquals(size, g_exercise_code_table_.size2[found_opcode]);
            assertEquals(mode, g_exercise_code_table_.mode2[found_opcode]);
            assertEquals(VCD_NOOP, g_exercise_code_table_.inst1[found_opcode]);
            assertEquals(0, g_exercise_code_table_.size1[found_opcode]);
            assertEquals(0, g_exercise_code_table_.mode1[found_opcode]);
        } else {
            assertEquals(expected_opcode, found_opcode);
            assertEquals(inst, g_exercise_code_table_.inst1[found_opcode]);
            assertEquals(size, g_exercise_code_table_.size1[found_opcode]);
            assertEquals(mode, g_exercise_code_table_.mode1[found_opcode]);
            assertEquals(VCD_NOOP, g_exercise_code_table_.inst2[found_opcode]);
            assertEquals(0, g_exercise_code_table_.size2[found_opcode]);
            assertEquals(0, g_exercise_code_table_.mode2[found_opcode]);
        }
    }

    private static void VerifyExerciseSecondInstruction(VCDiffInstructionMap exercise_map,
                                                        byte expected_opcode, byte inst1, byte size1, byte mode1, byte inst2, byte size2, byte mode2) {
        short first_opcode = exercise_map.LookupFirstOpcode(inst1, size1, mode1);

        Assert.assertFalse(kNoOpcode == first_opcode);
        assertEquals(expected_opcode & 0xff, exercise_map.LookupSecondOpcode((byte) first_opcode, inst2, size2, mode2));
    }

    @Test
    public void testAssumptions() {
        Assert.assertTrue(VCDiffCodeTableData.kDefaultCodeTableData.Validate());
    }

    @Test
    public void DefaultMapLookupFirstNoopTest() {
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte) 0, (byte) 255));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte) 255, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte) 255, (byte) 255));
    }

    @Test
    public void DefaultMapLookupFirstAddTest() {
        Assert.assertEquals(2, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(3, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 2, (byte) 0));
        Assert.assertEquals(4, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 3, (byte) 0));
        Assert.assertEquals(5, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 4, (byte) 0));
        Assert.assertEquals(6, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 5, (byte) 0));
        Assert.assertEquals(7, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 6, (byte) 0));
        Assert.assertEquals(8, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 7, (byte) 0));
        Assert.assertEquals(9, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 8, (byte) 0));
        Assert.assertEquals(10, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 9, (byte) 0));
        Assert.assertEquals(11, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 10, (byte) 0));
        Assert.assertEquals(12, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 11, (byte) 0));
        Assert.assertEquals(13, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 12, (byte) 0));
        Assert.assertEquals(14, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 13, (byte) 0));
        Assert.assertEquals(15, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 14, (byte) 0));
        Assert.assertEquals(16, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 15, (byte) 0));
        Assert.assertEquals(17, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 16, (byte) 0));
        Assert.assertEquals(18, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 17, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 100, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 255, (byte) 0));
        Assert.assertEquals(1, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 0, (byte) 0));

        // Value of "mode" should not matter
        Assert.assertEquals(2, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 1, (byte) 2));
        Assert.assertEquals(2, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte) 1, (byte) 255));
    }

    @Test
    public void DefaultMapLookupFirstRun() {
        Assert.assertEquals(0, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte) 1, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte) 255, (byte) 0));
        // Value of "mode" should not matter
        Assert.assertEquals(0, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte) 0, (byte) 2));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode0() {
        Assert.assertEquals(19, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 0));
        Assert.assertEquals(20, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 0));
        Assert.assertEquals(21, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 0));
        Assert.assertEquals(22, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 0));
        Assert.assertEquals(23, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 0));
        Assert.assertEquals(24, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 0));
        Assert.assertEquals(25, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 0));
        Assert.assertEquals(26, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 0));
        Assert.assertEquals(27, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 0));
        Assert.assertEquals(28, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 0));
        Assert.assertEquals(29, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 0));
        Assert.assertEquals(30, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 0));
        Assert.assertEquals(31, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 0));
        Assert.assertEquals(32, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 0));
        Assert.assertEquals(33, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 0));
        Assert.assertEquals(34, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 0));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode1() {
        Assert.assertEquals(35, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 1));
        Assert.assertEquals(36, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 1));
        Assert.assertEquals(37, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 1));
        Assert.assertEquals(38, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 1));
        Assert.assertEquals(39, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 1));
        Assert.assertEquals(40, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 1));
        Assert.assertEquals(41, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 1));
        Assert.assertEquals(42, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 1));
        Assert.assertEquals(43, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 1));
        Assert.assertEquals(44, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 1));
        Assert.assertEquals(45, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 1));
        Assert.assertEquals(46, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 1));
        Assert.assertEquals(47, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 1));
        Assert.assertEquals(48, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 1));
        Assert.assertEquals(49, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 1));
        Assert.assertEquals(50, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 1));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode2() {
        Assert.assertEquals(51, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 2));
        Assert.assertEquals(52, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 2));
        Assert.assertEquals(53, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 2));
        Assert.assertEquals(54, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 2));
        Assert.assertEquals(55, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 2));
        Assert.assertEquals(56, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 2));
        Assert.assertEquals(57, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 2));
        Assert.assertEquals(58, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 2));
        Assert.assertEquals(59, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 2));
        Assert.assertEquals(60, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 2));
        Assert.assertEquals(61, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 2));
        Assert.assertEquals(62, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 2));
        Assert.assertEquals(63, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 2));
        Assert.assertEquals(64, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 2));
        Assert.assertEquals(65, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 2));
        Assert.assertEquals(66, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 2));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode3() {
        Assert.assertEquals(67, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 3));
        Assert.assertEquals(68, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 3));
        Assert.assertEquals(69, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 3));
        Assert.assertEquals(70, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 3));
        Assert.assertEquals(71, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 3));
        Assert.assertEquals(72, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 3));
        Assert.assertEquals(73, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 3));
        Assert.assertEquals(74, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 3));
        Assert.assertEquals(75, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 3));
        Assert.assertEquals(76, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 3));
        Assert.assertEquals(77, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 3));
        Assert.assertEquals(78, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 3));
        Assert.assertEquals(79, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 3));
        Assert.assertEquals(80, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 3));
        Assert.assertEquals(81, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 3));
        Assert.assertEquals(82, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 3));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode4() {
        Assert.assertEquals(83, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 4));
        Assert.assertEquals(84, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 4));
        Assert.assertEquals(85, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 4));
        Assert.assertEquals(86, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 4));
        Assert.assertEquals(87, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 4));
        Assert.assertEquals(88, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 4));
        Assert.assertEquals(89, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 4));
        Assert.assertEquals(90, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 4));
        Assert.assertEquals(91, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 4));
        Assert.assertEquals(92, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 4));
        Assert.assertEquals(93, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 4));
        Assert.assertEquals(94, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 4));
        Assert.assertEquals(95, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 4));
        Assert.assertEquals(96, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 4));
        Assert.assertEquals(97, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 4));
        Assert.assertEquals(98, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 4));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode5() {
        Assert.assertEquals(99, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 5));
        Assert.assertEquals(100, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 5));
        Assert.assertEquals(101, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 5));
        Assert.assertEquals(102, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 5));
        Assert.assertEquals(103, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 5));
        Assert.assertEquals(104, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 5));
        Assert.assertEquals(105, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 5));
        Assert.assertEquals(106, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 5));
        Assert.assertEquals(107, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 5));
        Assert.assertEquals(108, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 5));
        Assert.assertEquals(109, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 5));
        Assert.assertEquals(110, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 5));
        Assert.assertEquals(111, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 5));
        Assert.assertEquals(112, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 5));
        Assert.assertEquals(113, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 5));
        Assert.assertEquals(114, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 5));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode6() {
        Assert.assertEquals(115, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 6));
        Assert.assertEquals(116, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 6));
        Assert.assertEquals(117, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 6));
        Assert.assertEquals(118, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 6));
        Assert.assertEquals(119, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 6));
        Assert.assertEquals(120, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 6));
        Assert.assertEquals(121, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 6));
        Assert.assertEquals(122, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 6));
        Assert.assertEquals(123, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 6));
        Assert.assertEquals(124, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 6));
        Assert.assertEquals(125, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 6));
        Assert.assertEquals(126, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 6));
        Assert.assertEquals(127, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 6));
        Assert.assertEquals(128, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 6));
        Assert.assertEquals(129, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 6));
        Assert.assertEquals(130, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 6));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode7() {
        Assert.assertEquals(131, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 7));
        Assert.assertEquals(132, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 7));
        Assert.assertEquals(133, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 7));
        Assert.assertEquals(134, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 7));
        Assert.assertEquals(135, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 7));
        Assert.assertEquals(136, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 7));
        Assert.assertEquals(137, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 7));
        Assert.assertEquals(138, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 7));
        Assert.assertEquals(139, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 7));
        Assert.assertEquals(140, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 7));
        Assert.assertEquals(141, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 7));
        Assert.assertEquals(142, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 7));
        Assert.assertEquals(143, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 7));
        Assert.assertEquals(144, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 7));
        Assert.assertEquals(145, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 7));
        Assert.assertEquals(146, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 7));
    }

    @Test
    public void DefaultMapLookupFirstCopyMode8() {
        Assert.assertEquals(147, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 0, (byte) 8));
        Assert.assertEquals(148, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 4, (byte) 8));
        Assert.assertEquals(149, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 5, (byte) 8));
        Assert.assertEquals(150, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 6, (byte) 8));
        Assert.assertEquals(151, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 7, (byte) 8));
        Assert.assertEquals(152, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 8, (byte) 8));
        Assert.assertEquals(153, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 9, (byte) 8));
        Assert.assertEquals(154, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 10, (byte) 8));
        Assert.assertEquals(155, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 11, (byte) 8));
        Assert.assertEquals(156, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 12, (byte) 8));
        Assert.assertEquals(157, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 13, (byte) 8));
        Assert.assertEquals(158, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 14, (byte) 8));
        Assert.assertEquals(159, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 15, (byte) 8));
        Assert.assertEquals(160, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 16, (byte) 8));
        Assert.assertEquals(161, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 17, (byte) 8));
        Assert.assertEquals(162, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 18, (byte) 8));
    }

    @Test
    public void DefaultMapLookupFirstCopyInvalid() {
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 3, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 3, (byte) 3));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte) 255, (byte) 0));
    }

    @Test
    public void DefaultMapLookupSecondNoop() {
        // The second opcode table does not store entries for NOOP instructions.
        // Just make sure that a NOOP does not crash the lookup code.
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_NOOP, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_NOOP, (byte) 0, (byte) 255));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_NOOP, (byte) 255, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_NOOP, (byte) 255, (byte) 255));
    }

    @Test
    public void DefaultMapLookupSecondAdd() {
        Assert.assertEquals(247, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(248, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 36, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(249, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 52, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(250, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 68, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(251, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 84, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(252, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 100, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(253, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 116, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(254, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 132, VCD_ADD, (byte) 1, (byte) 0));
        Assert.assertEquals(255, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 148, VCD_ADD, (byte) 1, (byte) 0));
        // Value of "mode" should not matter
        Assert.assertEquals(247, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_ADD, (byte) 1, (byte) 2));
        Assert.assertEquals(247, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_ADD, (byte) 1, (byte) 255));
        // Only valid 2nd ADD opcode has size 1
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_ADD, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_ADD, (byte) 0, (byte) 255));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_ADD, (byte) 255, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 0, VCD_ADD, (byte) 1, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 1, VCD_ADD, (byte) 1, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 247, VCD_ADD, (byte) 1, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 255, VCD_ADD, (byte) 1, (byte) 0));
    }

    @Test
    public void DefaultMapLookupSecondRun() {
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 0, VCD_RUN, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_RUN, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_RUN, (byte) 0, (byte) 255));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_RUN, (byte) 255, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 20, VCD_RUN, (byte) 255, (byte) 255));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 255, VCD_RUN, (byte) 0, (byte) 0));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode0() {
        Assert.assertEquals(163, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 0));
        Assert.assertEquals(164, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 5, (byte) 0));
        Assert.assertEquals(165, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 6, (byte) 0));
        Assert.assertEquals(166, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 0));
        Assert.assertEquals(167, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 5, (byte) 0));
        Assert.assertEquals(168, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 6, (byte) 0));
        Assert.assertEquals(169, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 0));
        Assert.assertEquals(170, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 5, (byte) 0));
        Assert.assertEquals(171, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 6, (byte) 0));
        Assert.assertEquals(172, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 0));
        Assert.assertEquals(173, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 5, (byte) 0));
        Assert.assertEquals(174, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 6, (byte) 0));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode1() {
        Assert.assertEquals(175, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 1));
        Assert.assertEquals(176, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 5, (byte) 1));
        Assert.assertEquals(177, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 6, (byte) 1));
        Assert.assertEquals(178, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 1));
        Assert.assertEquals(179, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 5, (byte) 1));
        Assert.assertEquals(180, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 6, (byte) 1));
        Assert.assertEquals(181, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 1));
        Assert.assertEquals(182, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 5, (byte) 1));
        Assert.assertEquals(183, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 6, (byte) 1));
        Assert.assertEquals(184, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 1));
        Assert.assertEquals(185, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 5, (byte) 1));
        Assert.assertEquals(186, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 6, (byte) 1));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode2() {
        Assert.assertEquals(187, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 2));
        Assert.assertEquals(188, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 5, (byte) 2));
        Assert.assertEquals(189, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 6, (byte) 2));
        Assert.assertEquals(190, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 2));
        Assert.assertEquals(191, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 5, (byte) 2));
        Assert.assertEquals(192, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 6, (byte) 2));
        Assert.assertEquals(193, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 2));
        Assert.assertEquals(194, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 5, (byte) 2));
        Assert.assertEquals(195, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 6, (byte) 2));
        Assert.assertEquals(196, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 2));
        Assert.assertEquals(197, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 5, (byte) 2));
        Assert.assertEquals(198, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 6, (byte) 2));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode3() {
        Assert.assertEquals(199, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 3));
        Assert.assertEquals(200, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 5, (byte) 3));
        Assert.assertEquals(201, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 6, (byte) 3));
        Assert.assertEquals(202, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 3));
        Assert.assertEquals(203, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 5, (byte) 3));
        Assert.assertEquals(204, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 6, (byte) 3));
        Assert.assertEquals(205, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 3));
        Assert.assertEquals(206, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 5, (byte) 3));
        Assert.assertEquals(207, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 6, (byte) 3));
        Assert.assertEquals(208, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 3));
        Assert.assertEquals(209, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 5, (byte) 3));
        Assert.assertEquals(210, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 6, (byte) 3));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode4() {
        Assert.assertEquals(211, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 4));
        Assert.assertEquals(212, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 5, (byte) 4));
        Assert.assertEquals(213, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 6, (byte) 4));
        Assert.assertEquals(214, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 4));
        Assert.assertEquals(215, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 5, (byte) 4));
        Assert.assertEquals(216, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 6, (byte) 4));
        Assert.assertEquals(217, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 4));
        Assert.assertEquals(218, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 5, (byte) 4));
        Assert.assertEquals(219, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 6, (byte) 4));
        Assert.assertEquals(220, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 4));
        Assert.assertEquals(221, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 5, (byte) 4));
        Assert.assertEquals(222, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 6, (byte) 4));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode5() {
        Assert.assertEquals(223, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 5));
        Assert.assertEquals(224, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 5, (byte) 5));
        Assert.assertEquals(225, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 6, (byte) 5));
        Assert.assertEquals(226, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 5));
        Assert.assertEquals(227, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 5, (byte) 5));
        Assert.assertEquals(228, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 6, (byte) 5));
        Assert.assertEquals(229, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 5));
        Assert.assertEquals(230, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 5, (byte) 5));
        Assert.assertEquals(231, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 6, (byte) 5));
        Assert.assertEquals(232, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 5));
        Assert.assertEquals(233, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 5, (byte) 5));
        Assert.assertEquals(234, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 6, (byte) 5));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode6() {
        Assert.assertEquals(235, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 6));
        Assert.assertEquals(236, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 6));
        Assert.assertEquals(237, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 6));
        Assert.assertEquals(238, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 6));
        Assert.assertEquals(239, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 7));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode7() {
        Assert.assertEquals(240, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 7));
        Assert.assertEquals(241, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 7));
        Assert.assertEquals(242, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 7));
    }

    @Test
    public void DefaultMapLookupSecondCopyMode8() {
        Assert.assertEquals(243, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 4, (byte) 8));
        Assert.assertEquals(244, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 3, VCD_COPY, (byte) 4, (byte) 8));
        Assert.assertEquals(245, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 4, VCD_COPY, (byte) 4, (byte) 8));
        Assert.assertEquals(246, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 5, VCD_COPY, (byte) 4, (byte) 8));
    }

    @Test
    public void DefaultMapLookupSecondCopyInvalid() {
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 0, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 255, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 2, VCD_COPY, (byte) 255, (byte) 255));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 0, VCD_COPY, (byte) 4, (byte) 0));
        assertEquals(kNoOpcode, VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte) 255, VCD_COPY, (byte) 4, (byte) 0));
    }

    @Test
    public void ExerciseTableLookupTest() {
        // This value is designed so that the total number of inst values and modes
        // will equal 8 (VCD_NOOP, VCD_ADD, VCD_RUN, VCD_COPY modes 0 - 4).
        // Eight combinations of inst and mode, times two possible size values,
        // squared (because there are two instructions per opcode), makes
        // exactly 256 possible instruction combinations, which fits kCodeTableSize
        // (the number of opcodes in the table.)
        final int kLastExerciseMode = 4;

        // Set up the test
        VCDiffCodeTableData g_exercise_code_table_ = new VCDiffCodeTableData();
        int opcode = 0;
        for (byte inst_mode1 = 0; inst_mode1 <= VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode1) {
            byte inst1 = inst_mode1;
            byte mode1 = 0;
            if (inst_mode1 > VCD_COPY) {
                inst1 = VCD_COPY;
                mode1 = (byte) (inst_mode1 - VCD_COPY);
            }
            for (byte inst_mode2 = 0; inst_mode2 <= VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode2) {
                byte inst2 = inst_mode2;
                byte mode2 = 0;
                if (inst_mode2 > VCD_COPY) {
                    inst2 = VCD_COPY;
                    mode2 = (byte) (inst_mode2 - VCD_COPY);
                }

                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 0, inst2, mode2, (byte) 0, opcode++);
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 0, inst2, mode2, (byte) 255, opcode++);
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 255, inst2, mode2, (byte) 0, opcode++);
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 255, inst2, mode2, (byte) 255, opcode++);
            }
        }

        assertEquals(VCDiffCodeTableData.kCodeTableSize, opcode);
        Assert.assertTrue(g_exercise_code_table_.Validate((short) kLastExerciseMode));

        VCDiffInstructionMap exercise_map = new VCDiffInstructionMap(g_exercise_code_table_, (byte) kLastExerciseMode);

        opcode = 0;

        // This loop has the same bounds as the one in SetUpTestCase.
        // Look up each instruction type and make sure it returns
        // the proper opcode.
        for (byte inst_mode1 = 0; inst_mode1 <= VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode1) {
            byte inst1 = inst_mode1;
            byte mode1 = 0;
            if (inst_mode1 > VCD_COPY) {
                inst1 = VCD_COPY;
                mode1 = (byte) (inst_mode1 - VCD_COPY);
            }

            for (byte inst_mode2 = 0; inst_mode2 <= VCD_LAST_INSTRUCTION_TYPE + kLastExerciseMode; ++inst_mode2) {
                byte inst2 = inst_mode2;
                byte mode2 = 0;
                if (inst_mode2 > VCD_COPY) {
                    inst2 = VCD_COPY;
                    mode2 = (byte) (inst_mode2 - VCD_COPY);
                }

                if (inst2 == VCD_NOOP) {
                    VerifyExerciseFirstInstruction(exercise_map, g_exercise_code_table_, (byte) opcode, inst1, (byte) 0, mode1);
                    VerifyExerciseFirstInstruction(exercise_map, g_exercise_code_table_, (byte) (opcode + 2), inst1, ((inst1 == VCD_NOOP) ? (byte) 0 : (byte) 255), mode1);
                } else if (inst1 != VCD_NOOP) {
                    VerifyExerciseSecondInstruction(exercise_map, (byte) opcode, inst1, (byte) 0, mode1, inst2, (byte) 0, mode2);
                    VerifyExerciseSecondInstruction(exercise_map, (byte) (opcode + 1), inst1, (byte) 0, mode1, inst2, (byte) 255, mode2);
                    VerifyExerciseSecondInstruction(exercise_map, (byte) (opcode + 2), inst1, (byte) 255, mode1, inst2, (byte) 0, mode2);
                    VerifyExerciseSecondInstruction(exercise_map, (byte) (opcode + 3), inst1, (byte) 255, mode1, inst2, (byte) 255, mode2);
                }

                opcode += 4;
            }
        }
    }
}
