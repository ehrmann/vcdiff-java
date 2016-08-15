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
// series of Add, Copy, and Run instructions and produces an output file in the
// desired format.


package com.davidehrmann.vcdiff;

import com.davidehrmann.vcdiff.google.VCDiffFormatExtension;

import java.io.IOException;
import java.util.EnumSet;


/**
 * @author David Ehrmann
 * 
 *  The method calls after construction should follow this pattern:
 *    {{Add|Copy|Run}* Output}*
 *
 * Output() will produce an encoding using the given series of Add, Copy,
 * and/or Run instructions.  One implementation of the interface
 * (VCDiffCodeTableWriter) produces a VCDIFF delta window, but other
 * implementations may be used to produce other output formats, or as test
 * mocks, or to gather encoding statistics.
 */
public interface CodeTableWriterInterface<OUT> {

    /**
     * Initializes the constructed object for use. It will return
     * false if there was an error initializing the object, or true if it
     * was successful.  After the object has been initialized and used,
     * Init() can be called again to restore the initial state of the object.
     *
     * @param dictionary_size size of the dictionary being used
     * @return true if initialization was successful, false otherwise
     */
    boolean Init(int dictionary_size);


    /**
     * Writes the header to the output string.
     * @param format_extensions Flags for enabling features that are extensions to the format
     */
    void WriteHeader(OUT out, EnumSet<VCDiffFormatExtension> format_extensions) throws IOException;

    /**
     * Encode an ADD opcode with the "size" length starting at offset
     * @param data data to add
     * @param offset offset in data to start from
     * @param length  total bytes to add
     */
    void Add(byte[] data, int offset, int length);

    /**
     * Encode a COPY opcode with args "offset" (into dictionary) and "size" bytes.
     * @param offset
     * @param size
     */
    void Copy(int offset, int size);

    /**
     * Encode a RUN opcode for "size" copies of the value "byte".
     * @param size
     * @param b
     */
    void Run(int size, byte b);

    /**
     * Appends the encoded delta window to the output
     * string.  The output string is not null-terminated and may contain embedded
     * '\0' characters.
     * @param checksum
     */
    void AddChecksum(int checksum);

    /**
     * Appends the encoded delta window to the output
     * string.  The output string is not null-terminated and may contain embedded
     * '\0' characters.
     */
    void Output(OUT out) throws IOException;

    /**
     * Finishes encoding.
     */
    void FinishEncoding(OUT out) throws IOException;
}
