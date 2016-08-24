package com.davidehrmann.vcdiff.engine;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriterImpl.VCD_SOURCE;

public abstract class VCDiffInterleavedCrossDecoderTestBase extends VCDiffStandardCrossDecoderTestBase {
    private static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x15,  // Length of the delta encoding
            StringLengthAsByte(kExpectedTarget),  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x10,  // length of instructions section
            0x00,  // length of addresses for COPYs
    };

    private static final byte[] kWindowBody = {
            0x01,  // VCD_ADD size 0
            0x07,  // Size of ADD (7)
            // Data for ADD (length 7)
            'S', 'p', 'i', 'd', 'e', 'r', 's',
            0x23,  // VCD_COPY mode VCD_HERE, size 0
            0x19,  // Size of COPY (25)
            0x15,  // HERE mode address for 1st copy (21 back from here_address)
            0x14,  // VCD_COPY mode VCD_SELF, size 4
            0x06,  // SELF mode address for 2nd copy
            0x25,  // VCD_COPY mode VCD_HERE, size 5
            0x14   // HERE mode address for 3rd copy
    };

    protected VCDiffInterleavedCrossDecoderTestBase() {
        UseInterleavedFileHeader();
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
    }
}
