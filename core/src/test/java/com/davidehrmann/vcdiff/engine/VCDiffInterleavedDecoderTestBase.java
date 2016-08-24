package com.davidehrmann.vcdiff.engine;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriter.VCD_SOURCE;

public abstract class VCDiffInterleavedDecoderTestBase extends VCDiffDecoderTest {
    private static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x79,  // Length of the delta encoding
            FirstByteOfStringLength(kExpectedTarget),  // Size of the target window
            SecondByteOfStringLength(kExpectedTarget),
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs (unused)
            0x73,  // length of interleaved section
            0x00  // length of addresses for COPYs (unused)
    };
    private static final byte[] kWindowBody = {
            0x13,  // VCD_COPY mode VCD_SELF, size 0
            0x1C,  // Size of COPY (28)
            0x00,  // Address of COPY: Start of dictionary
            0x01,  // VCD_ADD size 0
            0x3D,  // Size of ADD (61)
            // Data for ADD (length 61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            0x23,  // VCD_COPY mode VCD_HERE, size 0
            0x2C,  // Size of COPY (44)
            0x58,  // HERE mode address (27+61 back from here_address)
            (byte) 0xCB,  // VCD_ADD size 2 + VCD_COPY mode NEAR(1), size 5
            // Data for ADDs: 2nd section (length 2)
            'h', 'r',
            0x2D,  // NEAR(1) mode address (45 after prior address)
            0x0A,  // VCD_ADD size 9
            // Data for ADDs: 3rd section (length 9)
            'W', 'h', 'a', 't', ' ',
            'I', ' ', 't', 'e',
            0x00,  // VCD_RUN size 0
            0x02,  // Size of RUN (2)
            // Data for RUN: 4th section (length 1)
            'l',
            0x01,  // VCD_ADD size 0
            0x1B,  // Size of ADD (27)
            // Data for ADD: 4th section (length 27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n'
    };

    protected VCDiffInterleavedDecoderTestBase() {
        UseInterleavedFileHeader();
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
    }
}
