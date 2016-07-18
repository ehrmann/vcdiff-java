
package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.VCDiffCodeTableData;
import org.junit.Test;

import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_RUN;
import static org.junit.Assert.*;

public class VCDiffCustomCodeTableDecoderTest extends VCDiffCustomCodeTableDecoderTestBase {
    @Test
    public void CustomCodeTableEncodingMatches() throws Exception {
        VCDiffCodeTableData custom_code_table = VCDiffCodeTableData.kDefaultCodeTableData.clone();
        custom_code_table.inst1[3] = VCD_RUN;
        custom_code_table.size1[17] = 27;
        custom_code_table.size1[18] = 61;
        custom_code_table.size1[34] = 28;
        custom_code_table.size1[50] = 44;

        decoder_.StartDecoding(custom_code_table.getBytes());
        assertTrue(decoder_.DecodeChunk(kEncodedCustomCodeTable,
                0,
                kEncodedCustomCodeTable.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertEquals(custom_code_table.getBytes().length, output_.size());
        final VCDiffCodeTableData decoded_table = new VCDiffCodeTableData(output_.toByteArray());
        assertEquals(VCD_RUN, decoded_table.inst1[0]);
        assertEquals(VCD_RUN, decoded_table.inst1[3]);
        assertEquals(27, decoded_table.size1[17]);
        assertEquals(61, decoded_table.size1[18]);
        assertEquals(28, decoded_table.size1[34]);
        assertEquals(44, decoded_table.size1[50]);
        for (int i = 0; i < VCDiffCodeTableData.kCodeTableSize; ++i) {
            assertEquals(custom_code_table.inst1[i], decoded_table.inst1[i]);
            assertEquals(custom_code_table.inst2[i], decoded_table.inst2[i]);
            assertEquals(custom_code_table.size1[i], decoded_table.size1[i]);
            assertEquals(custom_code_table.size2[i], decoded_table.size2[i]);
            assertEquals(custom_code_table.mode1[i], decoded_table.mode1[i]);
            assertEquals(custom_code_table.mode2[i], decoded_table.mode2[i]);
        }
    }

    @Test
    public void DecodeUsingCustomCodeTable() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void IncompleteCustomCodeTable() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_header_,
                0,
                delta_file_header_.length - 1,
                output_));
        assertFalse(decoder_.FinishDecoding());
        assertArrayEquals(new byte[0], output_.toByteArray());
    }
}
