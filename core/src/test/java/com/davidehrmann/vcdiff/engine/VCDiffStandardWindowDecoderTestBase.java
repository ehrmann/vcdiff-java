package com.davidehrmann.vcdiff.engine;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriter.VCD_SOURCE;
import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriter.VCD_TARGET;

public abstract class VCDiffStandardWindowDecoderTestBase extends VCDiffDecoderTest {
    static final int kWindow2Size = 61;
    private static final byte[] kWindowBody = {
            // Window 1:
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x08,  // Length of the delta encoding
            0x1C,  // Size of the target window (28)
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x02,  // length of instructions section
            0x01,  // length of addresses for COPYs
            // No data for ADDs and RUNs
            // Instructions and sizes (length 2)
            0x13,  // VCD_COPY mode VCD_SELF, size 0
            0x1C,  // Size of COPY (28)
            // Addresses for COPYs (length 1)
            0x00,  // Start of dictionary
            // Window 2:
            0x00,  // Win_Indicator: No source segment (ADD only)
            0x44,  // Length of the delta encoding
            (byte) kWindow2Size,  // Size of the target window (61)
            0x00,  // Delta_indicator (no compression)
            0x3D,  // length of data for ADDs and RUNs
            0x02,  // length of instructions section
            0x00,  // length of addresses for COPYs
            // Data for ADD (length 61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            // Instructions and sizes (length 2)
            0x01,  // VCD_ADD size 0
            0x3D,  // Size of ADD (61)
            // No addresses for COPYs
            // Window 3:
            VCD_TARGET,  // Win_Indicator: take source from decoded data
            0x59,  // Source segment size: length of data decoded so far
            0x00,  // Source segment position: start of decoded data
            0x08,  // Length of the delta encoding
            0x2C,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x02,  // length of instructions section
            0x01,  // length of addresses for COPYs
            // No data for ADDs and RUNs
            // Instructions and sizes (length 2)
            0x23,  // VCD_COPY mode VCD_HERE, size 0
            0x2C,  // Size of COPY (44)
            // Addresses for COPYs (length 1)
            0x58,  // HERE mode address (27+61 back from here_address)
            // Window 4:
            VCD_TARGET,  // Win_Indicator: take source from decoded data
            0x05,  // Source segment size: only 5 bytes needed for this COPY
            0x2E,  // Source segment position: offset for COPY
            0x09,  // Length of the delta encoding
            0x07,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x02,  // length of data for ADDs and RUNs
            0x01,  // length of instructions section
            0x01,  // length of addresses for COPYs
            // Data for ADD (length 2)
            'h', 'r',
            // Instructions and sizes (length 1)
            (byte) 0xA7,  // VCD_ADD size 2 + VCD_COPY mode SELF size 5
            // Addresses for COPYs (length 1)
            0x00,  // SELF mode address (start of source segment)
            // Window 5:
            0x00,  // Win_Indicator: No source segment (ADD only)
            0x0F,  // Length of the delta encoding
            0x09,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x09,  // length of data for ADDs and RUNs
            0x01,  // length of instructions section
            0x00,  // length of addresses for COPYs
            // Data for ADD (length 9)
            'W', 'h', 'a', 't', ' ', 'I', ' ', 't', 'e',
            // Instructions and sizes (length 1)
            0x0A,       // VCD_ADD size 9
            // No addresses for COPYs
            // Window 6:
            0x00,  // Win_Indicator: No source segment (RUN only)
            0x08,  // Length of the delta encoding
            0x02,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x01,  // length of data for ADDs and RUNs
            0x02,  // length of instructions section
            0x00,  // length of addresses for COPYs
            // Data for RUN (length 1)
            'l',
            // Instructions and sizes (length 2)
            0x00,  // VCD_RUN size 0
            0x02,  // Size of RUN (2)
            // No addresses for COPYs
            // Window 7:
            0x00,  // Win_Indicator: No source segment (ADD only)
            0x22,  // Length of the delta encoding
            0x1B,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x1B,  // length of data for ADDs and RUNs
            0x02,  // length of instructions section
            0x00,  // length of addresses for COPYs
            // Data for ADD: 4th section (length 27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n',
            // Instructions and sizes (length 2)
            0x01,  // VCD_ADD size 0
            0x1B,  // Size of ADD (27)
            // No addresses for COPYs
    };

    protected VCDiffStandardWindowDecoderTestBase() {
        UseStandardFileHeader();
        delta_window_body_ = kWindowBody.clone();
        delta_window_header_ = new byte[0];
    }
}
