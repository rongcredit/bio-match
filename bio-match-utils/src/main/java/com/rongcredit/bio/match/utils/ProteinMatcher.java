package com.rongcredit.bio.match.utils;

import java.util.List;

/**
 * 蛋白匹配算法统一接口。
 * <p>
 * 本接口用于规范“在一组核酸序列中检索指定蛋白片段”的基础能力，以便不同场景下的匹配算法实现
 * 在统一约束下进行扩展，例如线性序列匹配、环状 RNA 跨边界匹配等。
 * </p>
 */
public interface ProteinMatcher {

    /**
     * 在给定核酸序列集合中执行蛋白片段匹配。
     *
     * @param provider 核酸序列提供者，键为序列标识，值为序列内容
     * @param protein 待匹配的目标蛋白片段
     * @param leftOffset 边界左侧偏移量，用于约束跨边界命中的判定范围
     * @param rightOffset 边界右侧偏移量，用于约束跨边界命中的判定范围
     * @return 匹配结果集合；当无匹配结果时，返回值由具体实现类约定
     */
    List<MatchResult> match(RNAProvider provider, String protein, final int leftOffset, final int rightOffset);
}
