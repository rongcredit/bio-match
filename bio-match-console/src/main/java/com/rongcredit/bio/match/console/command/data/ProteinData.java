package com.rongcredit.bio.match.console.command.data;

import java.io.Serializable;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

/**
 * 蛋白输入记录映射对象。
 * <p>
 * 本类用于映射蛋白输入 Excel 文件中的关键字段，主要包括蛋白名称及对应序列内容，
 * 以支持后续的标准化处理与批量匹配分析。
 * </p>
 */
@Data
@SuppressWarnings("serial")
public class ProteinData implements Serializable {
    /**
     * Excel 第 3 列中的蛋白序列原文。
     */
    @ExcelProperty(index = 2)
    private String protein;

    /**
     * Excel 第 8 列中的蛋白名称。
     */
    @ExcelProperty(index = 7)
    private String proteinName;
}
