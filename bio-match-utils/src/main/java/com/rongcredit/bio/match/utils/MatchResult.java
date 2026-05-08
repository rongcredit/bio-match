package com.rongcredit.bio.match.utils;

import java.io.Serializable;

import lombok.Data;

/**
 * 蛋白匹配结果数据对象。
 * <p>
 * 本类用于规范描述某一目标蛋白片段在核酸序列翻译结果中的命中情况，记录来源序列、命中阅读框、
 * 命中位置及边界信息等核心分析结果，便于后续结果输出、审阅与复核。
 * </p>
 */
@Data
@SuppressWarnings("serial")
public class MatchResult implements Serializable {

    /**
     * 命中的 DNA 或 RNA 序列标识。
     */
    private String dnaKey;

    /**
     * 发生命中的目标蛋白序列全文。
     */
    private String targetProtein;

    /**
     * 命中的目标蛋白序列索引，通常对应不同阅读框翻译结果中的序号。
     */
    private Integer targetIndex;

    /**
     * 待检索或待匹配的目标蛋白片段。
     */
    private String protein;

    /**
     * 目标蛋白片段在命中序列中的起始位置。
     */
    private Integer index;

    /**
     * 命中结果对应的边界位置；若未启用边界判定或未跨越边界，则该值可为空。
     */
    private Integer boundary;
}
