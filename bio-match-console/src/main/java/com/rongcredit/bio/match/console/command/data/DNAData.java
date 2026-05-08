package com.rongcredit.bio.match.console.command.data;

import java.io.Serializable;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

/**
 * DNA 输入记录映射对象。
 * <p>
 * 本类用于接收 DNA 输入 Excel 文件中的单行记录。结合后续解析逻辑，单元格内容既可能表示
 * 以 `&gt;` 开头的序列标识信息，也可能表示属于该标识的数据序列片段。
 * </p>
 */
@Data
@SuppressWarnings("serial")
public class DNAData implements Serializable {
    /**
     * Excel 第 1 列中的原始 DNA 文本内容。
     */
    @ExcelProperty(index = 0)
    private String dNA;
}
