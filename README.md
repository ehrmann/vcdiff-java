[![Build Status](https://travis-ci.org/ehrmann/vcdiff-java.svg?branch=master)](https://travis-ci.org/ehrmann/vcdiff-java)
[![Coverage Status](https://coveralls.io/repos/github/ehrmann/vcdiff-java/badge.svg?branch=master)](https://coveralls.io/github/ehrmann/vcdiff-java?branch=master)

# VCDiff-java

A Java port of Google's [open-vcdiff](https://github.com/google/open-vcdiff) vcdiff (RFC3284) implementation.
It's currently synced with [open-vcdiff 0.8.4](https://github.com/google/open-vcdiff/releases/tag/openvcdiff-0.8.4).

## Usage
### Encoding (compressing)
```java
byte[] dictionary = ...;
byte[] uncompressedData = ...;
OutputStream compressedData = ...;

// OutputStream (like GZIPOutputStream) and stream-based encoders are
// also available from the builder.
VCDiffEncoder<OutputStream> encoder = EncoderBuilder.builder()
    .withDictionary(dictionary)
    .buildSimple();

encoder.Encode(uncompressedData, compressedData);
```
### Decoding (decompressing)
```java
byte[] dictionary = ...;
byte[] compressedData = ...;
OutputStream uncompressedData = ...;

// InputStream (like GZIPInputStream) and stream-based decoders are
// also available from the builder.
VCDiffDecoder decoder = DecoderBuilder.builder().buildSimple();
decoder.Decode(dictionary, compressedData, uncompressedData);
```

## Command line usage

The command line wrapper for java-vcdiff is generally compatble with the open-vcdiff implementation:

```
Usage: java com.davidehrmann.vcdiff.VCDiffFileBasedCoder encode|decode [command options]
  Commands:
    encode(delta)      Create delta file from dictionary and target file
      Usage: encode(delta) [options]
        Options:
          -buffersize, --buffersize
             Buffer size for reading input file
             Default: 1048576
          -checksum, --checksum
             Include an Adler32 checksum of the target data when encoding
             Default: false
          -delta, --delta
             Encoded delta file (default is stdout for encode, stdin for decode)
        * -dictionary, --dictionary
             File containing dictionary data (required)
          -interleaved, --interleaved
             Use interleaved format
             Default: false
          -json, --json
             output diff in the JSON format when encoding
             Default: false
          -max_target_file_size, --max_target_file_size
             Maximum target file size allowed by decoder
             Default: 67108864
          -max_target_window_size, --max_target_window_size
             Maximum target window size allowed by decoder
             Default: 67108864
          -stats, --stats
             Report compression percentage
             Default: false
          -target, --target
             Target file (default is stdin for encode, stdout for decode)
          -target_matches, --target_matches
             Find duplicate strings in target data as well as dictionary data
             Default: false

    decode(patch)      Reconstruct target file from dictionary and delta file
      Usage: decode(patch) [options]
        Options:
          -allow_vcd_target, --allow_vcd_target
             If false, the decoder issues an error when the VCD_TARGET flag is
             encountered
             Default: true
          -buffersize, --buffersize
             Buffer size for reading input file
             Default: 1048576
          -delta, --delta
             Encoded delta file (default is stdout for encode, stdin for decode)
        * -dictionary, --dictionary
             File containing dictionary data (required)
          -max_target_file_size, --max_target_file_size
             Maximum target file size allowed by decoder
             Default: 67108864
          -max_target_window_size, --max_target_window_size
             Maximum target window size allowed by decoder
             Default: 67108864
          -stats, --stats
             Report compression percentage
             Default: false
          -target, --target
             Target file (default is stdin for encode, stdout for decode)
```

## See also
* [Femtozip](https://github.com/gtoubassi/femtozip) (includes dictionary generator)
* [Diffable](https://web.archive.org/web/20120301201412/http://code.google.com/p/diffable/)
