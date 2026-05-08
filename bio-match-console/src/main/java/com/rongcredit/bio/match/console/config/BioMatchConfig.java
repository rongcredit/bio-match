package com.rongcredit.bio.match.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * `bio-match` 控制台模块运行参数配置类。
 * <p>
 * 本类用于承接配置文件中 `bio-match` 前缀下的全部业务参数，并作为控制台命令执行时的默认参数来源。
 * 当外部启动参数未提供相应选项时，系统将优先采用此处定义的配置值，以保证批处理任务具备稳定、可复现的执行行为。
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "bio-match")
@Component
public class BioMatchConfig {

    /**
     * 是否启用环化边界校验。
     */
    private boolean boundary = false;

    /**
     * 边界左侧允许的匹配偏移量。
     */
    private Integer left = 1;

    /**
     * 边界右侧允许的匹配偏移量。
     */
    private Integer right = 1;

    /**
     * 蛋白序列归一化过程中是否保留方括号内的字符内容。
     */
    private boolean include = false;

    /**
     * DNA 序列输入文件路径。
     */
    private String dnaFile;

    /**
     * 蛋白序列输入文件路径。
     */
    private String proteinFile;

    /**
     * 匹配结果输出文件路径。
     */
    private String outputFile = "./output.txt";
}
