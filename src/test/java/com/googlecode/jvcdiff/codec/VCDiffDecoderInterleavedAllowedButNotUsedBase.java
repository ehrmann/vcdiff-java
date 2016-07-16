package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.codec.VCDiffStandardDecoderTestBase;

public abstract class VCDiffDecoderInterleavedAllowedButNotUsedBase extends VCDiffStandardDecoderTestBase {
    protected VCDiffDecoderInterleavedAllowedButNotUsedBase() {
        UseInterleavedFileHeader();
    }
}
