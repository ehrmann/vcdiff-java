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

```sh
# Compress original with dictionary dict to compressed.vcdiff
java com.davidehrmann.vcdiff.VCDiffFileBasedCoder encode -dictionary dict -delta compressed.vcdiff -target original

# Decompress compressed.vcdiff with dictionary dict to decompressed
java com.davidehrmann.vcdiff.VCDiffFileBasedCoder decode -dictionary dict -delta compressed.vcdiff -target decompressed

# Usage details
java com.davidehrmann.vcdiff.VCDiffFileBasedCoder help
```

## See also
* [Femtozip](https://github.com/gtoubassi/femtozip) (includes dictionary generator)
* [Diffable](https://web.archive.org/web/20120301201412/http://code.google.com/p/diffable/)
