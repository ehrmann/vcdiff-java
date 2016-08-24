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
//
// Definition of an abstract class that describes the interface between the
// encoding engine (which finds the best string matches between the source and
// target data) and the code table writer.  The code table writer is passed a
// series of add, copy, and run instructions and produces an output file in the
// desired format.


package com.davidehrmann.vcdiff;

import java.io.IOException;
import java.util.EnumSet;


/**
 * @author David Ehrmann
 * 
 *  The method calls after construction should follow this pattern:
 *    {{add|copy|run}* output}*
 *
 * output() will produce an encoding using the given series of add, copy,
 * and/or run instructions.  One implementation of the interface
 * (VCDiffCodeTableWriter) produces a VCDIFF delta window, but other
 * implementations may be used to produce other output formats, or as test
 * mocks, or to gather encoding statistics.
 */
public interface VCDiffCodeTableWriter<OUT> {

    /**
     * Initializes the constructed object for use. It will return
     * false if there was an error initializing the object, or true if it
     * was successful.  After the object has been initialized and used,
     * init() can be called again to restore the initial state of the object.
     *
     * @param dictionarySize size of the dictionary being used
     * @throws IOException if the CodeTableWriter failed to initialize
     */
    void init(int dictionarySize) throws IOException;


    /**
     * Writes the header to the output string.
     * @param out writer mechanism to write to
     * @param formatExtensions Flags for enabling features that are extensions to the format
     * @throws IOException if there's an exception writing to out
     */
    void writeHeader(OUT out, EnumSet<VCDiffFormatExtension> formatExtensions) throws IOException;

    /**
     * encode an ADD opcode with the "size" length starting at offset
     * @param data data to add
     * @param offset offset in data to start from
     * @param length  total bytes to add
     */
    void add(byte[] data, int offset, int length);

    /**
     * encode a COPY opcode with args "offset" (into dictionary) and "size" bytes.
     * @param offset offset into the dictionary to copy data from
     * @param size number of bytes to copy from dictionary
     */
    void copy(int offset, int size);

    /**
     * encode a RUN opcode for "size" copies of the value "b".
     * @param size number of copies of the value to write
     * @param b byte value to write
     */
    void run(int size, byte b);

    /**
     * add a checksum to the code table
     *
     * @param checksum checksum to write to the writer
     */
    void addChecksum(int checksum);

    /**
     * Appends the encoded delta window to the output
     * string.
     *
     * @param out writer mechanism to write to
     * @throws IOException if there's an exception writing to out
     */
    void output(OUT out) throws IOException;

    /**
     * Finishes encoding.
     *
     * @param out writer mechanism to write to
     * @throws IOException if there's an exception writing to out
     */
    void finishEncoding(OUT out) throws IOException;
}
