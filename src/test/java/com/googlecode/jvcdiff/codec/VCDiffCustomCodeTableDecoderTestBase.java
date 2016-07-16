package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.VCDiffCodeTableData;
import com.googlecode.jvcdiff.codec.VCDiffInterleavedDecoderTestBase;

import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_ADD;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_RUN;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;

public abstract class VCDiffCustomCodeTableDecoderTestBase extends VCDiffInterleavedDecoderTestBase {
    protected static final byte[] kFileHeader = {
            (byte) 0xD6,  // 'V' | 0x80
            (byte) 0xC3,  // 'C' | 0x80
            (byte) 0xC4,  // 'D' | 0x80
            'S',          // SDCH version code
            0x02          // Hdr_Indicator: Use custom code table
    };

    // Make a custom code table that includes exactly the instructions we need
    // to encode the first test's data without using any explicit length values.
    // Be careful not to replace any existing opcodes that have size 0,
    // to ensure that the custom code table is valid (can express all possible
    // values of inst (also known as instruction type) and mode with size 0.)
    // This encoding uses interleaved format, which is easier to read.
    //
    // Here are the changes to the standard code table:
    // ADD size 2 (opcode 3) => RUN size 2 (inst1[3] = VCD_RUN)
    // ADD size 16 (opcode 17) => ADD size 27 (size1[17] = 27)
    // ADD size 17 (opcode 18) => ADD size 61 (size1[18] = 61)
    // COPY mode 0 size 18 (opcode 34) => COPY mode 0 size 28 (size1[34] = 28)
    // COPY mode 1 size 18 (opcode 50) => COPY mode 1 size 44 (size1[50] = 44)
    //
    protected static final byte[] kEncodedCustomCodeTable = {
            (byte) 0xD6,  // 'V' | 0x80
            (byte) 0xC3,  // 'C' | 0x80
            (byte) 0xC4,  // 'D' | 0x80
            'S',   // SDCH version code
            0x00,  // Hdr_Indicator: no custom code table, no compression
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            // TODO: should be 1536
            (byte) ((new VCDiffCodeTableData().getBytes().length >> 7) | 0x80),  // First byte of table length
            (byte) (new VCDiffCodeTableData().getBytes().length & 0x7F),  // Second byte of table length
            0x00,  // Source segment position: start of default code table
            0x1F,  // Length of the delta encoding
            (byte) ((new VCDiffCodeTableData().getBytes().length >> 7) | 0x80),  // First byte of table length
            (byte) (new VCDiffCodeTableData().getBytes().length & 0x7F),  // Second byte of table length
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs (unused)
            0x19,  // length of interleaved section
            0x00,  // length of addresses for COPYs (unused)
            0x05,  // VCD_ADD size 4
            // Data for ADD (length 4)
            VCD_RUN, VCD_ADD, VCD_ADD, VCD_RUN,
            0x13,  // VCD_COPY mode VCD_SELF size 0
            (byte) 0x84,  // Size of copy: upper bits (512 - 4 + 17 = 525)
            0x0D,  // Size of copy: lower bits
            0x04,  // Address of COPY
            0x03,  // VCD_ADD size 2
            // Data for ADD (length 2)
            0x1B, 0x3D,
            0x3F,  // VCD_COPY mode VCD_NEAR(0) size 15
            (byte) 0x84,  // Address of copy: upper bits (525 + 2 = 527)
            0x0F,  // Address of copy: lower bits
            0x02,  // VCD_ADD size 1
            // Data for ADD (length 1)
            0x1C,
            0x4F,  // VCD_COPY mode VCD_NEAR(1) size 15
            0x10,  // Address of copy
            0x02,  // VCD_ADD size 1
            // Data for ADD (length 1)
            0x2C,
            0x53,  // VCD_COPY mode VCD_NEAR(2) size 0
            (byte) 0x87,  // Size of copy: upper bits (256 * 4 - 51 = 973)
            0x4D,  // Size of copy: lower bits
            0x10   // Address of copy
    };

    // This is similar to VCDiffInterleavedDecoderTest, but uses the custom code
    // table to eliminate the need to explicitly encode instruction sizes.
    // Notice that NEAR(0) mode is used here where NEAR(1) mode was used in
    // VCDiffInterleavedDecoderTest.  This is because the custom code table
    // has the size of the NEAR cache set to 1; only the most recent
    // COPY instruction is available.  This will also be a test of
    // custom cache sizes.
    protected static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x74,  // Length of the delta encoding
            FirstByteOfStringLength(kExpectedTarget),  // Size of the target window
            SecondByteOfStringLength(kExpectedTarget),
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs (unused)
            0x6E,  // length of interleaved section
            0x00   // length of addresses for COPYs (unused)
    };

    protected static final byte[] kWindowBody = {
            0x22,  // VCD_COPY mode VCD_SELF, size 28
            0x00,  // Address of COPY: Start of dictionary
            0x12,  // VCD_ADD size 61
            // Data for ADD (length 61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            0x32,  // VCD_COPY mode VCD_HERE, size 44
            0x58,  // HERE mode address (27+61 back from here_address)
            (byte) 0xBF,  // VCD_ADD size 2 + VCD_COPY mode NEAR(0), size 5
            // Data for ADDs: 2nd section (length 2)
            'h', 'r',
            0x2D,  // NEAR(0) mode address (45 after prior address)
            0x0A,  // VCD_ADD size 9
            // Data for ADDs: 3rd section (length 9)
            'W', 'h', 'a', 't', ' ',
            'I', ' ', 't', 'e',
            0x03,  // VCD_RUN size 2
            // Data for RUN: 4th section (length 1)
            'l',
            0x11,  // VCD_ADD size 27
            // Data for ADD: 4th section (length 27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n'
    };

    protected VCDiffCustomCodeTableDecoderTestBase() {
        delta_file_header_ = ArraysExtra.concat(
                kFileHeader,
                new byte[] {
                        0x01,  // NEAR cache size (custom)
                        0x06,  // SAME cache size (custom)
                },
                kEncodedCustomCodeTable
        );
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
    }
}
