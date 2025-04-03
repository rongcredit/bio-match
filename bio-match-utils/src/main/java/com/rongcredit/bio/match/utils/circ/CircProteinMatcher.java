package com.rongcredit.bio.match.utils.circ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.rongcredit.bio.match.utils.MatchResult;
import com.rongcredit.bio.match.utils.ProteinMatcher;
import com.rongcredit.bio.match.utils.ProteinTranslator;
import com.rongcredit.bio.match.utils.RNAProvider;
import com.rongcredit.bio.match.utils.TranslateResult;

public class CircProteinMatcher implements ProteinMatcher {

    private Map<String, CircProtein> sequence2ProteinMapCache = new HashMap<>();
    private final ProteinTranslator  proteinTranslator        = new ProteinTranslator();
    private ReentrantLock            cacheLock                = new ReentrantLock();
    private int                      circLoop                 = 4;

    private CircProtein toCirc(final String sequence, int loop) {
        StringBuilder builder = new StringBuilder();
        String remainSeq = null;
        List<Integer> boundarys = new ArrayList<>();
        int boundary = 0;
        for (int i = 0; i < loop; i++) {
            String subSeq;
            int length = sequence.length();
            if (remainSeq != null && !remainSeq.isBlank() && remainSeq.length() < 3) {
                subSeq = remainSeq + sequence;
                length -= (3 - remainSeq.length());
            } else {
                subSeq = sequence;
            }

            TranslateResult translateResult = proteinTranslator.translate(subSeq);
            String protein = translateResult.getProtein();
            builder.append(protein);
            boundary += length / 3;

            remainSeq = translateResult.getRemainSequence();
            if (remainSeq != null && !remainSeq.isBlank()) {
                boundary += 1;
            }
            // ignore the last boundary
            // append boundary
            if ((i + 1) < loop) {
                boundarys.add(boundary);
            }
        }
        return new CircProtein(builder.toString(), boundarys);
    }

    private CircProtein translate(final String sequence) {
        if (sequence == null) {
            throw new IllegalArgumentException("The DNA/RNA sequence can not be null");
        }
        String normalizedSequence = sequence.toUpperCase();
        CircProtein protein = sequence2ProteinMapCache.get(normalizedSequence);
        if (protein != null) {
            return protein;
        }
        cacheLock.lock();
        try {
            if (protein == null) {
                protein = toCirc(normalizedSequence, circLoop);
                sequence2ProteinMapCache.put(sequence, protein);
            }
            return protein;
        } finally {
            cacheLock.unlock();
        }
    }

    public static void main(String[] args) {
        CircProteinMatcher matcher = new CircProteinMatcher();
        final String rna = "ATTGCAGGAGGAGATGCTTCAGAGAGAGGAAGCCGAAAACACCCTGCAATCTTTCAGACAGGATGTGATTACAAGGATGACGACGATAAGTGACAATGCGTCTCTGGCACGTCTTGACCTTGAACGCAAAGTGGAATCTTTGCAAGAAGAGATTGCCTTTTTGAAGAAACTCCACGAAGAGGAAATCCAGGAGCTGCAGGCTCAGATTCAGGAACAGCATGTCCAAATCGATGTGGATGTTTCCAAGCCTGACCTCACGGCTGCCCTGCGTGACGTACGTCAGCAATATGAAAGTGTGGCTGCCAAGAACCTGCAGGAGGCAGAAGAATGGTACAAATCCAAGTTTGCTGACCTCTCTGAGGCTGCCAACCGGAACAATGACGCCCTGCGCCAGGCAAAGCAGGAGTCCACTGAGTACCGGAGACAGGTGCAGTCCCTCACCTGTGAAGTGGATGCCCTTAAAGGAACCAATGAGTCCCTGGAACGCCAGATGCGTGAAATGGAAGAGAACTTTGCCGTTGAAGCTGCTAACTACCAAGACACTATTGGCCGCCTGCAGGATGAGATTCAGAATATGAAGGAGGAAATGGCTCGTCACCTTCGTGAATACCAAGACCTGCTCAATGTTAAGATGGCCCTTGACATTGAGATTGCCACCTACAGGAAGCTGCTGGAAGGCGAGGAGAGCAGGATTTCTCTGCCTCTTCCAAACTTTTCCTCCCTGAACCTGAGGGAAACTAATCTGGATTCACTCCCTCTGGTTGATACCCACTCAAAAAGGACACTTCTGATTAAGACGGTTGAAACTAGAGATGGACAG";
        CircProtein protein = matcher.translate(rna);
        System.out.println(protein.getProtein());
        System.out.println(Arrays.toString(protein.getBoundarys().toArray()));
        matcher.match(null, rna, "VETRDGQIAGGDASER");
    }

    private MatchResult match(final String RNAKey, final String RNA, final String protein) {
        // cache the RNA
        CircProtein circProtein = translate(RNA);
        String target = circProtein.getProtein();
        List<Integer> boundarys = circProtein.getBoundarys();
        // do match
        final int pl = protein.length();
        Integer matchedBoundary = null;
        Integer matchedIndex = null;
        for (int fromIndex = 0; fromIndex < target.length(); fromIndex++) {
            int index = target.indexOf(protein, fromIndex);
            if (index == -1) {
                break;
            }
            fromIndex = index + 1;
            // check boundary
            for (Integer boundary : boundarys) {
                if ((index <= boundary - 2) && ((index + pl) >= boundary + 2)) {
                    matchedBoundary = boundary;
                    break;
                }
            }
            if (matchedBoundary != null) {
                matchedIndex = index;
                break;
            }
        }
        if (matchedIndex != null) {
//			System.out.print(String.format("%d, %d", matchedIndex, matchedBoundary));
        }
        MatchResult result = null;
        if (matchedIndex != null && matchedBoundary != null) {
            result = new MatchResult();
            result.setDnaKey(RNAKey);
            result.setDnaSequence(RNA);
            result.setIndex(matchedIndex);
            result.setBoundary(matchedBoundary);
            result.setProtein(protein);
        }
        return result;
    }

    @Override
    public List<MatchResult> match(final RNAProvider provider, final String protein) {
        if (provider == null || protein == null || protein.isBlank()) {
            return null;
        }
        List<MatchResult> matchedSequences = new ArrayList<>();
        ReentrantLock writeLock = new ReentrantLock();
        provider.entrySet().parallelStream().forEach(entry -> {
            String id = entry.getKey();
            String sequence = entry.getValue();
            if (sequence == null || sequence.isBlank()) {
                return;
            }
            MatchResult result = match(id, sequence, protein.toUpperCase());
            if (result != null) {
                writeLock.lock();
                try {
                    matchedSequences.add(result);
                } finally {
                    writeLock.unlock();
                }
            }
        });
        return matchedSequences;
    }
}
