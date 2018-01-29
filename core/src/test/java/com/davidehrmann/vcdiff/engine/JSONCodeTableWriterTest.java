package com.davidehrmann.vcdiff.engine;

import com.davidehrmann.vcdiff.VCDiffFormatExtension;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;


public class JSONCodeTableWriterTest {
    private JSONCodeTableWriter coder_ = null;
    private StringWriter writer = null;

    @Before
    public void init() throws Exception {
        writer = new StringWriter();
        coder_ = new JSONCodeTableWriter();
        coder_.writeHeader(writer, EnumSet.noneOf(VCDiffFormatExtension.class));
        coder_.init(0);
    }

    @Test
    public void nullTest() throws IOException {
        coder_.finishEncoding(writer);
        assertEquals("", writer.toString());
    }

    @Test
    public void Empty() throws IOException {
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[]", writer.toString());
    }

    @Test
    public void Add() throws IOException {
        coder_.add("123".getBytes("US-ASCII"), 0, 3);
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[\"123\"]", writer.toString());
    }

    @Test
    public void Copy() throws IOException {
        coder_.copy(3, 5);
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[3,5]", writer.toString());
    }

    @Test
    public void Run() throws IOException {
        coder_.run(3, (byte) 'a');
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[\"aaa\"]", writer.toString());
    }

    @Test
    public void AddEscaped() throws IOException {
        coder_.add("\n\b\r".getBytes("US-ASCII"), 0, 3);
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[\"\\n\\b\\r\"]", writer.toString());
    }

    @Test
    public void AddCopyAdd() throws IOException {
        coder_.add("abc".getBytes("US-ASCII"), 0, 3);
        coder_.copy(3, 5);
        coder_.add("defghij".getBytes("US-ASCII"), 0, 7);
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[\"abc\",3,5,\"defghij\"]", writer.toString());
    }

    @Test
    public void AddOutputAddOutputToSameString() throws IOException {
        coder_.add("abc".getBytes("US-ASCII"), 0, 3);
        coder_.output(writer);
        assertEquals("[\"abc\"", writer.toString());
        coder_.add("def".getBytes("US-ASCII"), 0, 3);
        coder_.output(writer);
        coder_.finishEncoding(writer);
        assertEquals("[\"abc\",\"def\"]", writer.toString());
    }

    // AddOutputAddOutputToDifferentString isn't tested because the writer isn't reusable
}
