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

package com.googlecode.jvcdiff;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.validators.PositiveInteger;
import com.googlecode.jvcdiff.codec.*;
import com.googlecode.jvcdiff.google.VCDiffFormatExtensionFlag;

import java.io.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * / command-line interface to the open-vcdiff library.
 */
public class VCDiffFileBasedCoder {
    public static final int kDefaultMaxTargetSize = 1 << 26;      // 64 MB

    // Definitions of command-line flags
    @Parameter(names = { "---dictionary" }, description = "File containing dictionary data (required)")
    protected String FLAGS_dictionary;

    @Parameter(names = {"--target"}, description = "Target file (default is stdin for encode, stdout for decode")
    protected String FLAGS_target;

    @Parameter(names = {"--delta"}, description = "Encoded delta file (default is stdout for encode, stdin for decode")
    protected String FLAGS_delta;

    // --buffersize is the maximum allowable size of a target window.
    // This value may be increased if there is sufficient memory available.
    @Parameter(names = {"--buffersize"}, description = "Buffer size for reading input file", validateWith = PositiveInteger.class)
    protected Integer FLAGS_buffersize = 1 << 20;

    @Parameter(names = {"--stats"}, description = "Report compression percentage")
    protected Boolean FLAGS_stats = false;

    @Parameter(names = {"--max_target_file_size"}, description = "Maximum target file size allowed by decoder")
    protected Long FLAGS_max_target_file_size = (long) kDefaultMaxTargetSize;

    @Parameter(names = {"--max_target_window_size"}, description = "Maximum target window size allowed by decoder")
    protected Integer FLAGS_max_target_window_size = kDefaultMaxTargetSize;

    public VCDiffFileBasedCoder() {

    }

    // Opens a file for incremental reading.  file_name is the name of the file
    // to be opened.  file_type should be a descriptive name (like "target") for
    // use in log messages.  If successful, returns true and sets *file to a
    // valid input file, *buffer to a region of memory allocated using malloc()
    // (so the caller must release it using free()), and buffer_size to the size
    // of the buffer, which will not be larger than the size of the file, and
    // will not be smaller than the --buffersize option.  If the function fails,
    // it outputs a log message and returns false.
    private static InputStream OpenFileForReading(String file_name, String file_type) throws FileNotFoundException{
        try {
            return new InputStreamExceptionMapper(
                    new FileInputStream(file_name),
                    file_type,
                    file_name
            );
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(String.format(
                    "Error opening %s file '%s': %s%n",
                    file_type, file_name, e.getMessage()
            ));
        }
    }

    private static OutputStream OpenFileForWriting(String file_name, String file_type) throws FileNotFoundException{
        try {
            return new OutputStreamExceptionMapper(
                    new FileOutputStream(file_name),
                    file_type,
                    file_name
            );
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(String.format(
                    "Error opening %s file '%s': %s%n",
                    file_type, file_name, e.getMessage()
            ));
        }
    }

