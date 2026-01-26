package com.rongcredit.bio.match.utils;

import java.util.List;

/**
 * Define a generic matcher interface for protein
 */
public interface ProteinMatcher {

    /**
     * Match a protein from the specified RNA sequence provider.
     * 
     * @param provider    RNA sequence provider
     * @param protein     protein to match
     * @param leftOffset  left offset
     * @param rightOffset right offset
     * @return {@code List<MatchResult>} a list of RNA sequence the fulfill the
     *         match
     *         rule
     */
    List<MatchResult> match(RNAProvider provider, String protein, final int leftOffset, final int rightOffset);
}
