package com.rongcredit.bio.match.utils;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 核酸序列蛋白翻译器。
 * <p>
 * 本类依据标准密码子映射关系，将 DNA 或 RNA 风格的核酸序列按照每三个碱基为一组进行翻译，
 * 输出对应的蛋白序列结果。对于末尾不足完整密码子的残留片段，系统不参与翻译，
 * 而是在 {@link TranslateResult} 中予以保留。
 * </p>
 */
@SuppressWarnings("serial")
public class ProteinTranslator implements Serializable {

	/**
	 * 密码子与氨基酸缩写之间的映射表。
	 */
	private HashMap<String, String> codons;

	/**
	 * 构造翻译器并初始化标准密码子表。
	 */
	public ProteinTranslator() {
		codons = new HashMap<>();
		// 初始化标准密码子与氨基酸缩写之间的对应关系。
		codons.put("TTT", "F");
		codons.put("TTC", "F");
		codons.put("TTA", "L");
		codons.put("TTG", "L");
		codons.put("TCT", "S");
		codons.put("TCC", "S");
		codons.put("TCA", "S");
		codons.put("TCG", "S");
		codons.put("TAT", "Y");
		codons.put("TAC", "Y");
		codons.put("TAA", "*");
		codons.put("TAG", "*");
		codons.put("TGT", "C");
		codons.put("TGC", "C");
		codons.put("TGA", "*");
		codons.put("TGG", "W");
		codons.put("CTT", "L");
		codons.put("CTC", "L");
		codons.put("CTA", "L");
		codons.put("CTG", "L");
		codons.put("CCT", "P");
		codons.put("CCC", "P");
		codons.put("CCA", "P");
		codons.put("CCG", "P");
		codons.put("CAT", "H");
		codons.put("CAC", "H");
		codons.put("CAA", "Q");
		codons.put("CAG", "Q");
		codons.put("CGT", "R");
		codons.put("CGC", "R");
		codons.put("CGA", "R");
		codons.put("CGG", "R");
		codons.put("ATT", "I");
		codons.put("ATC", "I");
		codons.put("ATA", "I");
		codons.put("ATG", "M");
		codons.put("ACT", "T");
		codons.put("ACC", "T");
		codons.put("ACA", "T");
		codons.put("ACG", "T");
		codons.put("AAT", "N");
		codons.put("AAC", "N");
		codons.put("AAA", "K");
		codons.put("AAG", "K");
		codons.put("AGT", "S");
		codons.put("AGC", "S");
		codons.put("AGA", "R");
		codons.put("AGG", "R");
		codons.put("GTT", "V");
		codons.put("GTC", "V");
		codons.put("GTA", "V");
		codons.put("GTG", "V");
		codons.put("GCT", "A");
		codons.put("GCC", "A");
		codons.put("GCA", "A");
		codons.put("GCG", "A");
		codons.put("GAT", "D");
		codons.put("GAC", "D");
		codons.put("GAA", "E");
		codons.put("GAG", "E");
		codons.put("GGT", "G");
		codons.put("GGC", "G");
		codons.put("GGA", "G");
		codons.put("GGG", "G");
	}

	/**
	 * 将输入核酸序列翻译为蛋白序列。
	 * <p>
	 * 本方法按照固定阅读框自序列起始位置开始逐个密码子进行转换。对于末尾不足三个字符的残余序列，
	 * 不参与当前轮翻译，并在返回结果中保留，以便调用方决定是否进行补齐、拼接或后续处理。
	 * </p>
	 *
	 * @param sequence 待翻译的核酸序列
	 * @return 翻译结果对象，包含蛋白序列与未翻译剩余片段
	 * @throws IllegalArgumentException 当输入序列为空时抛出
	 */
	public TranslateResult translate(String sequence) {
		if (sequence == null || sequence.isBlank()) {
			throw new IllegalArgumentException("The DNA/RNA sequence can not be null");
		}
		final int length = sequence.length();
		StringBuilder protein = new StringBuilder();
		int index = 0;
		for (index = 0; index < length && (index + 2) < length; index += 3) {
			String subSequence = sequence.substring(index, index + 3);
			String condo = codons.get(subSequence);
			protein.append(condo);
		}
		TranslateResult result = new TranslateResult(protein.toString(),
				index < length ? sequence.substring(index) : null);
		return result;
	}

