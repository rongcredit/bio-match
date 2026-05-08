package com.rongcredit.bio.match.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 核酸序列翻译结果数据对象。
 * <p>
 * 本对象用于描述单次核酸序列翻译操作的输出结果，既保存已成功转换得到的蛋白序列，
 * 也保留末尾不足一个完整密码子的剩余片段，以便后续分析或进一步处理。
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TranslateResult {

	/**
	 * 已完成翻译的蛋白序列结果。
	 */
	private String protein;

	/**
	 * 输入序列尾部未形成完整密码子的剩余片段。
	 */
	private String remainSequence;
}
