package com.davidehrmann.vcdiff.engine;

import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriterImpl.VCD_SOURCE;
import static com.davidehrmann.vcdiff.engine.VCDiffCodeTableWriterImpl.VCD_TARGET;

public abstract class VCDiffInterleavedWindowDecoderTestBase extends VCDiffStandardWindowDecoderTestBase {
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
            0x03,  // length of instructions section
            0x00,  // length of addresses for COPYs
            0x13,  // VCD_COPY mode VCD_SELF, size 0
            0x1C,  // Size of COPY (28)
            0x00,  // Start of dictionary
            // Window 2:
            0x00,  // Win_Indicator: No source segment (ADD only)
            0x44,  // Length of the delta encoding
            0x3D,  // Size of the target window (61)
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x3F,  // length of instructions section
            0x00,  // length of addresses for COPYs
            0x01,  // VCD_ADD size 0
            0x3D,  // Size of ADD (61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            // Window 3:
            VCD_TARGET,  // Win_Indicator: take source from decoded data
            0x59,  // Source segment size: length of data decoded so far
            0x00,  // Source segment position: start of decoded data
            0x08,  // Length of the delta encoding
            0x2C,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x03,  // length of instructions section
            0x00,  // length of addresses for COPYs
            0x23,  // VCD_COPY mode VCD_HERE, size 0
            0x2C,  // Size of COPY (44)
            0x58,  // HERE mode address (27+61 back from here_address)
            // Window 4:
            VCD_TARGET,  // Win_Indicator: take source from decoded data
            0x05,  // Source segment size: only 5 bytes needed for this COPY
            0x2E,  // Source segment position: offset for COPY
            0x09,  // Length of the delta encoding
            0x07,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x04,  // length of instructions section
            0x00,  // length of addresses for COPYs
            (byte) 0xA7,  // VCD_ADD size 2 + VCD_COPY mode SELF, size 5
            'h', 'r',
            0x00,  // SELF mode address (start of source segment)
            // Window 5:
            0x00,  // Win_Indicator: No source segment (ADD only)
            0x0F,  // Length of the delta encoding
            0x09,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x0A,  // length of instructions section
            0x00,  // length of addresses for COPYs
            0x0A,       // VCD_ADD size 9
            'W', 'h', 'a', 't', ' ', 'I', ' ', 't', 'e',
            // Window 6:
            0x00,  // Win_Indicator: No source segment (RUN only)
            0x08,  // Length of the delta encoding
            0x02,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x03,  // length of instructions section
            0x00,  // length of addresses for COPYs
            0x00,  // VCD_RUN size 0
            0x02,  // Size of RUN (2)
            'l',
            // Window 7:
            0x00,  // Win_Indicator: No source segment (ADD only)
            0x22,  // Length of the delta encoding
            0x1B,  // Size of the target window
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs
            0x1D,  // length of instructions section
            0x00,  // length of addresses for COPYs
            0x01,  // VCD_ADD size 0
            0x1B,  // Size of ADD (27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n',
    };

    protected VCDiffInterleavedWindowDecoderTestBase() {
        UseInterleavedFileHeader();
        // delta_window_header_ is left blank.  All window headers and bodies are
        // lumped together in delta_window_body_.  This means that addChecksum()
        // cannot be used to test the checksum feature.
        delta_window_body_ = kWindowBody.clone();
    }
}