	/**
	 * 本地调试入口。
	 *
	 * @param args 启动参数列表
	 */
	public static void main(String[] args) {
		ProteinTranslator translator = new ProteinTranslator();
		String RNA = "GCCATTGATGATTACAAGGATGACGACGATAAGGACAACATGTCCCTTGATGAGATTGAGAAGCTCACATACATTGACAAGTGGTTTTTGTATAAGATGCGTGATATTTTAAACATGGAAAAGACACTGAAAGGCCTCAACAGTGAGTCCATGACAGAAGAAACCCTGAAAAGGGCAAAGGAGATTGGGTTCTCAGATAAGCAGATTTCAAAATGCCTTGGGCTCACTGAGGCCCAGACAAGGGAGCTGAGGTTAAAGAAAAACATCCACCCTTGGGTTAAACAGATTGATACACTGGCTGCAGAATACCCATCAGTAACAAACTATCTCTATGTTACCTACAATGGTCAGGAGCATGATGTCAATTTTGATGACCATGGAATGATGGTGCTAGGCTGTGGTCCATATCACATTGGCAGCAGTGTGGAATTTGATTGGTGTGCTGTCTCTAGTATCCGCACACTGCGTCAACTTGGCAAGAAGACGGTGGTGGTGAATTGCAATCCTGAGACTGTGAGCACAGACTTTGATGAGTGTGACAAACTGTACTTTGAAGAGTTGTCCTTGGAGAGAATCCTAGACATCTACCATCAGGAGGCATGTGGTGGCTGCATCATATCAGTTGGAGGCCAGATTCCAAACAACCTGGCAGTTCCTCTATACAAGAATGGTGTCAAGATCATGGGCACAAGCCCCCTGCAGATCGACAGGGCTGAGGATCGCTCCATCTTCTCAGCTGTCTTGGATGAGCTGAAGGTGGCTCAGGCACCTTGGAAAGCTGTTAATACTTTGAATGAAGCACTGGAATTTGCAAAGTCTGTGGACTACCCCTGCTTGTTGAGGCCTTCCTATGTTTTGAGTGGGTCTGCTATGAATGTGGTATTCTCTGAGGATGAGATGAAAAAATTCCTAGAAGAGGCGACTAGAGTTTCTCAGGAGCACCCAGTGGTGCTGACAAAATTTGTTGAAGGGGCCCGAGAAGTAGAAATGGACGCTGTTGGCAAAGATGGAAGGGTTATCTCTCATGCCATCTCTGAACATGTTGAAGATGCAGGTGTCCACTCGGGAGATGCCACTCTGATGCTGCCCACACAAACCATCAGCCAAGGGGCCATTGAAAAGGTGAAGGATGCTACCCGGAAGATTGCAAAGGCTTTTGCCATCTCTGGTCCATTCAACGTCCAATTTCTTGTCAAAGGAAATGATGTCTTGGTGATTGAGTGTAACTTGAGAGCTTCTCGATCCTTCCCCTTTGTTTCCAAGACTCTTGGGGTTGACTTCATTGATGTGGCCACCAAGGTGATGATTGGAGAGAATGTTGATGAGAAACATCTTCCAACATTGGACCATCCCATAATTCCTGCTGACTATGTTGCAATTAAGGCTCCCATGTTTTCCTGGCCCCGGTTGAGGGATGCTGACCCCATTCTGAGATGTGAGATGGCTTCCACTGGAGAG";
		System.out.println(translator.translate(RNA));
	}
}
