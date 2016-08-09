[![Build Status](https://travis-ci.org/ehrmann/vcdiff-java.svg?branch=master)](https://travis-ci.org/ehrmann/vcdiff-java)
[![Coverage Status](https://coveralls.io/repos/github/ehrmann/vcdiff-java/badge.svg?branch=master)](https://coveralls.io/github/ehrmann/vcdiff-java?branch=master)

# VCDiff-java

A Java port of Google's [open-vcdiff](https://github.com/google/open-vcdiff) vcdiff (RFC3284) implementation.

## Usage
### Encoding (compressing)
```
byte[] dictionary = ...;
byte[] dataToCompress = ...;
OutputStream compressedData = ...;

VCDiffStreamingEncoder<OutputStream> encoder = EncoderBuilder.builder()
    .withDictionary(dictionary)
    .buildStreaming();

if (!encoder.StartEncoding(compressedData) ||
        !encoder.EncodeChunk(dataToCompress, 0, dataToCompress.length, compressedData) ||
        !encoder.FinishEncoding(compressedData)) {
    throw new IOException("Failed to compress data");
}
```
### Decoding (decompressing)

## Command line usage


## See also
* [Femtozip](https://github.com/gtoubassi/femtozip) (includes dictionary generator)
* [Diffable](https://web.archive.org/web/20120301201412/http://code.google.com/p/diffable/)
