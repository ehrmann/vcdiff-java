// Copyright 2008-2016 Google Inc., David Ehrmann
// Author: Lincoln Smith, David Ehrmann
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.davidehrmann.vcdiff;

import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings("ThrowFromFinallyBlock")
public class VCDiffFileBasedCoderTest {

    private static File dictionaryFile;
    private static File targetFile;
    private static File maliciousEncoding;

    @ClassRule
    public static final TemporaryFolder tempClassFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpFiles() throws IOException {
        dictionaryFile = tempClassFolder.newFile("configure.ac.v0.1");
        copyResourceToFile(dictionaryFile, "configure.ac.v0.1");
        assumeTrue(dictionaryFile.setReadOnly());

        targetFile = tempClassFolder.newFile("configure.ac.v0.2");
        copyResourceToFile(targetFile, "configure.ac.v0.2");
        assumeTrue(targetFile.setReadOnly());

        maliciousEncoding = tempClassFolder.newFile("allocates_4gb.vcdiff");
        copyResourceToFile(maliciousEncoding, "allocates_4gb.vcdiff");
        assumeTrue(maliciousEncoding.setReadOnly());
    }

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private File deltaFile;
    private File outputTargetFile;

    @Before
    public void setUpTempFiles() throws IOException {
        deltaFile = tempFolder.newFile("configure.ac.vcdiff");
        outputTargetFile = tempFolder.newFile("configure.ac.output");
    }

