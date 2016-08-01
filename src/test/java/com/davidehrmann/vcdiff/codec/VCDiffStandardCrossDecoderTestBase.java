package com.davidehrmann.vcdiff.codec;

import static com.davidehrmann.vcdiff.VCDiffCodeTableWriter.VCD_SOURCE;

public abstract class VCDiffStandardCrossDecoderTestBase extends VCDiffDecoderTest {
    protected static final byte[] kExpectedTarget = "Spiders in his hair.\nSpiders in the air.\n".getBytes(US_ASCII);
    private static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x15,  // Length of the delta encoding
            StringLengthAsByte(kExpectedTarget),  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x07,  // length of data for ADDs and RUNs
            0x06,  // length of instructions section
            0x03   // length of addresses for COPYs
    };
    private static final byte[] kWindowBody = {
            // Data for ADD (length 7)
            'S', 'p', 'i', 'd', 'e', 'r', 's',
            // Instructions and sizes (length 6)
            0x01,  // VCD_ADD size 0
            0x07,  // Size of ADD (7)
            0x23,  // VCD_COPY mode VCD_HERE, size 0
            0x19,  // Size of COPY (25)
            0x14,  // VCD_COPY mode VCD_SELF, size 4
            0x25,  // VCD_COPY mode VCD_HERE, size 5
            // Addresses for COPYs (length 3)
            0x15,  // HERE mode address for 1st copy (21 back from here_address)
            0x06,  // SELF mode address for 2nd copy
            0x14   // HERE mode address for 3rd copy
    };

    protected VCDiffStandardCrossDecoderTestBase() {
        UseStandardFileHeader();
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
        expected_target_ = kExpectedTarget.clone();
    }
}
