package com.rongcredit.bio.match.console.command.data;

import java.io.Serializable;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
@SuppressWarnings("serial")
public class ProteinData implements Serializable {
    @ExcelProperty(index = 2)
    private String protein;
    @ExcelProperty(index = 7)
    private String proteinName;
}
