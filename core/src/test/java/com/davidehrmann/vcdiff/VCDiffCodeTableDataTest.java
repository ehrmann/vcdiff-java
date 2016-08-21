package com.davidehrmann.vcdiff;

import org.junit.Assert;
import org.junit.Test;

public class VCDiffCodeTableDataTest {
    private VCDiffCodeTableData code_table_data_ = VCDiffCodeTableData.kDefaultCodeTableData.clone();

    private static void AddExerciseOpcode(VCDiffCodeTableData code_table, byte inst1, byte mode1, byte size1, byte inst2, byte mode2, byte size2, int opcode) {
        code_table.inst1[opcode] = inst1;
        code_table.mode1[opcode] = mode1;
        code_table.size1[opcode] = (inst1 == VCDiffCodeTableData.VCD_NOOP) ? 0 : size1;
        code_table.inst2[opcode] = inst2;
        code_table.mode2[opcode] = mode2;
        code_table.size2[opcode] = (inst2 == VCDiffCodeTableData.VCD_NOOP) ? 0 : size2;
    }

    private void VerifyInstruction(int opcode, byte inst, byte size, byte mode) {
        Assert.assertEquals(inst, code_table_data_.inst1[opcode]);
        Assert.assertEquals(size, code_table_data_.size1[opcode]);
        Assert.assertEquals(mode, code_table_data_.mode1[opcode]);
        Assert.assertEquals(VCDiffCodeTableData.VCD_NOOP, code_table_data_.inst2[opcode]);
        Assert.assertEquals(0, code_table_data_.size2[opcode]);
        Assert.assertEquals(0, code_table_data_.mode2[opcode]);
    }

    // All possible combinations of inst and mode should have an opcode with size 0.
    @Test
    public void MissingCopyMode() {
        VerifyInstruction(131, VCDiffCodeTableData.VCD_COPY, (byte) 0, (byte) 7);
        code_table_data_.size1[131] = (byte) 0xFF;
        // Now there is no opcode expressing COPY with mode 7 and size 0.
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void MissingAdd() {
        VerifyInstruction(1, VCDiffCodeTableData.VCD_ADD, (byte) 0, (byte) 0);
        code_table_data_.size1[1] = (byte) 0xFF;  // add size 0 => size 255
        // Now there is no opcode expressing ADD with size 0.
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void MissingRun() {
        VerifyInstruction(0, VCDiffCodeTableData.VCD_RUN, (byte) 0, (byte) 0);
        code_table_data_.size1[0] = (byte) 0xFF;  // run size 0 => size 255
        // Now there is no opcode expressing RUN with size 0.
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void BadOpcode() {
        VerifyInstruction(0, VCDiffCodeTableData.VCD_RUN, (byte) 0, (byte) 0);
        code_table_data_.inst1[0] = VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE + 1;
        Assert.assertFalse(code_table_data_.Validate());
        code_table_data_.inst1[0] = (byte) 0xFF;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void BadMode() {
        VerifyInstruction(131, VCDiffCodeTableData.VCD_COPY, (byte) 0, (byte) 7);
        code_table_data_.mode1[131] = (byte) (VCDiffAddressCache.DefaultLastMode() + 1);
        Assert.assertFalse(code_table_data_.Validate());
        code_table_data_.mode1[131] = (byte) 0xFF;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void AddWithNonzeroMode() {
        VerifyInstruction(1, VCDiffCodeTableData.VCD_ADD, (byte) 0, (byte) 0);
        code_table_data_.mode1[1] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void RunWithNonzeroMode() {
        VerifyInstruction(0, VCDiffCodeTableData.VCD_RUN, (byte) 0, (byte) 0);
        code_table_data_.mode1[0] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void NoOpWithNonzeroMode() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        code_table_data_.inst1[20] = VCDiffCodeTableData.VCD_NOOP;
        code_table_data_.mode1[20] = 0;
        code_table_data_.size1[20] = 0;
        Assert.assertTrue(code_table_data_.Validate());
        code_table_data_.mode1[20] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void NoOpWithNonzeroSize() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        code_table_data_.inst1[20] = VCDiffCodeTableData.VCD_NOOP;
        code_table_data_.mode1[20] = 0;
        code_table_data_.size1[20] = 0;
        Assert.assertTrue(code_table_data_.Validate());
        code_table_data_.size1[20] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void BadSecondOpcode() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        code_table_data_.inst2[20] = VCDiffCodeTableData.VCD_LAST_INSTRUCTION_TYPE + 1;
        Assert.assertFalse(code_table_data_.Validate());
        code_table_data_.inst2[20] = (byte) 0xFF;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void BadSecondMode() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        code_table_data_.inst2[20] = VCDiffCodeTableData.VCD_COPY;
        Assert.assertTrue(code_table_data_.Validate());
        code_table_data_.mode2[20] = (byte) (VCDiffAddressCache.DefaultLastMode() + 1);
        Assert.assertFalse(code_table_data_.Validate());
        code_table_data_.mode2[20] = (byte) 0xFF;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void AddSecondWithNonzeroMode() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        code_table_data_.inst2[20] = VCDiffCodeTableData.VCD_ADD;
        Assert.assertTrue(code_table_data_.Validate());
        code_table_data_.mode2[20] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void RunSecondWithNonzeroMode() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        code_table_data_.inst2[20] = VCDiffCodeTableData.VCD_RUN;
        Assert.assertTrue(code_table_data_.Validate());
        code_table_data_.mode2[20] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void SecondNoOpWithNonzeroMode() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        Assert.assertEquals(VCDiffCodeTableData.VCD_NOOP, code_table_data_.inst2[20]);
        code_table_data_.mode2[20] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void SecondNoOpWithNonzeroSize() {
        VerifyInstruction(20, VCDiffCodeTableData.VCD_COPY, (byte) 4, (byte) 0);
        Assert.assertEquals(VCDiffCodeTableData.VCD_NOOP, code_table_data_.inst2[20]);
        code_table_data_.size2[20] = 1;
        Assert.assertFalse(code_table_data_.Validate());
    }

    @Test
    public void ValidateExerciseCodeTable() {
        // This value is designed so that the total number of inst values and modes
        // will equal 8 (VCD_NOOP, VCD_ADD, VCD_RUN, VCD_COPY modes 0 - 4).
        // Eight combinations of inst and mode, times two possible size values,
        // squared (because there are two instructions per opcode), makes
        // exactly 256 possible instruction combinations, which fits kCodeTableSize
        // (the number of opcodes in the table.)
        final short kLastExerciseMode = 4;

        // A code table that exercises as many combinations as possible:
        // 2 instructions, each is a NOOP, ADD, RUN, or one of 5 copy modes
        // (== 8 total combinations of inst and mode), and each has
        // size == 0 or 255 (2 possibilities.)
        VCDiffCodeTableData g_exercise_code_table_ = new VCDiffCodeTableData();

        int opcode = 0;
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
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 0, inst2, mode2, (byte) 0, opcode++);
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 0, inst2, mode2, (byte) 255, opcode++);
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 255, inst2, mode2, (byte) 0, opcode++);
                AddExerciseOpcode(g_exercise_code_table_, inst1, mode1, (byte) 255, inst2, mode2, (byte) 255, opcode++);
            }
        }

        Assert.assertEquals(VCDiffCodeTableData.kCodeTableSize, opcode);

        Assert.assertTrue(VCDiffCodeTableData.kDefaultCodeTableData.Validate());
        Assert.assertTrue(g_exercise_code_table_.Validate(kLastExerciseMode));
    }
}
