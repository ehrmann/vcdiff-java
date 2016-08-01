// Copyright 2008 Google Inc.
// Author: Lincoln Smith
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

package com.davidehrmann.vcdiff.google;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// A simpler (non-streaming) interface to the VCDIFF decoder that can be used
// if the entire delta file is available.
public interface VCDiffDecoder {



    // Replaces old contents of "*target" with the result of decoding
    // the bytes found in "encoding."
    //
    // Returns true if "encoding" was a well-formed sequence of
    // instructions, and returns false if not.
    //
    boolean Decode(ByteBuffer dictionary, Charset encoding, OutputStream out);
}
