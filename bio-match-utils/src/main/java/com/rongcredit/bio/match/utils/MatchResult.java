package com.rongcredit.bio.match.utils;

import java.io.Serializable;

import lombok.Data;

@Data
@SuppressWarnings("serial")
public class MatchResult implements Serializable {

    private String dnaKey;

    private String targetProtein;
    private Integer targetIndex;

    private String protein;
    private Integer index;
    private Integer boundary;
}
