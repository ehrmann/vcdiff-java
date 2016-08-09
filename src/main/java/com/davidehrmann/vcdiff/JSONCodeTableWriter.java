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

package com.davidehrmann.vcdiff;

import com.davidehrmann.vcdiff.google.VCDiffFormatExtension;

import java.io.IOException;
import java.util.EnumSet;


public class JSONCodeTableWriter implements CodeTableWriterInterface<Appendable> {

    // Stores the JSON data before it is sent to the OutputString.
    private StringBuilder output_ = new StringBuilder(1024);

    // Set if some data has been output.
    private boolean output_called_ = false;

    // Set if an opcode has been added.
    private boolean opcode_added_ = false;

    public JSONCodeTableWriter() {
    }

    @Override
    public boolean Init(int dictionary_size) {
        this.output_.append('[');
        this.opcode_added_ = false;
        return true;
    }

    public void Add(final byte[] data, final int offset, final int length) {
        if (offset < 0 || offset + length > data.length) {
            throw new IllegalArgumentException();
        }

        // Add leading comma if this is not the first opcode.
        if (opcode_added_) {
            output_.append(',');
        }

        output_.append('"');

        for (int i = offset; i < offset + length; i++) {
            JSONEscape(data[i], output_);
        }

        output_.append('"');
        opcode_added_ = true;
    }

    public void AddChecksum(int checksum) {

    }

    public void Copy(int offset, int size) {
        // Add leading comma if this is not the first opcode.
        if (opcode_added_) {
            output_.append(',');
        }

        output_.append(offset);
        output_.append(',');
        output_.append(size);

        opcode_added_ = true;
    }

    public void FinishEncoding(Appendable out) throws IOException {
        if (output_called_) {
            out.append(']');
        }
    }

    public void Output(Appendable out) throws IOException {
        output_called_ = true;
        out.append(output_);
        output_ = new StringBuilder(1024);
    }

    public void Run(int size, byte b) {
        // Add leading comma if this is not the first opcode.
        if (opcode_added_) {
            output_.append(',');
        }

        output_.append('"');

        StringBuilder escapedByte = new StringBuilder(8);
        JSONEscape(b, escapedByte);

        for (int i = 0; i < size; i++) {
            output_.append(escapedByte);
        }

        output_.append('"');

        opcode_added_ = true;
    }

    public void WriteHeader(Appendable out, EnumSet<VCDiffFormatExtension> formatExtensions) throws IOException {
        if (!(formatExtensions.isEmpty() ||
                EnumSet.of(VCDiffFormatExtension.VCD_FORMAT_JSON).equals(formatExtensions))) {
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
