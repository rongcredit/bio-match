package com.rongcredit.bio.match.utils;

import java.util.Map;

/**
 * 核酸序列提供接口。
 * <p>
 * 本接口以 {@link Map} 结构抽象核酸序列数据源，其中键通常表示序列编号或名称，值表示对应的核酸序列正文。
 * 该抽象设计便于匹配算法在不依赖具体存储介质的前提下，统一访问内存数据、缓存数据或其他外部数据源。
 * </p>
 */
public interface RNAProvider extends Map<String,String> {

}
