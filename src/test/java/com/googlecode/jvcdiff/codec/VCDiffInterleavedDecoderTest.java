package com.googlecode.jvcdiff.codec;

import com.googlecode.jvcdiff.VarInt;
import org.junit.Test;

import java.nio.ByteBuffer;

import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_CHECKSUM;
import static com.googlecode.jvcdiff.VCDiffCodeTableWriter.VCD_SOURCE;
import static org.junit.Assert.*;

public class VCDiffInterleavedDecoderTest extends VCDiffDecoderTest {
    private static final byte[] kWindowHeader = {
            VCD_SOURCE,  // Win_Indicator: take source from dictionary
            FirstByteOfStringLength(kDictionary),  // Source segment size
            SecondByteOfStringLength(kDictionary),
            0x00,  // Source segment position: start of dictionary
            0x79,  // Length of the delta encoding
            FirstByteOfStringLength(kExpectedTarget),  // Size of the target window
            SecondByteOfStringLength(kExpectedTarget),
            0x00,  // Delta_indicator (no compression)
            0x00,  // length of data for ADDs and RUNs (unused)
            0x73,  // length of interleaved section
            0x00  // length of addresses for COPYs (unused)
    };

    private static final byte[] kWindowBody = {
            0x13,  // VCD_COPY mode VCD_SELF, size 0
            0x1C,  // Size of COPY (28)
            0x00,  // Address of COPY: Start of dictionary
            0x01,  // VCD_ADD size 0
            0x3D,  // Size of ADD (61)
            // Data for ADD (length 61)
            ' ', 'I', ' ', 'h', 'a', 'v', 'e', ' ', 's', 'a', 'i', 'd', ' ',
            'i', 't', ' ', 't', 'w', 'i', 'c', 'e', ':', '\n',
            'T', 'h', 'a', 't', ' ',
            'a', 'l', 'o', 'n', 'e', ' ', 's', 'h', 'o', 'u', 'l', 'd', ' ',
            'e', 'n', 'c', 'o', 'u', 'r', 'a', 'g', 'e', ' ',
            't', 'h', 'e', ' ', 'c', 'r', 'e', 'w', '.', '\n',
            0x23,  // VCD_COPY mode VCD_HERE, size 0
            0x2C,  // Size of COPY (44)
            0x58,  // HERE mode address (27+61 back from here_address)
            (byte) 0xCB,  // VCD_ADD size 2 + VCD_COPY mode NEAR(1), size 5
            // Data for ADDs: 2nd section (length 2)
            'h', 'r',
            0x2D,  // NEAR(1) mode address (45 after prior address)
            0x0A,  // VCD_ADD size 9
            // Data for ADDs: 3rd section (length 9)
            'W', 'h', 'a', 't', ' ',
            'I', ' ', 't', 'e',
            0x00,  // VCD_RUN size 0
            0x02,  // Size of RUN (2)
            // Data for RUN: 4th section (length 1)
            'l',
            0x01,  // VCD_ADD size 0
            0x1B,  // Size of ADD (27)
            // Data for ADD: 4th section (length 27)
            ' ', 'y', 'o', 'u', ' ',
            't', 'h', 'r', 'e', 'e', ' ', 't', 'i', 'm', 'e', 's', ' ', 'i', 's', ' ',
            't', 'r', 'u', 'e', '.', '\"', '\n'
    };

    public VCDiffInterleavedDecoderTest() {
        UseInterleavedFileHeader();
        delta_window_header_ = kWindowHeader.clone();
        delta_window_body_ = kWindowBody.clone();
    }

    // Test headers, valid and invalid.

