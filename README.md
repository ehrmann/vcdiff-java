[![Build Status](https://travis-ci.org/ehrmann/vcdiff-java.svg?branch=master)](https://travis-ci.org/ehrmann/vcdiff-java)
[![Coverage Status](https://coveralls.io/repos/github/ehrmann/vcdiff-java/badge.svg?branch=master)](https://coveralls.io/github/ehrmann/vcdiff-java?branch=master)

# VCDiff-java

A Java port of Google's [open-vcdiff](https://github.com/google/open-vcdiff) vcdiff (RFC3284) implementation.
It's currently synced with [open-vcdiff 0.8.4](https://github.com/google/open-vcdiff/releases/tag/openvcdiff-0.8.4).

## Usage
### Encoding (compressing)
```
byte[] dictionary = ...;
byte[] uncompressedData = ...;
OutputStream compressedData = ...;

// An OutputStream (like GZIPOutputStream) and a stream-based encoder are
// also available from the builder.
VCDiffEncoder<OutputStream> encoder = EncoderBuilder.builder()
    .withDictionary(dictionary)
    .buildSimple();

encoder.Encode(uncompressedData, 0, uncompressedData.length, compressedData);
```
### Decoding (decompressing)
```
byte[] dictionary = ...;
byte[] compressedData = ...;
OutputStream uncompressedData = ...;

// An InputStream (like GZIPInputStream) and a stream-based decoder are
// also available from the builder.
VCDiffDecoder decoder = DecoderBuilder.builder().buildSimple();
decoder.Decode(dictionary, compressedData, 0, compressedData.length, uncompressedData);
```

## Command line usage


## See also
* [Femtozip](https://github.com/gtoubassi/femtozip) (includes dictionary generator)
* [Diffable](https://web.archive.org/web/20120301201412/http://code.google.com/p/diffable/)
