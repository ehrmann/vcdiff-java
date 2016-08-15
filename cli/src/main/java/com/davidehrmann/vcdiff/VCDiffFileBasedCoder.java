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

import com.beust.jcommander.*;
import com.davidehrmann.vcdiff.codec.DecoderBuilder;
import com.davidehrmann.vcdiff.codec.EncoderBuilder;
import com.davidehrmann.vcdiff.io.ComparingOutputStream;
import com.davidehrmann.vcdiff.io.CountingInputStream;
import com.davidehrmann.vcdiff.io.CountingOutputStream;
import com.davidehrmann.vcdiff.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static com.davidehrmann.vcdiff.io.IOUtils.closeQuietly;

/**
 * / command-line interface to the open-vcdiff library.
 */
public class VCDiffFileBasedCoder {
    public static final int DEFAULT_MAX_TARGET_SIZE = 1 << 26;      // 64 MB

    public static class PositiveInteger implements IParameterValidator {
        public void validate(String name, String value)  throws ParameterException {
            int n = Integer.parseInt(value);
            if (n <= 0) {
                throw new ParameterException("Parameter " + name + " should be positive (found " + value +")");
            }
        }
    }

    // Definitions of command-line flags
    private static class OptionalTargetAndDeltaOptions {
        @Parameter(names = {"-target", "--target"}, description = "Target file (default is stdin for encode, stdout for decode)")
        protected String target;

        @Parameter(names = {"-delta", "--delta"}, description = "Encoded delta file (default is stdout for encode, stdin for decode)")
        protected String delta;
    }

    private static class RequiredTargetAndDeltaOptions {
        @Parameter(names = {"-target", "--target"}, description = "Target file", required = true)
        protected String target;

        @Parameter(names = {"-delta", "--delta"}, description = "Encoded delta file", required = true)
        protected String delta;
    }

    protected static class EncodeOptions {
        @Parameter(names = {"-checksum", "--checksum"}, description = "Include an Adler32 checksum of the target data when encoding")
        protected boolean checksum = false;

        @Parameter(names = {"-interleaved", "--interleaved"}, description = "Use interleaved format")
        protected boolean interleaved = false;

        @Parameter(names = {"-json", "--json"}, description = "Output diff in the JSON format when encoding")
        protected boolean json = false;

        @Parameter(names = {"-target_matches", "--target_matches"}, description = "Find duplicate strings in target data as well as dictionary data")
        protected boolean targetMatches = false;
    }

    protected static class DecodeOptions {
        @Parameter(names = {"-allow_vcd_target", "--allow_vcd_target"}, description = "If false, the decoder issues an error when the VCD_TARGET flag is encountered")
        protected boolean allowVcdTarget = true;
    }

    protected static class GlobalOptions {
        @Parameter(names = {"-dictionary", "--dictionary"}, description = "File containing dictionary data (required)", required = true)
        protected String dictionary;

        @Parameter(names = {"-max_target_file_size", "--max_target_file_size"}, description = "Maximum target file size allowed by decoder")
        protected long maxTargetFileSize = (long) DEFAULT_MAX_TARGET_SIZE;

        @Parameter(names = {"-max_target_window_size", "--max_target_window_size"}, description = "Maximum target window size allowed by decoder")
        protected int maxTargetWindowSize = DEFAULT_MAX_TARGET_SIZE;

        // --buffersize is the maximum allowable size of a target window.
        // This value may be increased if there is sufficient memory available.
        @Parameter(names = {"-buffersize", "--buffersize"}, description = "Buffer size for reading input file", validateWith = PositiveInteger.class)
        protected int bufferSize = 1 << 20;

        @Parameter(names = {"-stats", "--stats"}, description = "Report compression percentage")
        protected boolean stats = false;
    }

    private VCDiffFileBasedCoder() {

    }

    // Opens a file for incremental reading.  file_name is the name of the file
    // to be opened.  file_type should be a descriptive name (like "target") for
    // use in log messages.
    private static InputStream OpenFileForReading(String file_name, String file_type) throws FileNotFoundException{
        try {
            return new InputStreamExceptionMapper(
                    new FileInputStream(file_name),
                    file_type
            );
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(String.format(
                    "Error opening %s file: %s",
                    file_type, e.getMessage()
            ));
        }
    }

