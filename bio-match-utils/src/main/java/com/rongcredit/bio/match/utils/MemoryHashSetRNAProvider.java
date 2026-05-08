package com.rongcredit.bio.match.utils;

import java.util.LinkedHashMap;

/**
 * 基于内存的核酸序列提供实现。
 * <p>
 * 本实现直接继承 {@link LinkedHashMap}，适用于将输入文件中的序列数据一次性装载至内存后，
 * 供批量翻译、检索与匹配任务重复访问。使用有序映射结构也有助于在分析输出时保留原始读取顺序。
 * </p>
 */
@SuppressWarnings("serial")
public class MemoryHashSetRNAProvider extends LinkedHashMap<String,String> implements RNAProvider {

}
