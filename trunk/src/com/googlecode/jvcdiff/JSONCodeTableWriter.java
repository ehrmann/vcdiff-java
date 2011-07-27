package com.googlecode.jvcdiff;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;


public class JSONCodeTableWriter implements CodeTableWriterInterface {

	protected final Writer writer;

	// Stores the JSON data before it is sent to the OutputString.
	private StringBuilder output_ = new StringBuilder(64);

	// The sum of all the size arguments passed to Add(), Copy() and Run().
	private int target_length_ = 0;

	// Set if some data has been output.
	private boolean output_called_ = false;

	public JSONCodeTableWriter(Writer writer) {
		if (writer == null) {
			throw new NullPointerException();
		}

		this.writer = writer;

		this.output_.append('[');
		this.target_length_ = 0;
	}

	public void Add(final byte[] data, final int offset, final int length) {
		if (offset < 0 || offset + length > data.length) {
			throw new IllegalArgumentException();
		}
		
		output_.append('"');
		
		for (int i = offset; i < offset + length; i++) {
			JSONEscape(data[i], output_);
		}
		
		output_.append("\",");
		target_length_ += length;
	}

	public void AddChecksum(int checksum) {

	}

	public void Copy(int offset, int size) {
		output_.append(offset);
		output_.append(',');
		output_.append(size);
		output_.append(',');

		target_length_ += size;
	}

	public void FinishEncoding() throws IOException {
		if (output_called_) {
			writer.append(']');
		}
	}

	public void Output() throws IOException {
		output_called_ = true;
		writer.append(output_);
		output_ = new StringBuilder(64);
		target_length_ = 0;
	}

	public void Run(int size, byte b) {
		output_.append('"');

		StringBuilder escapedByte = new StringBuilder(8);
		JSONEscape(b, escapedByte);
		
		for (int i = 0; i < size; i++) {
			output_.append(escapedByte);
		}

		output_.append("\",");
		target_length_ += size;
	}

	public void WriteHeader(EnumSet<VCDiffFormatExtensionFlags> formatExtensions) {
		// The JSON format does not need a header.
	}

	public int target_length() {
		return target_length_;
	}

	static protected void JSONEscape(byte b, StringBuilder out) {
		switch (b) {
		case '"': out.append("\\\""); break;
		case '\\': out.append("\\\\"); break;
		case '\b': out.append("\\b"); break;
		case '\f': out.append("\\f"); break;
		case '\n': out.append("\\n"); break;
		case '\r': out.append("\\r"); break;
		case '\t':out.append("\\t"); break;
		default:
			// encode zero as unicode, also all control codes.
			if (b < 32 || b >= 127) {
				out.append(String.format("\\u%04x", b & 0xffff));
			} else {
				out.append((char)b);
			}
		}
	}
}
