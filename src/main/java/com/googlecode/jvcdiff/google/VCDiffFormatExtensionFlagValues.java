// Copyright 2009 Google Inc.
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

package com.googlecode.jvcdiff.google;

public enum VCDiffFormatExtensionFlagValues {

    // No extensions: the encoded format will conform to the RFC
    // draft standard for VCDIFF.
    VCD_STANDARD_FORMAT(0x00),

    // If this flag is specified, then the encoder writes each delta file
    // window by interleaving instructions and sizes with their corresponding
    // addresses and data, rather than placing these elements
    // into three separate sections.  This facilitates providing partially
    // decoded results when only a portion of a delta file window is received
    // (e.g. when HTTP over TCP is used as the transmission protocol.)
    VCD_FORMAT_INTERLEAVED(0x01),

    // If this flag is specified, then an Adler32 checksum
    // of the target window data is included in the delta window.
    VCD_FORMAT_CHECKSUM(0x02),

    // If this flag is specified, the encoder will output a JSON string
    // instead of the VCDIFF file format. If this flag is set, all other
    // flags have no effect.
    VCD_FORMAT_JSON (0x04);

    public final int flag;
    VCDiffFormatExtensionFlagValues(int flag) {
        this.flag = flag;
    }
}
