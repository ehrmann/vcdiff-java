package com.davidehrmann.vcdiff.engine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableData.*;
import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriterImpl.VCD_CHECKSUM;
import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriterImpl.VCD_SOURCE;
import static org.junit.Assert.*;

public class VCDiffCodeTableWriterImplTest {

    // This value is designed so that the total number of inst values and modes
    // will equal 8 (VCD_NOOP, VCD_ADD, VCD_RUN, VCD_COPY modes 0 - 4).
    // Eight combinations of inst and mode, times two possible size values,
    // squared (because there are two instructions per opcode), makes
    // exactly 256 possible instruction combinations, which fits kCodeTableSize
    // (the number of opcodes in the table.)
    protected static final short kLastExerciseMode = 4;

    protected static final Charset US_ASCII = Charset.forName("US-ASCII");

    // Destination for VCDiffCodeTableWriter::output()
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

    // A code table that exercises as many combinations as possible:
    // 2 instructions, each is a NOOP, ADD, RUN, or one of 5 copy modes
    // (== 8 total combinations of inst and mode), and each has
    // size == 0 or 255 (2 possibilities.)
    protected final VCDiffCodeTableData g_exercise_code_table_ = new VCDiffCodeTableData();

    // The code table writer for standard encoding, default code table.
    protected final VCDiffCodeTableWriterImpl standard_writer = new VCDiffCodeTableWriterImpl(false);

    // The code table writer for interleaved encoding, default code table.
    protected final VCDiffCodeTableWriterImpl interleaved_writer = new VCDiffCodeTableWriterImpl(true);

    // The code table writer corresponding to g_exercise_code_table_ (interleaved encoding).
    protected final VCDiffCodeTableWriterImpl exercise_writer = new VCDiffCodeTableWriterImpl(true, VCDiffAddressCache.kDefaultNearCacheSize, VCDiffAddressCache.kDefaultSameCacheSize, g_exercise_code_table_, kLastExerciseMode);

    static void AddExerciseOpcode(VCDiffCodeTableData codeTable, byte inst1, byte mode1, byte size1, byte inst2, byte mode2, byte size2, int opcode) {
        codeTable.inst1[opcode] = inst1;
        codeTable.mode1[opcode] = mode1;
        codeTable.size1[opcode] = (inst1 == VCD_NOOP) ? 0 : size1;
        codeTable.inst2[opcode] = inst2;
        codeTable.mode2[opcode] = mode2;
        codeTable.size2[opcode] = (inst2 == VCD_NOOP) ? 0 : size2;
    }

    @Before
    public void SetUpTestCase() {
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
        // This is a CHECK rather than an EXPECT because it validates only
        // the logic of the test, not of the code being tested.
        assertEquals(VCDiffCodeTableData.kCodeTableSize, opcode);

        assertTrue(g_exercise_code_table_.Validate(kLastExerciseMode));
    }

    @Test
    public void WriterOutputWithoutInit() throws IOException {
        standard_writer.output(out);
        assertEquals(0, out.size());
    }

    @Test
    public void WriterEncodeNothing() throws IOException {
        standard_writer.init(0);
        standard_writer.output(out);

        // The writer should know not to append a delta file window
        // if nothing was encoded.
        assertEquals(0, out.size());

        interleaved_writer.init(0x10);
        interleaved_writer.output(out);
        assertEquals(0, out.size());

        exercise_writer.init(0x20);
        exercise_writer.output(out);
        assertEquals(0, out.size());
    }

