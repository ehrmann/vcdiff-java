package com.googlecode.jvcdiff;

import java.io.IOException;
import java.util.EnumSet;


/**
 * @author David Ehrmann
 * The method calls after construction *must* conform
 * to the following pattern:
 *    {{Add|Copy|Run}* [AddChecksum] Output}*
 *
 * When Output has been called in this sequence, a complete target window
 * (as defined in RFC 3284 section 4.3) will have been appended to
 * out (unless no calls to Add, Run, or Copy were made, in which
 * case Output will do nothing.)  The output will not be available for use
 * until after each call to Output().
 *
 * NOT threadsafe.
 */
public class VCDiffCodeTableWriter implements CodeTableWriterInterface {

	/**
	 * This constructor uses the default code table.
	 * If interleaved is true, the encoder writes each delta file window
	 * by interleaving instructions and sizes with their corresponding
	 * addresses and data, rather than placing these elements into three
	 * separate sections.  This facilitates providing partially
	 * decoded results when only a portion of a delta file window
	 * is received (e.g. when HTTP over TCP is used as the
	 * transmission protocol.)  The interleaved format is
	 * not consistent with the VCDIFF draft standard.
	 * 
	 * @param interleaved Whether or not to interleave the output data
	 */
	public VCDiffCodeTableWriter(boolean interleaved) {
		// TODO
	}

	/**
	 * 
	 * Uses a non-standard code table and non-standard cache sizes.  The caller
	 * must guarantee that code_table_data remains allocated for the lifetime of
	 * the VCDiffCodeTableWriter object.  Note that this is different from how
	 * VCDiffCodeTableReader::UseCodeTable works.  It is assumed that a given
	 * encoder will use either the default code table or a statically-defined
	 * non-standard code table, whereas the decoder must have the ability to read
	 * an arbitrary non-standard code table from a delta file and discard it once
	 * the file has been decoded.
	 * 
	 * @param interleaved
	 * @param near_cache_size
	 * @param same_cache_size
	 */
	public VCDiffCodeTableWriter(boolean interleaved, int near_cache_size, int same_cache_size, VCDiffCodeTableData code_table_data, short max_mode) {
		// TODO
	}

	public void Add(byte[] data, int offset, int length) {
		// TODO Auto-generated method stub

	}

	public void AddChecksum(int checksum) {
		// TODO Auto-generated method stub

	}

	public void Copy(int offset, int size) {
		// TODO Auto-generated method stub

	}

	public void FinishEncoding() throws IOException {
		// TODO Auto-generated method stub

	}

	public void Output() throws IOException {
		// TODO Auto-generated method stub

	}

	public void Run(int size, byte b) {
		// TODO Auto-generated method stub

	}

	public void WriteHeader(EnumSet<VCDiffFormatExtensionFlags> formatExtensions) {
		// TODO Auto-generated method stub

	}

	public int target_length() {
		// TODO Auto-generated method stub
		return 0;
	}

}
