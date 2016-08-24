package com.davidehrmann.vcdiff.engine;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VCDiffCustomCodeTableDecoderDeathTest extends VCDiffCustomCodeTableDecoderTestBase {

    public VCDiffCustomCodeTableDecoderDeathTest() {
        output_ = new ByteArrayOutputStream() {
            @Override
            public void write(int b) {
                Assert.fail();
            }

            @Override
            public void write(byte[] b, int off, int len) {
                Assert.fail();
            }

            @Override
            public void write(byte[] b) throws IOException {
                Assert.fail();
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void BadCustomCacheSizes() throws Exception {
        delta_file_header_ = ArraysExtra.concat(
                kFileHeader,
                new byte[] {
                        (byte) 0x81,  // NEAR cache size (top bit)
                        0x10,         // NEAR cache size (custom value 0x90)
                        (byte) 0x81,  // SAME cache size (top bit)
                        0x10,         // SAME cache size (custom value 0x90)
                },
                kEncodedCustomCodeTable
        );

        InitializeDeltaFile();
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(
                delta_file_,
                0,
                delta_file_.length,
                output_
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void BadCustomCacheSizesNoVcdTarget() throws Exception {
        decoder_.setAllowVcdTarget(false);
        delta_file_header_ = ArraysExtra.concat(
                kFileHeader,
                new byte[] {
                        (byte) 0x81,  // NEAR cache size (top bit)
                        0x10,         // NEAR cache size (custom value 0x90)
                        (byte) 0x81,  // SAME cache size (top bit)
                        0x10,         // SAME cache size (custom value 0x90)
                },
                kEncodedCustomCodeTable
        );

        InitializeDeltaFile();
        decoder_.startDecoding(dictionary_);
        decoder_.decodeChunk(
                delta_file_,
                0,
                delta_file_.length,
                output_
        );
    }
}