    @Test
    public void StandardWriterEncodeAdd() throws IOException {
        standard_writer.init(0x11);
        standard_writer.add("foo".getBytes(US_ASCII), 0, 3);
        standard_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE, // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x09,        // Length of the delta encoding
                0x03,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x03,        // length of data for ADDs and RUNs
                0x01,        // length of instructions section
                0x00,        // length of addresses for COPYs
                'f', 'o', 'o',
                0x04,        // ADD(3) opcode
        }, out.toByteArray());
    }

    @Test
    public void ExerciseWriterEncodeAdd() throws IOException {
        exercise_writer.init(0x11);
        exercise_writer.add("foo".getBytes(US_ASCII), 0, 3);
        exercise_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x0A,        // Length of the delta encoding
                0x03,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x05,        // length of instructions section
                0x00,        // length of addresses for COPYs
                0x04,        // Opcode: NOOP + ADD(0)
                0x03,        // Size of ADD (3)
                'f', 'o', 'o'
        }, out.toByteArray());
    }

    @Test
    public void StandardWriterEncodeRun() throws IOException {
        standard_writer.init(0x11);
        standard_writer.run(3, (byte) 'a');
        standard_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x08,        // Length of the delta encoding
                0x03,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x01,        // length of data for ADDs and RUNs
                0x02,        // length of instructions section
                0x00,        // length of addresses for COPYs
                'a',
                0x00,        // RUN(0) opcode
                0x03,        // Size of RUN (3)
        }, out.toByteArray());
    }

    @Test
    public void ExerciseWriterEncodeRun() throws IOException {
        exercise_writer.init(0x11);
        exercise_writer.run(3, (byte) 'a');
        exercise_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x08,        // Length of the delta encoding
                0x03,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x03,        // length of instructions section
                0x00,        // length of addresses for COPYs
                0x08,        // Opcode: NOOP + RUN(0)
                0x03,        // Size of RUN (3)
                'a',
        }, out.toByteArray());
    }

    @Test
    public void StandardWriterEncodeCopy() throws IOException {
        standard_writer.init(0x11);
        standard_writer.copy(2, 8);
        standard_writer.copy(2, 8);
        standard_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x09,        // Length of the delta encoding
                0x10,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x02,        // length of instructions section
                0x02,        // length of addresses for COPYs
                0x18,        // COPY mode SELF, size 8
                0x78,        // COPY mode SAME(0), size 8
                0x02,        // COPY address (2)
                0x02,        // COPY address (2)
        }, out.toByteArray());
    }

    // The exercise code table can't be used to test how the code table
    // writer encodes COPY instructions because the code table writer
    // always uses the default cache sizes, which exceed the maximum mode
    // used in the exercise table.
    @Test
    public void InterleavedWriterEncodeCopy() throws IOException {
        interleaved_writer.init(0x11);
        interleaved_writer.copy(2, 8);
        interleaved_writer.copy(2, 8);
        interleaved_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x09,        // Length of the delta encoding
                0x10,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x04,        // length of instructions section
                0x00,        // length of addresses for COPYs
                0x18,        // COPY mode SELF, size 8
                0x02,        // COPY address (2)
                0x78,        // COPY mode SAME(0), size 8
                0x02,        // COPY address (2)
        }, out.toByteArray());
    }

    @Test
    public void StandardWriterEncodeCombo() throws IOException {
        standard_writer.init(0x11);
        standard_writer.add("rayo".getBytes(US_ASCII), 0, 4);
        standard_writer.copy(2, 5);
        standard_writer.copy(0, 4);
        standard_writer.add("X".getBytes(US_ASCII), 0, 1);
        standard_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x0E,        // Length of the delta encoding
                0x0E,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x05,        // length of data for ADDs and RUNs
                0x02,        // length of instructions section
                0x02,        // length of addresses for COPYs
                'r', 'a', 'y', 'o', 'X',
                (byte) 0xAD,    // Combo: add size 4 + COPY mode SELF, size 5
                (byte) 0xFD,    // Combo: COPY mode SAME(0), size 4 + add size 1
                0x02,        // COPY address (2)
                0x00,        // COPY address (0)
        }, out.toByteArray());
    }

    @Test
    public void InterleavedWriterEncodeCombo() throws IOException {
        interleaved_writer.init(0x11);
        interleaved_writer.add("rayo".getBytes(US_ASCII), 0, 4);
        interleaved_writer.copy(2, 5);
        interleaved_writer.copy(0, 4);
        interleaved_writer.add("X".getBytes(US_ASCII), 0, 1);
        interleaved_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x0E,        // Length of the delta encoding
                0x0E,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x09,        // length of instructions section
                0x00,        // length of addresses for COPYs
                (byte) 0xAD,    // Combo: add size 4 + COPY mode SELF, size 5
                'r', 'a', 'y', 'o',
                0x02,        // COPY address (2)
                (byte) 0xFD,    // Combo: COPY mode SAME(0), size 4 + add size 1
                0x00,        // COPY address (0)
                'X',
        }, out.toByteArray());
    }

    @Test
    public void InterleavedWriterEncodeComboWithChecksum() throws IOException {
        interleaved_writer.init(0x11);
        final int checksum = 0xFFFFFFFF;  // would be negative if signed
        interleaved_writer.addChecksum(checksum);
        interleaved_writer.add("rayo".getBytes(US_ASCII), 0, 4);
        interleaved_writer.copy(2, 5);
        interleaved_writer.copy(0, 4);
        interleaved_writer.add("X".getBytes(US_ASCII), 0, 1);
        interleaved_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE | VCD_CHECKSUM,        // Win_Indicator
                0x11,        // Source segment size: dictionary length
                0x00,        // Source segment position: start of dictionary
                0x13,        // Length of the delta encoding
                0x0E,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x09,        // length of instructions section
                0x00,        // length of addresses for COPYs
                (byte) 0x8F,    // checksum byte 1
                (byte) 0xFF,    // checksum byte 2
                (byte) 0xFF,    // checksum byte 3
                (byte) 0xFF,    // checksum byte 4
                0x7F,        // checksum byte 5
                (byte) 0xAD,    // Combo: add size 4 + COPY mode SELF, size 5
                'r', 'a', 'y', 'o',
                0x02,        // COPY address (2)
                (byte) 0xFD,    // Combo: COPY mode SAME(0), size 4 + add size 1
                0x00,        // COPY address (0)
                'X',
        }, out.toByteArray());
    }

    @Test
    public void ReallyBigDictionary() throws IOException {
        interleaved_writer.init(0x3FFFFFFF);
        interleaved_writer.copy(2, 8);
        interleaved_writer.copy(0x3FFFFFFE, 8);
        interleaved_writer.output(out);

        assertArrayEquals(new byte[]{
                VCD_SOURCE,    // Win_Indicator: VCD_SOURCE (dictionary)
                (byte) 0x83,    // Source segment size: dictionary length (1)
                (byte) 0xFF,    // Source segment size: dictionary length (2)
                (byte) 0xFF,    // Source segment size: dictionary length (3)
                (byte) 0xFF,    // Source segment size: dictionary length (4)
                0x7F,        // Source segment size: dictionary length (5)
                0x00,        // Source segment position: start of dictionary
                0x09,        // Length of the delta encoding
                0x10,        // Size of the target window
                0x00,        // Delta_indicator (no compression)
                0x00,        // length of data for ADDs and RUNs
                0x04,        // length of instructions section
                0x00,        // length of addresses for COPYs
                0x18,        // COPY mode SELF, size 8
                0x02,        // COPY address (2)
                0x28,        // COPY mode HERE, size 8
                0x09,        // COPY address (9)
        }, out.toByteArray());
    }

    @Test(expected = IllegalStateException.class)
    public void WriterAddWithoutInit() {
        standard_writer.add("Hello".getBytes(US_ASCII), 0, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void WriterRunWithoutInit() {
        standard_writer.run(3, (byte) 'a');
    }

    @Test(expected = IllegalStateException.class)
    public void WriterCopyWithoutInit() {
        standard_writer.copy(6, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void DictionaryTooBig() {
        try {
            interleaved_writer.init(0x7FFFFFFF);
        } catch (RuntimeException e) {
            Assert.fail();
        }

        interleaved_writer.copy(2, 8);
        interleaved_writer.copy(0x7FFFFFFE, 8);
    }
}
