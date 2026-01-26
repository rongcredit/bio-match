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
    private final ProteinTranslator proteinTranslator = new ProteinTranslator();
    private ReentrantLock cacheLock = new ReentrantLock();
    private final boolean checkBoundary;

    public CircProteinMatcher() {
        this.checkBoundary = true;
    }

    public CircProteinMatcher(boolean checkBoundary) {
        this.checkBoundary = checkBoundary;
    }

    private CircProtein toCirc(final String sequence) {
        List<Integer> boundarys = new ArrayList<>();
        int boundary = sequence.length() / 3;
        boundarys.add(boundary);

        List<String> proteins = new ArrayList<>();
        for (int beginIndex = 0; beginIndex < 3; beginIndex++) {
            String subSeq = sequence.substring(beginIndex) + sequence;
            TranslateResult translateResult = proteinTranslator.translate(subSeq);
            String protein = translateResult.getProtein();
            proteins.add(protein);
        }
        return new CircProtein(proteins, boundarys);
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
                protein = toCirc(normalizedSequence);
                sequence2ProteinMapCache.put(sequence, protein);
            }
            return protein;
        } finally {
            cacheLock.unlock();
        }
    }

    public static void main(String[] args) {
        CircProteinMatcher matcher = new CircProteinMatcher(true);
        final String rna = "GTCTGGGATAAAAGTGAAAGTGGTGATTGGCATTGTACTGCTAGCTGGAAGACACATAGTGGATCTGTATGGCGTGTGACATGGGCCCATCCTGAATTTGGGCAGGTTTTGGCTTCCTGTTCTTTTGACCGAACAGCTGCTGTATGGGAAGAAATAGTAGGAGAATCAAATGATAAACTGCGAGGACAGAGCCACTGGGTTAAAAGGACAACTCTGGTGGATAGCAGAACATCTGTTACTGATGTGAAGTTTGCTCCCAAGCACATGGGTCTTATGTTAGCAACCTGTTCCGCAGATGGTATAGTAAGAATCTATGAGGCACCAGATGTTATGAATCTCAGCCAGTGGTCTTTGCAGCATGAGATCTCATGTAAGCTAAGCTGTAGTTGTATTTCTTGGAACCCTTCAAGCTCTCGTGCTCATTCCCCCATGATCGCCGTAGGAAGTGATGACAGTAGCCCCAACGCAATGGCCAAGGTTCAGATTTTTGAATATAATGAAAACACCAG";
        CircProtein protein = matcher.translate(rna);
        for (String p : protein.getProteins()) {
            System.out.println(p);
        }

        System.out.println(Arrays.toString(protein.getBoundarys().toArray()));
        List<MatchResult> results1 = matcher.match(null, rna, "VQIFEYNENTR", 1, 1);
        if (results1 != null) {
            for (MatchResult results : results1) {
                System.out.println(results.toString());
            }
        }
        // List<MatchResult> results2 = matcher.match(null, rna, "SGFVRNRTF");
        // if (results2 != null) {
        // for (MatchResult results : results2) {
        // System.out.println(results.toString());
        // }
        // }
        // List<MatchResult> results2 = matcher.match(null, rna, "NATAASGFVRNRTF");
        // if (results2 != null) {
        // for (MatchResult results : results2) {
        // System.out.println(results.toString());
        // }
        // }
    }

    private List<MatchResult> match(final String key, final String RNA, final String protein, final int leftOffset,
            final int rightOffset) {
        // cache the RNA
//        boolean x = (RNA == null || RNA.isBlank() ? 0 : RNA.length() % 3) == 0 ? true : false;
        CircProtein circProtein = translate(RNA);
        List<String> targets = circProtein.getProteins();
        List<Integer> boundarys = circProtein.getBoundarys();
        // do match
        final int pl = protein.length();
        List<MatchResult> results = new ArrayList<>();
        int targetIndex = 0;
        int left = leftOffset + 1;
        int right = rightOffset;
        for (String target : targets) {

            Integer matchedBoundary = null;
            Integer matchedIndex = null;

            for (int fromIndex = 0; fromIndex < target.length(); fromIndex++) {
                int index = target.indexOf(protein, fromIndex);
                if (index == -1) {
                    break;
                }
                fromIndex = index + 1;
                // check boundary
                if (checkBoundary) {
                    for (Integer boundary : boundarys) {
                        if ((index <= boundary - left) && ((index + pl) >= (boundary + right))) {
                            matchedBoundary = boundary;
                            break;
                        }
                    }
                    if (matchedBoundary != null) {
                        matchedIndex = index;
                        break;
                    }
                } else {
                    matchedIndex = index;
                    break;
                }
            }

            MatchResult result = null;
            if (matchedIndex != null) {
                result = new MatchResult();
                result.setDnaKey(key);
                result.setTargetProtein(target);
                result.setTargetIndex(targetIndex);
                result.setIndex(matchedIndex);
                result.setBoundary(matchedBoundary);
                result.setProtein(protein);
                results.add(result);
            }
            targetIndex++;
        }
        return results.isEmpty() ? null : results;
    }

    @Override
    public List<MatchResult> match(final RNAProvider provider, final String protein, final int leftOffset,
            final int rightOffset) {
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
            List<MatchResult> result = match(id, sequence, protein.toUpperCase(), leftOffset, rightOffset);
            if (result != null) {
                writeLock.lock();
                try {
                    matchedSequences.addAll(result);
                } finally {
                    writeLock.unlock();
                }
            }
        });
        return matchedSequences;
    }
}
