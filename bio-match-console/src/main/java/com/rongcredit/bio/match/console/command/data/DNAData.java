package com.rongcredit.bio.match.console.command.data;

import java.io.Serializable;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
@SuppressWarnings("serial")
public class DNAData implements Serializable {
    @ExcelProperty(index = 0)
    private String dNA;
}