    // Opens the dictionary file and reads it into a newly allocated buffer.
    // If successful, returns true and populates dictionary with the dictionary
    // contents; otherwise, returns the buffer.
    protected byte[] OpenDictionary() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = OpenFileForReading(FLAGS_dictionary, "dictionary");
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            quietClose(in);
        }

        return out.toByteArray();
    }

    protected static class ComparingOutputStream extends OutputStream {

        private final InputStream expected;
        private volatile boolean open = true;

        public ComparingOutputStream(InputStream expected) {
            this.expected = new BufferedInputStream(expected);
        }

        @Override
        public void write(int b) throws IOException {
            if (!open) {
                throw new IllegalStateException();
            }

            b = b & 0xff;
            int read = expected.read();
            if (read < 0) {
                throw new IOException("Decoded target is longer than original target file");
            } else if (b != read) {
                throw new IOException("Original target file does not match decoded target");
            }
        }

        @Override
        public void close() throws IOException {
            open = false;
            if (expected.read() >= 0) {
                throw new IOException("Decoded target is shorter than original target file");
            }
        }
    }

    protected static class CountingOutputStream extends FilterOutputStream {

        private final AtomicLong bytesWritten = new AtomicLong();

        public CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            bytesWritten.getAndIncrement();
        }

        @Override
        public void write(byte[] b) throws IOException {
            super.write(b);
            bytesWritten.getAndAdd(b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            bytesWritten.getAndAdd(len);
        }

        public long getBytesWritten() {
            return bytesWritten.get();
        }
    }

    protected static class InputStreamExceptionMapper extends FilterInputStream {

        private final String type;
        private final String file;

        protected InputStreamExceptionMapper(InputStream in, String type, String file) {
            super(in);
            this.type = type;
            this.file = file;
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error reading from %s file '%s': %s%n",
                        type,
                        file,
                        e.getMessage()
                ));
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            try {
                return super.read(b);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error reading from %s file '%s': %s%n",
                        type,
                        file,
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
                        "Error reading from %s file '%s': %s%n",
                        type,
                        file,
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
                        "Error reading from %s file '%s': %s%n",
                        type,
                        file,
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
                        "Error reading from %s file '%s': %s%n",
                        type,
                        file,
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
                        "Error closing %s file '%s': %s%n",
                        type,
                        file,
                        e.getMessage()
                ));
            }
        }
    }

    protected static class OutputStreamExceptionMapper extends FilterOutputStream {
        private final String type;
        private final String file;

        public OutputStreamExceptionMapper(OutputStream out, String type, String file) {
            super(out);
            this.type = type;
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                super.write(b);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error writing %d byte to %s file '%s': %s%n",
                        1, type, file, e.getMessage()
                ));
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            try {
                super.write(b);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error writing %d bytes to %s file '%s': %s%n",
                        b.length, type, file, e.getMessage()
                ));
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                super.write(b);
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error writing %d bytes to %s file '%s': %s%n",
                        len, type, file, e.getMessage()
                ));
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                super.flush();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error flushing %s file '%s': %s%n",
                        type, file, e.getMessage()
                ));
            }
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } catch (IOException e) {
                throw new IOException(String.format(
                        "Error closing %s file '%s': %s%n",
                        type, file, e.getMessage()
                ));
            }
        }
    }

    // Once the command-line arguments have been parsed, these functions
    // will use the supplied options to carry out a file-based encode
    // or decode operation.

    @Parameters(commandDescription = "Create delta file from dictionary and target file")
    private static class EncodeCommand extends VCDiffFileBasedCoder {

        @Parameter(names = {"--checksum"}, description = "Include an Adler32 checksum of the target data when encoding")
        protected Boolean FLAGS_checksum = false;

        @Parameter(names = {"--interleaved"}, description = "Use interleaved format")
        protected Boolean FLAGS_interleaved = false;

        @Parameter(names = {"--json"}, description = "Output diff in the JSON format when encoding")
        protected Boolean FLAGS_json = false;

        @Parameter(names = {"--target_matches"}, description = "Find duplicate strings in target data as well as dictionary data")
        protected Boolean FLAGS_target_matches = false;

        public void Encode() throws IOException {
            byte[] dictionary = OpenDictionary();
            HashedDictionary hashedDictionary = new HashedDictionary(dictionary);

            EnumSet<VCDiffFormatExtensionFlag> format_flags = EnumSet.of(VCDiffFormatExtensionFlag.VCD_STANDARD_FORMAT);
            if (FLAGS_interleaved) {
                format_flags.add(VCDiffFormatExtensionFlag.VCD_FORMAT_INTERLEAVED);
            }
            if (FLAGS_checksum) {
                format_flags.add( VCDiffFormatExtensionFlag.VCD_FORMAT_CHECKSUM);
            }
            if (FLAGS_json) {
                format_flags.add(VCDiffFormatExtensionFlag.VCD_FORMAT_JSON);
            }

            CodeTableWriterInterface<OutputStream> coder = new VCDiffCodeTableWriter(FLAGS_interleaved);
            coder.Init(dictionary.length);

            VCDiffStreamingEncoder<OutputStream> encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                    coder,
                    hashedDictionary,
                    format_flags,
                    FLAGS_target_matches
            );

            boolean useStdin = (FLAGS_target == null || FLAGS_target.isEmpty());
            boolean useStdout = (FLAGS_delta == null || FLAGS_delta.isEmpty());

            InputStream in = useStdin ?  new InputStreamExceptionMapper(System.in, "target", "stdin") : OpenFileForReading(FLAGS_target, "target");
            try {
                long inputSize = 0;

                CountingOutputStream out = new CountingOutputStream(useStdout ?
                        new OutputStreamExceptionMapper(System.out, "delta", "stdout") : OpenFileForWriting(FLAGS_delta, "delta")
                );
                try {
                    if (!encoder.StartEncoding(out)) {
                        throw new IOException("Error during encoder initialization");
                    }

                    byte[] buffer = new byte[FLAGS_buffersize];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        inputSize += read;
                        if (!encoder.EncodeChunk(buffer, 0, read, out)) {
                            throw new IOException("Error trying to encode data chunk of length " + read);
                        }
                    }

                    encoder.FinishEncoding(out);
                } finally {
                    quietClose(out);
                }

                if (FLAGS_stats && (inputSize > 0)) {
                    System.err.printf("Original size: %d\tCompressed size: %d (%.2f%% of original)%n",
                            inputSize,
                            out.getBytesWritten(),
                            100.0 * out.getBytesWritten() / inputSize
                    );
                }
            } finally {
                quietClose(in);
            }
        }
    }

    @Parameters(commandDescription = "Reconstruct target file from dictionary and delta file")
    private static class DecodeCommand extends VCDiffFileBasedCoder {

        @Parameter(names = {"--allow_vcd_target"}, description = "If false, the decoder issues an error when the VCD_TARGET flag is encountered" )
        protected Boolean FLAGS_allow_vcd_target = true;

        public void Decode() throws IOException {
            byte[] dictionary = OpenDictionary();
            HashedDictionary hashedDictionary = new HashedDictionary(dictionary);

            VCDiffStreamingDecoder decoder = new VCDiffStreamingDecoderImpl();
            decoder.SetMaximumTargetFileSize(FLAGS_max_target_file_size);
            decoder.SetMaximumTargetWindowSize(FLAGS_max_target_window_size);
            decoder.SetAllowVcdTarget(FLAGS_allow_vcd_target);

            boolean useStdin = (FLAGS_delta == null || FLAGS_delta.isEmpty());
            boolean useStdout = (FLAGS_target == null || FLAGS_target.isEmpty());

            InputStream in = useStdin ? new InputStreamExceptionMapper(System.in, "delta", "stdin") : OpenFileForReading(FLAGS_delta, "delta");
            try {
                long inputSize = 0;

                CountingOutputStream out = new CountingOutputStream(useStdout ?
                        new OutputStreamExceptionMapper(System.out, "target", "stdout") :
                        OpenFileForWriting(FLAGS_target, "target")
                );
                try {
                    decoder.StartDecoding(dictionary);

                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        inputSize += read;
                        if (!decoder.DecodeChunk(buffer, 0, read, out)) {
                            throw new IOException("Error trying to decode data chunk of length " + read);
                        }
                    }

                    if (!decoder.FinishDecoding()) {
                        throw new IOException("Decode error; '" + FLAGS_delta + "' may not be a valid VCDIFF delta file");
                    }

                    if (FLAGS_stats && (out.getBytesWritten() > 0)) {
                        System.err.printf("Decompressed size: %d\tCompressed size: %d (%.2f%% of original)%n",
                                out.getBytesWritten(),
                                inputSize,
                                100.0 * inputSize / out.getBytesWritten()
                        );
                    }
                } finally {
                    quietClose(out);
                }
            } finally {
                quietClose(in);
            }
        }
    }

    // for "vcdiff test"; compare target with original
    @Parameters
    private static class DecodeAndCompareCommand extends VCDiffFileBasedCoder {

        @Parameter(names = {"--target"}, description = "Target file (default is stdin for encode, stdout for decode", required = true)
        protected String FLAGS_target;

        @Parameter(names = {"--delta"}, description = "Encoded delta file (default is stdout for encode, stdin for decode", required = true)
        protected String FLAGS_delta;

        @Parameter(names = {"--allow_vcd_target"}, description = "If false, the decoder issues an error when the VCD_TARGET flag is encountered" )
        protected Boolean FLAGS_allow_vcd_target = true;

        public void DecodeAndCompare() throws IOException {
            byte[] dictionary = OpenDictionary();
            HashedDictionary hashedDictionary = new HashedDictionary(dictionary);

            VCDiffStreamingDecoder decoder = new VCDiffStreamingDecoderImpl();
            decoder.SetMaximumTargetFileSize(FLAGS_max_target_file_size);
            decoder.SetMaximumTargetWindowSize(FLAGS_max_target_window_size);
            decoder.SetAllowVcdTarget(FLAGS_allow_vcd_target);

            InputStream in = OpenFileForReading(FLAGS_delta, "delta");
            try {
                long input_size = 0;

                InputStream expected = OpenFileForReading(FLAGS_target, "target");
                try {
                    CountingOutputStream out = new CountingOutputStream(
                            new ComparingOutputStream(expected)
                    );
                    try {
                        decoder.StartDecoding(dictionary);

                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = in.read(buffer)) >= 0) {
                            input_size += read;
                            if (!decoder.DecodeChunk(buffer, 0, read, out)) {
                                throw new IOException("Error trying to decode data chunk of length " + read);
                            }
                        }

                        if (!decoder.FinishDecoding()) {
                            throw new IOException("Decode error; '" + FLAGS_delta + "' may not be a valid VCDIFF delta file");
                        }

                        // Close out here so it verifies EOF
                        out.close();

                        if (FLAGS_stats && (out.getBytesWritten() > 0)) {
                            System.err.printf("Decompressed size: %d\tCompressed size: %d (%.2f%% of original)%n",
                                    out.getBytesWritten(),
                                    input_size,
                                    100.0 * input_size / out.getBytesWritten()
                            );
                        }
                    } finally {
                        quietClose(out);
                    }
                } finally {
                    quietClose(expected);
                }
            } finally {
                quietClose(in);
            }
        }
    }

    protected static void quietClose(Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) { }
    }

    public static void main(String[] argv) throws Exception {
        VCDiffFileBasedCoder coder = new VCDiffFileBasedCoder();
        EncodeCommand encodeCommand = new EncodeCommand();
        DecodeCommand decodeCommand = new DecodeCommand();
        DecodeAndCompareCommand decodeAndCompareCommand = new DecodeAndCompareCommand();

        JCommander jCommander = new JCommander();
        jCommander.addObject(coder);
        jCommander.addCommand("encode", encodeCommand, "delta");
        jCommander.addCommand("decode", decodeCommand, "patch");
        jCommander.addCommand("test", decodeAndCompareCommand);

        jCommander.parse(argv);

        String command_name = VCDiffFileBasedCoder.class.getCanonicalName();

        String command_option = jCommander.getParsedCommand();
        if (coder.FLAGS_dictionary == null || coder.FLAGS_dictionary.isEmpty()) {
            System.err.printf("%s %s: Must specify --dictionary <file-name>%n",
                    command_name,
                    command_option

            );
            jCommander.usage(command_option);
            System.exit(1);
        }

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

            JCommander jCommander2 = new JCommander();
            jCommander2.addObject(encodeCommand);
            jCommander.parse(Arrays.copyOfRange(argv, 1, argv.length));

            encodeCommand.Encode();

            decodeAndCompareCommand.DecodeAndCompare();
        } else {
            System.err.printf("%s: Unrecognized command option %s%n", command_name, command_option);
            jCommander.usage();
            System.exit(1);
        }
    }
}
