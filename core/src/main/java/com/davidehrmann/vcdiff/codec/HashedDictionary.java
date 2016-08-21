// Copyright 2007-2016 Google Inc., David Ehrmann
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

package com.davidehrmann.vcdiff.codec;

import com.davidehrmann.vcdiff.VCDiffEngine;

// A HashedDictionary must be constructed from the dictionary data
// in order to use VCDiffStreamingEncoder.  If the same dictionary will
// be used to perform several encoding operations, then the caller should
// create the HashedDictionary once and cache it for reuse.  This object
// is thread-safe: the same const HashedDictionary can be used
// by several threads simultaneously, each with its own VCDiffStreamingEncoder.
//
// dictionary_contents is copied into the HashedDictionary, so the
// caller may free that string, if desired, after the constructor returns.
//
public class HashedDictionary {
    private final VCDiffEngine engine;

    public HashedDictionary(byte[] dictionaryContents) {
        engine = new VCDiffEngine(dictionaryContents);
    }

    public VCDiffEngine engine() { return engine; }
}
