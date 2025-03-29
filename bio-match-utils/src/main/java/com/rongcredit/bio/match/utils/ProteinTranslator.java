package com.rongcredit.bio.match.utils;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class ProteinTranslator implements Serializable {

	private HashMap<String, String> codons;

	public ProteinTranslator() {
		codons = new HashMap<>();
		// init
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
	 * Translate the giving DNA sequence to protein
	 * 
	 * @param sequence the giving DNA sequence
	 * @return String If there is no associated codon, null is returned
	 */
	public String translate(String sequence) {
		if (sequence == null || sequence.isBlank()) {
			throw new IllegalArgumentException("The DNA/RNA sequence can not be null");
		}
		final int length = sequence.length();
		if (length % 3 != 0) {
			throw new IllegalArgumentException("The DNA/RNA sequence is illegal");
		}
		StringBuilder protein = new StringBuilder();
		for (int i = 0; i < length; i += 3) {
			String subSequence = sequence.substring(i, i + 3);
			// TODO how to translate if there has no condo defined?
			String condo = codons.get(subSequence);
			protein.append(condo);
		}
		return protein.toString();
	}

	public static void main(String[] args) {
		ProteinTranslator translator = new ProteinTranslator();
		String RNA = "GGGGGTGAC";
		System.out.println(translator.translate(RNA));
	}
}
