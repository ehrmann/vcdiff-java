package com.davidehrmann.vcdiff.engine;

import java.nio.ByteBuffer;

class DeltaFileHeader {
    public static final int SERIALIZED_SIZE = 5;

    public final byte header1;  // Always 0xD6 ('V' | 0x80)
    public final byte header2;  // Always 0xC3 ('C' | 0x80)
    public final byte header3;  // Always 0xC4 ('D' | 0x80)
    public final byte header4;  // 0x00 for standard format, 'S' if extensions used
    public final byte hdr_indicator;

    public DeltaFileHeader(ByteBuffer buffer) {
        header1 = buffer.get();
        header2 = buffer.get();
        header3 = buffer.get();
        header4 = buffer.get();
        hdr_indicator = buffer.get();
    }
}
