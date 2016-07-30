An in-progress Java implementation of [VCDIFF](https://github.com/google/open-vcdiff) (RFC3284).

# TODO:

This feels brittle:
        VCDiffStreamingEncoder<OutputStream> embedded_null_encoder = new BaseVCDiffStreamingEncoder<OutputStream>(
                interleavedCodeTableWriter,
                embedded_null_dictionary,
                EnumSet.of(VCD_FORMAT_INTERLEAVED, VCD_FORMAT_CHECKSUM),

```

# See also
* [Diffable](https://web.archive.org/web/20120301201412/http://code.google.com/p/diffable/)
