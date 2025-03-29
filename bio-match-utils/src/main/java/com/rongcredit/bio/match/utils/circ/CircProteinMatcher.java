package com.rongcredit.bio.match.utils.circ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.rongcredit.bio.match.utils.ProteinMatcher;
import com.rongcredit.bio.match.utils.ProteinTranslator;
import com.rongcredit.bio.match.utils.RNAProvider;

public class CircProteinMatcher implements ProteinMatcher {

	private Map<String, String> sequence2ProteinMapCache = new HashMap<>();
	private final ProteinTranslator proteinTranslator = new ProteinTranslator();
	private ReentrantLock cacheLock = new ReentrantLock();
	private int circLoop = 4;

	private String toCircRNA(String sequence, int loop) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < loop; i++) {
			builder.append(sequence);
		}
		return builder.toString();
	}

	private String translate(final String sequence) {
		if (sequence == null) {
			throw new IllegalArgumentException("The DNA/RNA sequence can not be null");
		}
		String normalizedSequence = sequence.toUpperCase();
		cacheLock.lock();
		try {
			String protein = sequence2ProteinMapCache.get(normalizedSequence);
			if (protein == null) {
				String circRNA = toCircRNA(normalizedSequence, circLoop);
				protein = proteinTranslator.translate(circRNA);
				sequence2ProteinMapCache.put(sequence, protein);
			}
			return protein;
		} finally {
			cacheLock.unlock();
		}
	}

	private boolean isMatched(final String RNA, final String protein) {
		// cache the RNA
		String proteinTarget = translate(RNA);
		// TODO do match
		
		return false;
	}

	@Override
	public List<String> match(final RNAProvider provider, final String protein) {
		if (provider == null || protein == null || protein.isBlank()) {
			return null;
		}
		List<String> matchedSequences = new ArrayList<>();
		ReentrantLock writeLock = new ReentrantLock();
		provider.parallelStream().forEach(sequence -> {
			if (sequence == null || sequence.isBlank()) {
				return;
			}
			if (isMatched(sequence, protein.toUpperCase())) {
				writeLock.lock();
				try {
					matchedSequences.add(sequence);
				} finally {
					writeLock.unlock();
				}
			}
		});
		return null;
	}
}
