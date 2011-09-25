package com.googlecode.jvcdiff;

import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_ADD;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_COPY;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_NOOP;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.VCD_RUN;
import static com.googlecode.jvcdiff.VCDiffCodeTableData.kNoOpcode;
import static com.googlecode.jvcdiff.VCDiffInstructionMap.DEFAULT_INSTRUCTION_MAP;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class VCDiffInstructionMapTest {

	@Test
	public void DefaultMapLookupFirstNoopTest() {
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte)0, (byte)255));
		assertEquals(kNoOpcode,DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte)255, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_NOOP, (byte)255, (byte)255));
	}

	@Test
	public void DefaultMapLookupFirstAddTest() {
		assertEquals(2, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)1, (byte)0));
		assertEquals(3, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)2, (byte)0));
		assertEquals(4, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)3, (byte)0));
		assertEquals(5, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)4, (byte)0));
		assertEquals(6, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)5, (byte)0));
		assertEquals(7, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)6, (byte)0));
		assertEquals(8, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)7, (byte)0));
		assertEquals(9, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)8, (byte)0));
		assertEquals(10, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)9, (byte)0));
		assertEquals(11, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)10, (byte)0));
		assertEquals(12, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)11, (byte)0));
		assertEquals(13, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)12, (byte)0));
		assertEquals(14, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)13, (byte)0));
		assertEquals(15, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)14, (byte)0));
		assertEquals(16, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)15, (byte)0));
		assertEquals(17, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)16, (byte)0));
		assertEquals(18, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)17, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)100, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)255, (byte)0));
		assertEquals(1, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)0, (byte)0));

		// Value of "mode" should not matter
		assertEquals(2, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)1, (byte)2));
		assertEquals(2, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_ADD, (byte)1, (byte)255));
	}

	@Test
	public void DefaultMapLookupFirstRun() {
		assertEquals(0, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte)1, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte)255, (byte)0));
		// Value of "mode" should not matter
		assertEquals(0, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_RUN, (byte)0, (byte)2));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode0() {
		assertEquals(19, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)0));
		assertEquals(20, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)0));
		assertEquals(21, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)0));
		assertEquals(22, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)0));
		assertEquals(23, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)0));
		assertEquals(24, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)0));
		assertEquals(25, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)0));
		assertEquals(26, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)0));
		assertEquals(27, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)0));
		assertEquals(28, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)0));
		assertEquals(29, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)0));
		assertEquals(30, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)0));
		assertEquals(31, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)0));
		assertEquals(32, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)0));
		assertEquals(33, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)0));
		assertEquals(34, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)0));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode1() {
		assertEquals(35, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)1));
		assertEquals(36, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)1));
		assertEquals(37, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)1));
		assertEquals(38, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)1));
		assertEquals(39, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)1));
		assertEquals(40, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)1));
		assertEquals(41, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)1));
		assertEquals(42, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)1));
		assertEquals(43, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)1));
		assertEquals(44, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)1));
		assertEquals(45, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)1));
		assertEquals(46, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)1));
		assertEquals(47, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)1));
		assertEquals(48, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)1));
		assertEquals(49, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)1));
		assertEquals(50, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)1));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode2() {
		assertEquals(51, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)2));
		assertEquals(52, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)2));
		assertEquals(53, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)2));
		assertEquals(54, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)2));
		assertEquals(55, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)2));
		assertEquals(56, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)2));
		assertEquals(57, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)2));
		assertEquals(58, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)2));
		assertEquals(59, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)2));
		assertEquals(60, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)2));
		assertEquals(61, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)2));
		assertEquals(62, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)2));
		assertEquals(63, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)2));
		assertEquals(64, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)2));
		assertEquals(65, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)2));
		assertEquals(66, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)2));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode3() {
		assertEquals(67, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)3));
		assertEquals(68, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)3));
		assertEquals(69, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)3));
		assertEquals(70, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)3));
		assertEquals(71, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)3));
		assertEquals(72, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)3));
		assertEquals(73, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)3));
		assertEquals(74, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)3));
		assertEquals(75, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)3));
		assertEquals(76, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)3));
		assertEquals(77, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)3));
		assertEquals(78, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)3));
		assertEquals(79, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)3));
		assertEquals(80, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)3));
		assertEquals(81, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)3));
		assertEquals(82, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)3));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode4() {
		assertEquals(83, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)4));
		assertEquals(84, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)4));
		assertEquals(85, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)4));
		assertEquals(86, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)4));
		assertEquals(87, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)4));
		assertEquals(88, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)4));
		assertEquals(89, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)4));
		assertEquals(90, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)4));
		assertEquals(91, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)4));
		assertEquals(92, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)4));
		assertEquals(93, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)4));
		assertEquals(94, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)4));
		assertEquals(95, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)4));
		assertEquals(96, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)4));
		assertEquals(97, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)4));
		assertEquals(98, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)4));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode5() {
		assertEquals(99, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)5));
		assertEquals(100, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)5));
		assertEquals(101, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)5));
		assertEquals(102, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)5));
		assertEquals(103, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)5));
		assertEquals(104, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)5));
		assertEquals(105, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)5));
		assertEquals(106, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)5));
		assertEquals(107, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)5));
		assertEquals(108, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)5));
		assertEquals(109, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)5));
		assertEquals(110, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)5));
		assertEquals(111, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)5));
		assertEquals(112, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)5));
		assertEquals(113, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)5));
		assertEquals(114, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)5));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode6() {
		assertEquals(115, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)6));
		assertEquals(116, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)6));
		assertEquals(117, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)6));
		assertEquals(118, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)6));
		assertEquals(119, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)6));
		assertEquals(120, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)6));
		assertEquals(121, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)6));
		assertEquals(122, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)6));
		assertEquals(123, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)6));
		assertEquals(124, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)6));
		assertEquals(125, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)6));
		assertEquals(126, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)6));
		assertEquals(127, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)6));
		assertEquals(128, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)6));
		assertEquals(129, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)6));
		assertEquals(130, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)6));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode7() {
		assertEquals(131, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)7));
		assertEquals(132, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)7));
		assertEquals(133, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)7));
		assertEquals(134, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)7));
		assertEquals(135, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)7));
		assertEquals(136, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)7));
		assertEquals(137, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)7));
		assertEquals(138, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)7));
		assertEquals(139, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)7));
		assertEquals(140, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)7));
		assertEquals(141, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)7));
		assertEquals(142, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)7));
		assertEquals(143, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)7));
		assertEquals(144, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)7));
		assertEquals(145, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)7));
		assertEquals(146, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)7));
	}

	@Test
	public void DefaultMapLookupFirstCopyMode8() {
		assertEquals(147, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)0, (byte)8));
		assertEquals(148, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)4, (byte)8));
		assertEquals(149, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)5, (byte)8));
		assertEquals(150, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)6, (byte)8));
		assertEquals(151, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)7, (byte)8));
		assertEquals(152, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)8, (byte)8));
		assertEquals(153, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)9, (byte)8));
		assertEquals(154, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)10, (byte)8));
		assertEquals(155, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)11, (byte)8));
		assertEquals(156, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)12, (byte)8));
		assertEquals(157, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)13, (byte)8));
		assertEquals(158, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)14, (byte)8));
		assertEquals(159, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)15, (byte)8));
		assertEquals(160, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)16, (byte)8));
		assertEquals(161, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)17, (byte)8));
		assertEquals(162, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)18, (byte)8));
	}

	@Test
	public void DefaultMapLookupFirstCopyInvalid() {
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)3, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)3, (byte)3));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupFirstOpcode(VCD_COPY, (byte)255, (byte)0));
	}

	@Test
	public void DefaultMapLookupSecondNoop() {
		// The second opcode table does not store entries for NOOP instructions.
		// Just make sure that a NOOP does not crash the lookup code.
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_NOOP, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_NOOP, (byte)0, (byte)255));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_NOOP, (byte)255, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_NOOP, (byte)255, (byte)255));
	}

	@Test
	public void DefaultMapLookupSecondAdd() {
		assertEquals(247, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_ADD, (byte)1, (byte)0));
		assertEquals(248, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)36, VCD_ADD, (byte)1, (byte)0));
		assertEquals(249, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)52, VCD_ADD, (byte)1, (byte)0));
		assertEquals(250, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)68, VCD_ADD, (byte)1, (byte)0));
		assertEquals(251, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)84, VCD_ADD, (byte)1, (byte)0));
		assertEquals(252, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)100, VCD_ADD, (byte)1, (byte)0));
		assertEquals(253, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)116, VCD_ADD, (byte)1, (byte)0));
		assertEquals(254, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)132, VCD_ADD, (byte)1, (byte)0));
		assertEquals(255, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)148, VCD_ADD, (byte)1, (byte)0));
		// Value of "mode" should not matter
		assertEquals(247, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_ADD, (byte)1, (byte)2));
		assertEquals(247, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_ADD, (byte)1, (byte)255));
		// Only valid 2nd ADD opcode has size 1
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_ADD, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_ADD, (byte)0, (byte)255));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_ADD, (byte)255, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)0, VCD_ADD, (byte)1, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)1, VCD_ADD, (byte)1, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)247, VCD_ADD, (byte)1, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)255, VCD_ADD, (byte)1, (byte)0));
	}

	@Test
	public void DefaultMapLookupSecondRun() {
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)0, VCD_RUN, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_RUN, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_RUN, (byte)0, (byte)255));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_RUN, (byte)255, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)20, VCD_RUN, (byte)255, (byte)255));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)255, VCD_RUN, (byte)0, (byte)0));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode0() {
		assertEquals(163, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)0));
		assertEquals(164, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)5, (byte)0));
		assertEquals(165, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)6, (byte)0));
		assertEquals(166, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)0));
		assertEquals(167, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)5, (byte)0));
		assertEquals(168, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)6, (byte)0));
		assertEquals(169, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)0));
		assertEquals(170, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)5, (byte)0));
		assertEquals(171, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)6, (byte)0));
		assertEquals(172, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)0));
		assertEquals(173, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)5, (byte)0));
		assertEquals(174, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)6, (byte)0));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode1() {
		assertEquals(175, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)1));
		assertEquals(176, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)5, (byte)1));
		assertEquals(177, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)6, (byte)1));
		assertEquals(178, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)1));
		assertEquals(179, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)5, (byte)1));
		assertEquals(180, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)6, (byte)1));
		assertEquals(181, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)1));
		assertEquals(182, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)5, (byte)1));
		assertEquals(183, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)6, (byte)1));
		assertEquals(184, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)1));
		assertEquals(185, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)5, (byte)1));
		assertEquals(186, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)6, (byte)1));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode2() {
		assertEquals(187, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)2));
		assertEquals(188, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)5, (byte)2));
		assertEquals(189, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)6, (byte)2));
		assertEquals(190, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)2));
		assertEquals(191, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)5, (byte)2));
		assertEquals(192, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)6, (byte)2));
		assertEquals(193, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)2));
		assertEquals(194, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)5, (byte)2));
		assertEquals(195, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)6, (byte)2));
		assertEquals(196, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)2));
		assertEquals(197, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)5, (byte)2));
		assertEquals(198, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)6, (byte)2));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode3() {
		assertEquals(199, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)3));
		assertEquals(200, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)5, (byte)3));
		assertEquals(201, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)6, (byte)3));
		assertEquals(202, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)3));
		assertEquals(203, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)5, (byte)3));
		assertEquals(204, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)6, (byte)3));
		assertEquals(205, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)3));
		assertEquals(206, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)5, (byte)3));
		assertEquals(207, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)6, (byte)3));
		assertEquals(208, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)3));
		assertEquals(209, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)5, (byte)3));
		assertEquals(210, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)6, (byte)3));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode4() {
		assertEquals(211, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)4));
		assertEquals(212, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)5, (byte)4));
		assertEquals(213, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)6, (byte)4));
		assertEquals(214, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)4));
		assertEquals(215, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)5, (byte)4));
		assertEquals(216, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)6, (byte)4));
		assertEquals(217, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)4));
		assertEquals(218, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)5, (byte)4));
		assertEquals(219, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)6, (byte)4));
		assertEquals(220, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)4));
		assertEquals(221, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)5, (byte)4));
		assertEquals(222, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)6, (byte)4));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode5() {
		assertEquals(223, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)5));
		assertEquals(224, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)5, (byte)5));
		assertEquals(225, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)6, (byte)5));
		assertEquals(226, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)5));
		assertEquals(227, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)5, (byte)5));
		assertEquals(228, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)6, (byte)5));
		assertEquals(229, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)5));
		assertEquals(230, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)5, (byte)5));
		assertEquals(231, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)6, (byte)5));
		assertEquals(232, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)5));
		assertEquals(233, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)5, (byte)5));
		assertEquals(234, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)6, (byte)5));
	}

	@Test 
	public void DefaultMapLookupSecondCopyMode6() {
		assertEquals(235, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)6));
		assertEquals(236, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)6));
		assertEquals(237, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)6));
		assertEquals(238, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)6));
		assertEquals(239, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)7));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode7() {
		assertEquals(240, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)7));
		assertEquals(241, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)7));
		assertEquals(242, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)7));
	}

	@Test
	public void DefaultMapLookupSecondCopyMode8() {
		assertEquals(243, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)4, (byte)8));
		assertEquals(244, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)3, VCD_COPY, (byte)4, (byte)8));
		assertEquals(245, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)4, VCD_COPY, (byte)4, (byte)8));
		assertEquals(246, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)5, VCD_COPY, (byte)4, (byte)8));
	}

	@Test
	public void DefaultMapLookupSecondCopyInvalid() {
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)0, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)255, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)2, VCD_COPY, (byte)255, (byte)255));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)0, VCD_COPY, (byte)4, (byte)0));
		assertEquals(kNoOpcode, DEFAULT_INSTRUCTION_MAP.LookupSecondOpcode((byte)255, VCD_COPY, (byte)4, (byte)0));
	}
}