    @Test
    public void DecodeHeaderOnly() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_header_,
                0,
                delta_file_header_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void PartialHeaderNotEnough() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_header_,
                0,
                delta_file_header_.length - 2,
                output_));
        assertFalse(decoder_.FinishDecoding());
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void BadMagicNumber() throws Exception {
        delta_file_[1] = (byte) ('Q' | 0x80);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void BadVersionNumber() throws Exception {
        delta_file_[3] = 0x01;
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void SecondaryCompressionNotSupported() throws Exception {
        delta_file_[4] = 0x01;
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void Decode() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void DecodeWithChecksum() throws Exception {
        ComputeAndAddChecksum();
        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void ChecksumDoesNotMatch() throws Exception {
        AddChecksum(0xBADBAD);
        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void ChecksumIsInvalid64BitVarint() throws Exception {
        final byte[] kInvalidVarint = {
                (byte) 0x81, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00 };
        delta_window_header_[0] |= VCD_CHECKSUM;
        delta_window_header_ = ArraysExtra.concat(delta_window_header_, kInvalidVarint);

        // delta_window_header_

        // Adjust delta window size to include size of invalid Varint.
        byte[] size_of_invalid_varint = new byte[VarInt.calculateIntLength(delta_window_header_[4] + kInvalidVarint.length)];
        VarInt.putInt(ByteBuffer.wrap(size_of_invalid_varint), delta_window_header_[4] + kInvalidVarint.length);

        delta_window_header_ = ArraysExtra.replace(delta_window_header_, 4, 1, size_of_invalid_varint);
        InitializeDeltaFile();
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    // Remove one byte from the length of the chunk to process, and
    // verify that an error is returned for FinishDecoding().
    @Test
    public void FinishAfterDecodingPartialWindow() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length - 1,
                output_));
        assertFalse(decoder_.FinishDecoding());
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void FinishAfterDecodingPartialWindowHeader() throws Exception {
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_header_.length
                        + delta_window_header_.length - 1,
                output_));
        assertFalse(decoder_.FinishDecoding());
        // The decoder should not create more target bytes than were expected.
        assertTrue(expected_target_.length >= output_.size());
    }

    @Test
    public void TargetMatchesWindowSizeLimit() throws Exception {
        decoder_.SetMaximumTargetWindowSize(expected_target_.length);
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void TargetMatchesFileSizeLimit() throws Exception {
        decoder_.SetMaximumTargetFileSize(expected_target_.length);
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());
    }

    @Test
    public void TargetExceedsWindowSizeLimit() throws Exception {
        decoder_.SetMaximumTargetWindowSize(expected_target_.length - 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void TargetExceedsFileSizeLimit() throws Exception {
        decoder_.SetMaximumTargetFileSize(expected_target_.length - 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    // Fuzz bits to make sure decoder does not violently crash.
    // This test has no expected behavior except that no crashes should occur.
    // In some cases, changing bits will still decode to the correct target;
    // for example, changing unused bits within a bitfield.
    @Test
    public void FuzzBits() throws Exception {
        while (FuzzOneByteInDeltaFile()) {
            decoder_.StartDecoding(dictionary_);
            if (decoder_.DecodeChunk(delta_file_,
                    0,
                    delta_file_.length,
                    output_)) {
                decoder_.FinishDecoding();
            }
            InitializeDeltaFile();
            output_.reset();
        }
    }

    // If a checksum is present, then fuzzing any of the bits may produce an error,
    // but it should not result in an incorrect target being produced without
    // an error.
    @Test
    public void FuzzBitsWithChecksum() throws Exception {
        ComputeAndAddChecksum();
        InitializeDeltaFile();
        while (FuzzOneByteInDeltaFile()) {
            decoder_.StartDecoding(dictionary_);
            if (decoder_.DecodeChunk(delta_file_,
                    0,
                    delta_file_.length,
                    output_)) {
                if (decoder_.FinishDecoding()) {
                    // Decoding succeeded.  Make sure the correct target was produced.
                    assertArrayEquals(expected_target_, output_.toByteArray());
                }
            } else {
                assertArrayEquals(new byte[0], output_.toByteArray());
            }
            InitializeDeltaFile();
            output_.reset();
        }
    }

    @Test
    public void CopyMoreThanExpectedTarget() throws Exception {
        delta_file_[delta_file_header_.length + 0x0C] =
                FirstByteOfStringLength(kExpectedTarget);
        delta_file_[delta_file_header_.length + 0x0D] =
                (byte) (SecondByteOfStringLength(kExpectedTarget) + 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopySizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 0x0C] = 0;
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopySizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 0x0C];
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopySizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 0x0C];
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopySizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x0C, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopySizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x0C, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopySizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x0C, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressBeyondHereAddress() throws Exception {
        delta_file_[delta_file_header_.length + 0x0D] =
                FirstByteOfStringLength(kDictionary);
        delta_file_[delta_file_header_.length + 0x0E] =
                SecondByteOfStringLength(kDictionary);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x0D, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x0D, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void CopyAddressInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x0D, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddMoreThanExpectedTarget() throws Exception {
        delta_file_[delta_file_header_.length + 0x0F] =
                FirstByteOfStringLength(kExpectedTarget);
        delta_file_[delta_file_header_.length + 0x10] =
                (byte) (SecondByteOfStringLength(kExpectedTarget) + 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 0x0F] = 0;
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 0x0F];
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 0x0F];
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x0F, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x0F, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void AddSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x0F, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunMoreThanExpectedTarget() throws Exception {
        delta_file_[delta_file_header_.length + 0x5F] =
                FirstByteOfStringLength(kExpectedTarget);
        delta_file_[delta_file_header_.length + 0x60] =
                (byte) (SecondByteOfStringLength(kExpectedTarget) + 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunSizeZero() throws Exception {
        delta_file_[delta_file_header_.length + 0x5F] = 0;
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunSizeTooLargeByOne() throws Exception {
        ++delta_file_[delta_file_header_.length + 0x5F];
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunSizeTooSmallByOne() throws Exception {
        --delta_file_[delta_file_header_.length + 0x5F];
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunSizeMaxInt() throws Exception {
        WriteMaxVarintAtOffset(0x5F, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunSizeNegative() throws Exception {
        WriteNegativeVarintAtOffset(0x5F, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    @Test
    public void RunSizeInvalid() throws Exception {
        WriteInvalidVarintAtOffset(0x5F, 1);
        decoder_.StartDecoding(dictionary_);
        assertFalse(decoder_.DecodeChunk(delta_file_,
                0,
                delta_file_.length,
                output_));
        assertArrayEquals(new byte[0], output_.toByteArray());
    }

    // TODO
    /*
    #if defined(HAVE_MPROTECT) && \
            (defined(HAVE_MEMALIGN) || defined(HAVE_POSIX_MEMALIGN))
    @Test
    public void ShouldNotReadPastEndOfBuffer() throws Exception {
        // Allocate two memory pages.
        const int page_size = getpagesize();
        void* two_pages = NULL;
        #ifdef HAVE_POSIX_MEMALIGN
        posix_memalign(&two_pages, page_size, 2 * page_size);
        #else  // !HAVE_POSIX_MEMALIGN
        two_pages = memalign(page_size, 2 * page_size);
        #endif  // HAVE_POSIX_MEMALIGN
        char* const first_page = reinterpret_cast<char*>(two_pages);
        char* const second_page = first_page + page_size;

        // Place the delta string at the end of the first page.
        char* delta_with_guard = second_page - delta_file_.length;
        memcpy(delta_with_guard, delta_file_, delta_file_.length);

        // Make the second page unreadable.
        mprotect(second_page, page_size, PROT_NONE);

        // Now perform the decode operation, which will cause a segmentation fault
        // if it reads past the end of the buffer.
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_with_guard,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());

        // Undo the mprotect.
        mprotect(second_page, page_size, PROT_READ|PROT_WRITE);
        free(two_pages);
    }

    @Test
    public void ShouldNotReadPastBeginningOfBuffer() throws Exception {
        // Allocate two memory pages.
        const int page_size = getpagesize();
        void* two_pages = NULL;
        #ifdef HAVE_POSIX_MEMALIGN
        posix_memalign(&two_pages, page_size, 2 * page_size);
        #else  // !HAVE_POSIX_MEMALIGN
        two_pages = memalign(page_size, 2 * page_size);
        #endif  // HAVE_POSIX_MEMALIGN
        char* const first_page = reinterpret_cast<char*>(two_pages);
        char* const second_page = first_page + page_size;

        // Make the first page unreadable.
        mprotect(first_page, page_size, PROT_NONE);

        // Place the delta string at the beginning of the second page.
        char* delta_with_guard = second_page;
        memcpy(delta_with_guard, delta_file_, delta_file_.length);

        // Now perform the decode operation, which will cause a segmentation fault
        // if it reads past the beginning of the buffer.
        decoder_.StartDecoding(dictionary_);
        assertTrue(decoder_.DecodeChunk(delta_with_guard,
                delta_file_.length,
                output_));
        assertTrue(decoder_.FinishDecoding());
        assertArrayEquals(expected_target_, output_.toByteArray());

        // Undo the mprotect.
        mprotect(first_page, page_size, PROT_READ|PROT_WRITE);
        free(two_pages);
    }
    #endif  // HAVE_MPROTECT && (HAVE_MEMALIGN || HAVE_POSIX_MEMALIGN)
    */

}
