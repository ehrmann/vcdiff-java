package com.googlecode.jvcdiff;

import java.io.IOException;
import java.util.EnumSet;


/**
 * @author David Ehrmann
 * 
 *  The method calls after construction should follow this pattern:
 *    {{Add|Copy|Run}* Output}*
 *
 * Output() will produce an encoding using the given series of Add, Copy,
 * and/or Run instructions.  One implementation of the interface
 * (VCDiffCodeTableWriter) produces a VCDIFF delta window, but other
 * implementations may be used to produce other output formats, or as test
 * mocks, or to gather encoding statistics.
 */
public interface CodeTableWriterInterface {

	/**
	 * Writes the header to the output string.
	 * @param format_extensions
	 */
	public void WriteHeader(EnumSet<VCDiffFormatExtensionFlags> format_extensions);

	/**
	 * Encode an ADD opcode with the "size" bytes starting at data
	 * @param data
	 * @param offset
	 * @param length
	 */
	public void Add(byte[] data, int offset, int length);

	/**
	 * Encode a COPY opcode with args "offset" (into dictionary) and "size" bytes.
	 * @param offset
	 * @param size
	 */
	public void Copy(int offset, int size);

	/**
	 * Encode a RUN opcode for "size" copies of the value "byte".
	 * @param size
	 * @param b
	 */
	public void Run(int size, byte b);

	/**
	 * Appends the encoded delta window to the output
	 * string.  The output string is not null-terminated and may contain embedded
	 * '\0' characters.
	 * @param checksum
	 */
	public void AddChecksum(int checksum);

	/**
	 * Appends the encoded delta window to the output
	 * string.  The output string is not null-terminated and may contain embedded
	 * '\0' characters.
	 */
	public void Output() throws IOException;

	/**
	 * Finishes encoding.
	 */
	public void FinishEncoding() throws IOException;

	/**
	 * Returns the number of target bytes processed, which is the sum of all the
	 * size arguments passed to Add(), Copy(), and Run().
	 * @return
	 */
	public int target_length();
}
