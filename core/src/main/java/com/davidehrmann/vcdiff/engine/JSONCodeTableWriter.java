// Copyright 2009-2016 Google Inc., David Ehrmann
// Author: James deBoer, David Ehrmann
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

package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.CodeTableWriter;
import com.davidehrmann.vcdiff.FormatExtension;

import java.io.IOException;
import java.util.EnumSet;


public class JSONCodeTableWriter implements CodeTableWriter<Appendable> {

    // Stores the JSON data before it is sent to the OutputString.
    private StringBuilder output = new StringBuilder(1024);

    // Set if some data has been output.
    private boolean outputCalled = false;

    // Set if an opcode has been added.
    private boolean opcodeAdded = false;

    public JSONCodeTableWriter() {
    }

    public void init(int dictionarySize) {
        this.output.append('[');
        this.opcodeAdded = false;
    }

    public void add(final byte[] data, final int offset, final int length) {
        if (offset < 0 || offset + length > data.length) {
            throw new IllegalArgumentException();
        }

        // add leading comma if this is not the first opcode.
        if (opcodeAdded) {
            output.append(',');
        }

        output.append('"');

        for (int i = offset; i < offset + length; i++) {
            JSONEscape(data[i], output);
        }

        output.append('"');
        opcodeAdded = true;
    }

    public void addChecksum(int checksum) {
        throw new UnsupportedOperationException("Checksum not supported");
    }

    public void copy(int offset, int size) {
        // add leading comma if this is not the first opcode.
        if (opcodeAdded) {
            output.append(',');
        }

        output.append(offset);
        output.append(',');
        output.append(size);

        opcodeAdded = true;
    }

    public void finishEncoding(Appendable out) throws IOException {
        if (outputCalled) {
            out.append(']');
        }
    }

    public void output(Appendable out) throws IOException {
        outputCalled = true;
        out.append(output);
        output = new StringBuilder(1024);
    }

    public void run(int size, byte b) {
        // add leading comma if this is not the first opcode.
        if (opcodeAdded) {
            output.append(',');
        }

        output.append('"');

        StringBuilder escapedByte = new StringBuilder(8);
        JSONEscape(b, escapedByte);

        for (int i = 0; i < size; i++) {
            output.append(escapedByte);
        }

        output.append('"');

        opcodeAdded = true;
    }

    @SuppressWarnings("deprecation")
    public void writeHeader(Appendable out, EnumSet<FormatExtension> formatExtensions) throws IOException {
        if (!(formatExtensions.isEmpty() ||
                EnumSet.of(FormatExtension.GOOGLE_VCD_FORMAT_JSON).equals(formatExtensions))) {
            throw new IOException("VCDiffFormatExtensions " + formatExtensions + " no compatible with JSONCodeTableWritar");
        }

        // The JSON format does not need a header.
    }

    static private void JSONEscape(byte b, StringBuilder out) {
        switch (b) {
        case '"': out.append("\\\""); break;
        case '\\': out.append("\\\\"); break;
        case '\b': out.append("\\b"); break;
        case '\f': out.append("\\f"); break;
        case '\n': out.append("\\n"); break;
        case '\r': out.append("\\r"); break;
        case '\t':out.append("\\t"); break;
        default:
            // encode zero as unicode, also all control codes.
            if (b < 32 || b >= 127) {
                out.append(String.format("\\u%04x", b & 0xffff));
            } else {
                out.append((char)b);
            }
        }
    }
}