    private static OutputStream OpenFileForWriting(String file_name, String file_type) throws FileNotFoundException{
        try {
            return new OutputStreamExceptionMapper(
                    new FileOutputStream(file_name),
                    file_type
            );
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(String.format(
                    "Error opening %s file: %s",
                    file_type, e.getMessage()
            ));
        }
    }

    // Opens the dictionary file and reads it into a newly allocated buffer.
    // If successful, returns true and populates dictionary with the dictionary
    // contents; otherwise, returns the buffer.
    protected static byte[] OpenDictionary(String dictionary) throws IOException {
        InputStream in = OpenFileForReading(dictionary, "dictionary");
        try {
            return IOUtils.toByteArray(in);
        } finally {
            closeQuietly(in);
        }
    }

    protected static class InputStreamExceptionMapper extends FilterInputStream {

        private final String type;

        protected InputStreamExceptionMapper(InputStream in, String type) {
            super(in);
            this.type = type;
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error reading from %s file: %s%n",
                        type,
                        e.getMessage()
                ));
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return super.read(b, off, len);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error reading from %s file: %s%n",
                        type,
                        e.getMessage()
                ));
            }
        }

        @Override
        public long skip(long n) throws IOException {
            try {
                return super.skip(n);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error reading from %s file: %s%n",
                        type,
                        e.getMessage()
                ));
            }
        }

        @Override
        public int available() throws IOException {
            try {
                return super.available();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error reading from %s file: %s%n",
                        type,
                        e.getMessage()
                ));
            }
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error closing %s file: %s",
                        type,
                        e.getMessage()
                ));
            }
        }
    }

    protected static class OutputStreamExceptionMapper extends FilterOutputStream {
        private final String type;

        public OutputStreamExceptionMapper(OutputStream out, String type) {
            super(out);
            this.type = type;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                super.write(b);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error writing %d byte to %s file: %s",
                        1, type, e.getMessage()
                ));
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                super.out.write(b, off, len);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error writing %d byte(s) to %s file: %s",
                        len, type, e.getMessage()
                ));
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                super.flush();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error flushing %s file: %s",
                        type, e.getMessage()
                ));
            }
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error closing %s file: %s%n",
                        type, e.getMessage()
                ));
            }
        }
    }

    // Once the command-line arguments have been parsed, these functions
    // will use the supplied options to carry out a file-based encode
    // or decode operation.

    @Parameters(commandDescription = "Create delta file from dictionary and target file", separators = " =")
    private static class EncodeCommand extends VCDiffFileBasedCoder {

        @ParametersDelegate
        private EncodeOptions encodeOptions = new EncodeOptions();

        @ParametersDelegate
        private GlobalOptions globalOptions = new GlobalOptions();

        @ParametersDelegate
        private OptionalTargetAndDeltaOptions targetAndDeltaOptions = new OptionalTargetAndDeltaOptions();

        public void Encode() throws IOException {
            byte[] dictionary = OpenDictionary(globalOptions.dictionary);

            // FIXME: support encodeOptions.json
            /*
            if (encodeOptions.json) {
                format_flags.add(VCDiffFormatExtensionFlag.VCD_FORMAT_JSON);
            }
            */

            boolean useStdin = (targetAndDeltaOptions.target == null || targetAndDeltaOptions.target.isEmpty());
            boolean useStdout = (targetAndDeltaOptions.delta == null || targetAndDeltaOptions.delta.isEmpty());

            InputStream fileIn = useStdin ?  new InputStreamExceptionMapper(System.in, "target") : OpenFileForReading(targetAndDeltaOptions.target, "target");
            try {
                CountingInputStream countingIn = new CountingInputStream(fileIn);
                try {
                    OutputStream fileOut = useStdout ? new OutputStreamExceptionMapper(System.out, "delta") : OpenFileForWriting(targetAndDeltaOptions.delta, "delta");
                    try {
                        CountingOutputStream countingOut = new CountingOutputStream(fileOut);
                        try {
                            OutputStream vcDiffOut = EncoderBuilder.builder()
                                    .withDictionary(dictionary)
                                    .withTargetMatches(encodeOptions.targetMatches)
                                    .withChecksum(encodeOptions.checksum)
                                    .withInterleaving(encodeOptions.interleaved)
                                    .buildOutputStream(countingOut);
                            try {
                                IOUtils.copyLarge(countingIn, vcDiffOut, new byte[globalOptions.bufferSize]);
                            } finally {
                                closeQuietly(vcDiffOut);
                            }
                        } finally {
                            closeQuietly(countingOut);
                        }

                        if (globalOptions.stats && (countingIn.getBytesRead() > 0)) {
                            System.err.printf("Original size: %d\tCompressed size: %d (%.2f%% of original)%n",
                                    countingIn.getBytesRead(),
                                    countingOut.getBytesWritten(),
                                    100.0 * countingOut.getBytesWritten() / countingIn.getBytesRead()
                            );
                        }
                    } finally {
                        closeQuietly(fileOut);
                    }
                } finally {
                    closeQuietly(countingIn);
                }
            } finally {
                closeQuietly(fileIn);
            }
        }
    }

    @Parameters(commandDescription = "Reconstruct target file from dictionary and delta file", separators = " =")
    private static class DecodeCommand extends VCDiffFileBasedCoder {

        @ParametersDelegate
        private GlobalOptions globalOptions = new GlobalOptions();

        @ParametersDelegate
        private DecodeOptions decodeOptions = new DecodeOptions();

        @ParametersDelegate
        private OptionalTargetAndDeltaOptions targetAndDeltaFlags = new OptionalTargetAndDeltaOptions();

        public void Decode() throws IOException {
            byte[] dictionary = OpenDictionary(globalOptions.dictionary);

            boolean useStdin = (targetAndDeltaFlags.delta == null || targetAndDeltaFlags.delta.isEmpty());
            boolean useStdout = (targetAndDeltaFlags.target == null || targetAndDeltaFlags.target.isEmpty());

            CountingInputStream countedIn = new CountingInputStream(useStdin ? new InputStreamExceptionMapper(System.in, "delta") : OpenFileForReading(targetAndDeltaFlags.delta, "delta"));
            try {
                InputStream vcDiffIn = DecoderBuilder.builder()
                        .withMaxTargetFileSize(globalOptions.maxTargetFileSize)
                        .withMaxTargetWindowSize(globalOptions.maxTargetWindowSize)
                        .withAllowTargetMatches(decodeOptions.allowVcdTarget)
                        .buildInputStream(countedIn, dictionary);
                try {
                    CountingOutputStream out = new CountingOutputStream(useStdout ?
                            new OutputStreamExceptionMapper(System.out, "target") :
                            OpenFileForWriting(targetAndDeltaFlags.target, "target")
                    );
                    try {
                        IOUtils.copyLarge(vcDiffIn, out, new byte[globalOptions.bufferSize]);

                        if (globalOptions.stats && (out.getBytesWritten() > 0)) {
                            System.err.printf("Decompressed size: %d\tCompressed size: %d (%.2f%% of original)%n",
                                    out.getBytesWritten(),
                                    countedIn.getBytesRead(),
                                    100.0 * countedIn.getBytesRead() / out.getBytesWritten()
                            );
                        }
                    } finally {
                        closeQuietly(out);
                    }
                } finally {
                    closeQuietly(vcDiffIn);
                }
            } finally {
                closeQuietly(countedIn);
            }
        }
    }

    // for "vcdiff test"; compare target with original
    @Parameters(hidden = true, separators = " =")
    private static class DecodeAndCompareCommand extends VCDiffFileBasedCoder {

        @ParametersDelegate
        private GlobalOptions globalOptions = new GlobalOptions();

        @ParametersDelegate
        private EncodeOptions encodeOptions = new EncodeOptions();

        @ParametersDelegate
        private DecodeOptions decodeOptions = new DecodeOptions();

        @ParametersDelegate
        private RequiredTargetAndDeltaOptions targetAndDeltaOptions = new RequiredTargetAndDeltaOptions();

        public void DecodeAndCompare() throws IOException {
            byte[] dictionary = OpenDictionary(globalOptions.dictionary);

            CountingInputStream countedIn = new CountingInputStream(OpenFileForReading(targetAndDeltaOptions.delta, "delta"));
            try {
                InputStream in = DecoderBuilder.builder()
                        .withMaxTargetFileSize(globalOptions.maxTargetFileSize)
                        .withMaxTargetWindowSize(globalOptions.maxTargetWindowSize)
                        .withAllowTargetMatches(decodeOptions.allowVcdTarget)
                        .buildInputStream(countedIn, dictionary);
                try {
                    InputStream expected = OpenFileForReading(targetAndDeltaOptions.target, "target");
                    try {
                        CountingOutputStream out = new CountingOutputStream(
                                new ComparingOutputStream(expected)
                        );
                        try {
                            IOUtils.copyLarge(in, out, new byte[globalOptions.bufferSize]);

                            // Close out here so it verifies EOF
                            out.close();

                            if (globalOptions.stats && (out.getBytesWritten() > 0)) {
                                System.err.printf("Decompressed size: %d\tCompressed size: %d (%.2f%% of original)%n",
                                        out.getBytesWritten(),
                                        countedIn.getBytesRead(),
                                        100.0 * countedIn.getBytesRead() / out.getBytesWritten()
                                );
                            }
                        } finally {
                            closeQuietly(out);
                        }
                    } finally {
                        closeQuietly(expected);
                    }
                } finally {
                    closeQuietly(in);
                }
            } finally {
                countedIn.close();
            }
        }
    }

    public static void main(String[] argv) throws Exception {

        // TODO: JCommander has an issue with boolean arity. Rewrite allow_vcd_target.
        List<String> newArgv = new LinkedList<String>(Arrays.asList(argv));

        ListIterator<String> i = newArgv.listIterator();
        while (i.hasNext()) {
            String arg = i.next();
            if (arg.matches("--?allow_vcd_target") && i.hasNext()) {
                String val = i.next();
                if (val.matches("(?i)true|yes|1")) {
                    i.remove();
                } else if (val.matches("(?i)false|no|0")) {
                    i.remove();
                    i.previous();
                    i.remove();
                } else {
                    i.previous();
                }
            } else if (arg.matches("--?allow_vcd_target(=.*)?")) {
                if (arg.matches("(?i)--?allow_vcd_target=(true|yes|1)")) {
                    i.set("-allow_vcd_target");
                } else if (arg.matches("(?i)--?allow_vcd_target=(false|no|0)")) {
                    i.remove();
                }
            }

            argv = newArgv.toArray(new String[newArgv.size()]);
        }

        EncodeCommand encodeCommand = new EncodeCommand();
        DecodeCommand decodeCommand = new DecodeCommand();
        DecodeAndCompareCommand decodeAndCompareCommand = new DecodeAndCompareCommand();

        JCommander jCommander = new JCommander();
        jCommander.addCommand("encode", encodeCommand, "delta");
        jCommander.addCommand("decode", decodeCommand, "patch");
        jCommander.addCommand("test", decodeAndCompareCommand);

        try {
            jCommander.parse(argv);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        String command_name = VCDiffFileBasedCoder.class.getCanonicalName();
        String command_option = jCommander.getParsedCommand();

        try {
            if ("encode".equals(command_option) || "delta".equals(command_option)) {
                encodeCommand.Encode();
            } else if ("decode".equals(command_option) || "patch".equals(command_option)) {
                decodeCommand.Decode();
            } else if ("test".equals(command_option)) {
                // "vcdiff test" does not appear in the usage string, but can be
                // used for debugging.  It encodes, then decodes, then compares the result
                // with the original target. It expects the same arguments as
                // "vcdiff encode", with the additional requirement that the --target
                // and --delta file arguments must be specified, rather than using stdin
                // or stdout.  It produces a delta file just as for "vcdiff encode".

                // TODO: the test command is kludgy
                JCommander jCommander2 = new JCommander();
                jCommander2.addObject(encodeCommand);
                jCommander2.parse(Arrays.copyOfRange(argv, 1, argv.length));

                encodeCommand.Encode();

                decodeAndCompareCommand.DecodeAndCompare();
            } else {
                System.err.printf("%s: Unrecognized command option %s%n", command_name, command_option);
                jCommander.usage();
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