    @Test
    public void testNoArguments() throws Exception {
        // vcdiff with no arguments shows usage information & error result
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[0]);
    }

    @Test
    public void testNoCommand() throws Exception {
        // vcdiff with three arguments but without "encode" or "decode"
        // shows usage information & error result
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[]{
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testVCDiff() throws Exception {
        // vcdiff with all three arguments.  Verify that output file matches target file.
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testVCDiffWithStdio() throws Exception {
        // vcdiff using stdin/stdout.  Verify that output file matches target file.
        InputStream inBackup;
        PrintStream outBackup;

        // encode
        inBackup = System.in;
        try {
            outBackup = System.out;
            try {
                InputStream in = new FileInputStream(targetFile);
                try {
                    OutputStream out = new FileOutputStream(deltaFile);
                    try {
                        System.setIn(in);
                        System.setOut(new PrintStream(out));

                        VCDiffFileBasedCoder.main(new String[] {
                                "encode",
                                "-interleaved",
                                "-checksum",
                                "-dictionary", dictionaryFile.getCanonicalPath(),
                        });
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } finally {
                System.setOut(outBackup);
            }
        } finally {
            System.setIn(inBackup);
        }

        // decode
        inBackup = System.in;
        try {
            outBackup = System.out;
            try {
                InputStream in = new FileInputStream(deltaFile);
                try {
                    OutputStream out = new FileOutputStream(outputTargetFile);
                    try {
                        System.setIn(in);
                        System.setOut(new PrintStream(out));

                        VCDiffFileBasedCoder.main(new String[] {
                                "decode",
                                "-dictionary", dictionaryFile.getCanonicalPath(),
                        });
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } finally {
                System.setOut(outBackup);
            }
        } finally {
            System.setIn(inBackup);
        }

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testVCDiffWithStdout() throws Exception {
        // vcdiff with mixed stdin/stdout.

        // encode
        PrintStream outBackup = System.out;
        try {
            OutputStream deltaOut = new FileOutputStream(deltaFile);
            try {
                System.setOut(new PrintStream(deltaOut));

                VCDiffFileBasedCoder.main(new String[] {
                        "encode",
                        "-interleaved",
                        "-checksum",
                        "-target", targetFile.getCanonicalPath(),
                        "-dictionary", dictionaryFile.getCanonicalPath(),
                });
            } finally {
                deltaOut.close();
            }
        } finally {
            System.setOut(outBackup);
        }

        // decode
        outBackup = System.out;
        try {
            OutputStream targetOut = new FileOutputStream(outputTargetFile);
            try {
                System.setOut(new PrintStream(targetOut));

                VCDiffFileBasedCoder.main(new String[] {
                        "decode",
                        "-delta", deltaFile.getCanonicalPath(),
                        "-dictionary", dictionaryFile.getCanonicalPath(),
                });
            }  finally {
                targetOut.close();
            }
        } finally {
            System.setOut(outBackup);
        }

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testVCDiffWithStdin() throws Exception {
        // vcdiff using stdin/stdout.  Verify that output file matches target file.
        InputStream inBackup;

        // encode
        inBackup = System.in;
        try {
            InputStream in = new FileInputStream(targetFile);
            try {
                System.setIn(in);

                VCDiffFileBasedCoder.main(new String[] {
                        "encode",
                        "-interleaved",
                        "-checksum",
                        "-delta", deltaFile.getCanonicalPath(),
                        "-dictionary", dictionaryFile.getCanonicalPath(),
                });
            } finally {
                in.close();
            }
        } finally {
            System.setIn(inBackup);
        }

        // decode
        inBackup = System.in;
        try {
            InputStream in = new FileInputStream(deltaFile);
            try {
                System.setIn(in);

                VCDiffFileBasedCoder.main(new String[] {
                        "decode",
                        "-target", outputTargetFile.getCanonicalPath(),
                        "-dictionary", dictionaryFile.getCanonicalPath(),
                });
            } finally {
                in.close();
            }
        } finally {
            System.setIn(inBackup);
        }

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testWrongDictionary() throws Exception {
        // If using the wrong dictionary, and dictionary is smaller than the original
        // dictionary, vcdiff will spot the mistake and return an error.  (It can't
        // detect the case where the wrong dictionary is larger than the right one.)
        exit.expectSystemExit();

        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-dictionary", targetFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testVCDiffTest() throws Exception {
        // "vcdiff test" with all three arguments.
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath()
        });
    }

    @Test
    public void testDictionaryNotFound() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", tempFolder.getRoot().getCanonicalPath() + File.separator + "nonexistent_file",
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testTargetNotFound() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", tempFolder.getRoot().getCanonicalPath() + File.separator + "nonexistent_file",
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testDeltaNotFound() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", tempFolder.getRoot().getCanonicalPath() + File.separator + "nonexistent_file",
                "-target", outputTargetFile.getCanonicalPath()
        });
    }

    @Test
    public void testStats() throws Exception {
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-stats",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
    });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-stats",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testEmptyDictionary() throws Exception {
        // Using and empty dictionary should work, but (because dictionary is empty)
        // it will not produce a small delta file.
        File emptyDictionary = tempFolder.newFile("empty_dictionary");

        // "vcdiff test" with all three arguments.
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-interleaved",
                "-checksum",
                "-stats",
                "-dictionary", emptyDictionary.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath()
        });
    }

    @Test
    public void testEmptyUnreadableDictionary() throws Exception {
        exit.expectSystemExit();

        File writeOnlyDictionary = tempFolder.newFile("write_only_dictionary");
        assumeTrue("Couldn't create a read-only file", writeOnlyDictionary.setReadable(false));

        // "vcdiff test" with all three arguments.
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", writeOnlyDictionary.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath()
        });
    }

    @Test
    public void testEmptyUnreadableTarget() throws Exception {
        exit.expectSystemExit();

        File writeOnlyTarget = tempFolder.newFile("write_only_target");
        assumeTrue("Couldn't create a read-only file", writeOnlyTarget.setReadable(false));

        // "vcdiff test" with all three arguments.
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", writeOnlyTarget.getCanonicalPath()
        });
    }

    @Test
    public void testInvalidDelta() throws Exception {
        exit.expectSystemExit();

        File invalidDeltaFile = tempFolder.newFile("invalid_delta");

        Random random = new Random(42);
        FileOutputStream out = new FileOutputStream(invalidDeltaFile);
        try {
            for (int i = 0; i < 97; i++) {
                out.write(random.nextInt());
            }
        } finally {
            out.close();
        }

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-dictionary", targetFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
                "-delta", invalidDeltaFile.getCanonicalPath()
        });
    }

    @Test
    public void missingDictionary() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-delta", deltaFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-dictionary"
        });
    }

    @Test
    public void testMissingTarget() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-delta", deltaFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target"
        });
    }

    @Test
    public void testMissingDelta() throws Exception {
        exit.expectSystemExit();

        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta"
        });
    }

    @Test
    public void testMissingBufferSize() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-buffersize"
        });
    }

    @Test
    public void testBufferSize() throws Exception {
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-buffersize", "1",
                "-stats"
        });
    }

    @Test
    public void testBufferSizeWithStdio() throws Exception {
        InputStream inBackup;
        PrintStream outBackup;

        // encode
        inBackup = System.in;
        try {
            outBackup = System.out;
            try {
                InputStream in = new FileInputStream(targetFile);
                try {
                    OutputStream out = new FileOutputStream(deltaFile);
                    try {
                        System.setIn(in);
                        System.setOut(new PrintStream(out));

                        VCDiffFileBasedCoder.main(new String[] {
                                "encode",
                                "-interleaved",
                                "-checksum",
                                "-buffersize", "1",
                                "-stats",
                                "-dictionary", dictionaryFile.getCanonicalPath(),
                        });
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } finally {
                System.setOut(outBackup);
            }
        } finally {
            System.setIn(inBackup);
        }

        // decode
        inBackup = System.in;
        try {
            outBackup = System.out;
            try {
                InputStream in = new FileInputStream(deltaFile);
                try {
                    OutputStream out = new FileOutputStream(outputTargetFile);
                    try {
                        System.setIn(in);
                        System.setOut(new PrintStream(out));

                        VCDiffFileBasedCoder.main(new String[] {
                                "decode",
                                "-buffersize", "1",
                                "-stats",
                                "-dictionary", dictionaryFile.getCanonicalPath(),
                        });
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } finally {
                System.setOut(outBackup);
            }
        } finally {
            System.setIn(inBackup);
        }

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testBufferSizeZero() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-buffersize", "0",
        });
    }

    @Test
    public void testLargeBufferSize() throws Exception {
        // Using -buffersize=128M (larger than default maximum) should still work.
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-buffersize", "134217728",
        });
    }

    @Test
    public void testUnrecognizedOption() throws Exception {
        exit.expectSystemExit();

        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-froobish"
        });
    }

    @Test
    public void testEncodeMissingDictionary() throws Exception {
        exit.expectSystemExit();

        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testDecodeMissingDictionary() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testVCDiffNoInterleavedAndChecksum() throws Exception {
        // vcdiff with all three arguments.  Verify that output file matches target file.
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testVCDiffTargetMatches() throws Exception {
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-target_matches",
                "-stats",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testUnrecognizedCommand() throws Exception {
        exit.expectSystemExit();

        VCDiffFileBasedCoder.main(new String[] {
                "dencode",
                "-target_matches",
                "-stats",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });
    }

    @Test
    public void testTestWithoutDelta() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-target_matches",
                "-stats",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
        });
    }

    @Test
    public void testTestWithoutTarget() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-target_matches",
                "-stats",
                "-delta", deltaFile.getCanonicalPath(),
                "-dictionary", dictionaryFile.getCanonicalPath(),
        });
    }

    @Test
    public void testTestWithoutDictionary() throws Exception {
        exit.expectSystemExit();
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-target_matches",
                "-delta", deltaFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath()
        });
    }

    @Test
    public void testMaliciousEncodingWithMaxTargetFileSize() throws Exception {
        // A malicious encoding that tries to produce a 4GB target file made up of 64
        // windows, each window having a size of 64MB.

        exit.expectSystemExit();

        PrintStream outBackup = System.out;
        try {
            System.setOut(new PrintStream(nopOutputStream()));

            VCDiffFileBasedCoder.main(new String[] {
                    "decode",
                    "-dictionary", dictionaryFile.getCanonicalPath(),
                    "-delta", maliciousEncoding.getCanonicalPath(),
                    "-max_target_file_size", "65536"
            });
        } finally {
            System.setOut(outBackup);
        }
    }

    @Test
    public void testMaliciousEncodingWithMaxTargetWindowSize() throws Exception {
        exit.expectSystemExit();

        PrintStream outBackup = System.out;
        try {
            System.setOut(new PrintStream(nopOutputStream()));

            VCDiffFileBasedCoder.main(new String[] {
                    "decode",
                    "-dictionary", dictionaryFile.getCanonicalPath(),
                    "-delta", maliciousEncoding.getCanonicalPath(),
                    "-max_target_window_size", "65536"
            });
        } finally {
            System.setOut(outBackup);
        }
    }

    @Test
    public void testWithMaxTargetFileSize() throws Exception {
        // Decoding a small target with the -max_target_file_size option should succeed.
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-max_target_file_size", "65536"
        });
    }

    @Test
    public void testWithMaxTargetWindowSize() throws Exception {
        // Decoding a small target with -max_target_window_size option should succeed.
        VCDiffFileBasedCoder.main(new String[] {
                "test",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-max_target_window_size", "65536"
        });
    }

    @Test
    public void testDisallowVCDTarget() throws Exception {
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-allow_vcd_target=false",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testAllowVCDTarget() throws Exception {
        VCDiffFileBasedCoder.main(new String[] {
                "encode",
                "-interleaved",
                "-checksum",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-target", targetFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath()
        });

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-allow_vcd_target=true",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    @Test
    public void testDecodeReferenceDeltas() throws Exception {
        // These deltas were generated by open-vcdiff
        for (int i = 0; i < 6; i++) {
            String filename = String.format("configure.ac.vcdiff.%d", i);
            copyResourceToFile(deltaFile, filename);

            VCDiffFileBasedCoder.main(new String[] {
                    "decode",
                    "-allow_vcd_target=true",
                    "-dictionary", dictionaryFile.getCanonicalPath(),
                    "-delta", deltaFile.getCanonicalPath(),
                    "-target", outputTargetFile.getCanonicalPath(),
            });

            assertFileEquals(targetFile, outputTargetFile);
        }
    }

    @Test
    public void testDecodeXDelta3() throws Exception {
        // This delta was generated by xdelta3:
        // xdelta3 -e -S -A -n -s configure.ac.v0.1 configure.ac.v0.2 xdelta3.vcdiff
        copyResourceToFile(deltaFile, "xdelta3.vcdiff");

        VCDiffFileBasedCoder.main(new String[] {
                "decode",
                "-allow_vcd_target=true",
                "-dictionary", dictionaryFile.getCanonicalPath(),
                "-delta", deltaFile.getCanonicalPath(),
                "-target", outputTargetFile.getCanonicalPath(),
        });

        assertFileEquals(targetFile, outputTargetFile);
    }

    private static void copyResourceToFile(File dest, String resource) throws IOException {
        InputStream in = VCDiffFileBasedCoderTest.class.getResource(resource).openStream();
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static void assertFileEquals(File expected, File actual) throws IOException {
        InputStream eIn = new BufferedInputStream(new FileInputStream(expected));
        try {
            InputStream aIn = new BufferedInputStream(new FileInputStream(actual));
            try {
                assertInputStreamEquals(eIn, aIn);
            } finally {
                aIn.close();
            }
        } finally {
            eIn.close();
        }
    }

    private static void assertInputStreamEquals(InputStream expected, InputStream actual) throws IOException {
        long offset = 0;
        int e, a;
        do {
            e = expected.read();
            a = actual.read();
            assertEquals("Actual file differs at byte offset " + offset, e, a);
            ++offset;
        } while (e >= 0 && a >= 0);
    }

    private static OutputStream nopOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) { }
        };
    }
}
