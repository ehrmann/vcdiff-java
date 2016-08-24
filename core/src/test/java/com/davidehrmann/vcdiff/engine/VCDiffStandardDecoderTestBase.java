package com.davidehrmann.vcdiff.engine;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriter.VCD_SOURCE;

public class VCDiffStandardDecoderTestBase extends VCDiffDecoderTest {
    protected static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x79,  // Length of the delta encoding
            FirstByteOfStringLength(kExpectedTarget),  // Size of the target window
            SecondByteOfStringLength(kExpectedTarget),
            0x00,  // Delta_indicator (no compression)
            0x64,  // length of data for ADDs and RUNs
            0x0C,  // length of instructions section
            0x03  // length of addresses for COPYs
    };
    protected static final byte[] kWindowBody = {
            // Data for ADDs: 1st section (length 61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            // Data for ADDs: 2nd section (length 2)
            'h', 'r',
            // Data for ADDs: 3rd section (length 9)
            'W', 'h', 'a', 't', ' ',
            'I', ' ', 't', 'e',
            // Data for RUN: 4th section (length 1)
            'l',
            // Data for ADD: 4th section (length 27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n',
            // Instructions and sizes (length 13)
            0x13,         // VCD_COPY mode VCD_SELF, size 0
            0x1C,         // Size of COPY (28)
            0x01,         // VCD_ADD size 0
            0x3D,         // Size of ADD (61)
            0x23,         // VCD_COPY mode VCD_HERE, size 0
            0x2C,         // Size of COPY (44)
            (byte) 0xCB,  // VCD_ADD size 2 + VCD_COPY mode NEAR(1), size 5
            0x0A,         // VCD_ADD size 9
            0x00,         // VCD_RUN size 0
            0x02,         // Size of RUN (2)
            0x01,         // VCD_ADD size 0
            0x1B,         // Size of ADD (27)
            // Addresses for COPYs (length 3)
            0x00,         // Start of dictionary
            0x58,         // HERE mode address for 2nd copy (27+61 back from here_address)
            0x2D          // NEAR(1) mode address for 2nd copy (45 after prior address)
    };

    protected VCDiffStandardDecoderTestBase() {
        UseStandardFileHeader();
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
    }
}
