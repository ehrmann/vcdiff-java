package com.davidehrmann.vcdiff;

import com.davidehrmann.vcdiff.google.VCDiffFormatExtensionFlag;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class JSONCodeTableWriterTest {
    private JSONCodeTableWriter coder_ = null;
    private StringWriter writer = null;

    @Before
    public void init() {
        writer = new StringWriter();
        coder_ = new JSONCodeTableWriter();
        coder_.WriteHeader(writer, EnumSet.noneOf(VCDiffFormatExtensionFlag.class));
        assertTrue(coder_.Init(0));
    }

    @Test
    public void nullTest() throws IOException {
        coder_.FinishEncoding(writer);
        assertEquals("", writer.toString());
    }

    @Test
    public void Empty() throws IOException {
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[]", writer.toString());
    }

    @Test
    public void Add() throws IOException {
        coder_.Add("123".getBytes("US-ASCII"), 0, 3);
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[\"123\"]", writer.toString());
    }

    @Test
    public void Copy() throws IOException {
        coder_.Copy(3, 5);
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[3,5]", writer.toString());
    }

    @Test
    public void Run() throws IOException {
        coder_.Run(3, (byte) 'a');
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[\"aaa\"]", writer.toString());
    }

    @Test
    public void AddEscaped() throws IOException {
        coder_.Add("\n\b\r".getBytes("US-ASCII"), 0, 3);
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[\"\\n\\b\\r\"]", writer.toString());
    }

    @Test
    public void AddCopyAdd() throws IOException {
        coder_.Add("abc".getBytes("US-ASCII"), 0, 3);
        coder_.Copy(3, 5);
        coder_.Add("defghij".getBytes("US-ASCII"), 0, 7);
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[\"abc\",3,5,\"defghij\"]", writer.toString());
    }

    @Test
    public void AddOutputAddOutputToSameString() throws IOException {
        coder_.Add("abc".getBytes("US-ASCII"), 0, 3);
        coder_.Output(writer);
        assertEquals("[\"abc\"", writer.toString());
        coder_.Add("def".getBytes("US-ASCII"), 0, 3);
        coder_.Output(writer);
        coder_.FinishEncoding(writer);
        assertEquals("[\"abc\",\"def\"]", writer.toString());
    }

    // AddOutputAddOutputToDifferentString isn't tested because the writer isn't reusable
}
