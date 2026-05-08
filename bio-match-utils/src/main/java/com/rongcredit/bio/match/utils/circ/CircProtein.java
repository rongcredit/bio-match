package com.rongcredit.bio.match.utils.circ;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 环状 RNA 翻译结果数据对象。
 * <p>
 * 本类用于统一封装环状 RNA 在不同阅读框条件下产生的候选蛋白序列，以及用于判定是否跨越环化连接位点的边界信息，
 * 以支持后续的边界匹配分析与结果解释。
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CircProtein {

    /**
     * 环状 RNA 在不同阅读框条件下翻译得到的候选蛋白序列集合。
     */
    private List<String> proteins;

    /**
     * 环状 RNA 的边界位置集合。
     */
    private List<Integer> boundarys;
}
