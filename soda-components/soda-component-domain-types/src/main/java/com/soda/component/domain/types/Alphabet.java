package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ValidateUtils;
import org.springframework.util.Assert;

/**
 * 字符集 DP — 包装任意字符集字符串（开集，非枚举），供随机字符串生成限定输出字符池。
 * <p>
 * 不变量：非空、字符唯一（字符集语义是集合，重复字符 = 隐式加权，几乎必是 bug）、
 * size ≥ 2（熵下限，单字符字符集恒等输出）。提供严格索引 {@link #charAt(int)}
 * （0 ≤ index &lt; size，越界 IAE）——DP 只做纯索引映射，随机源由生成器负责
 * （{@code SecureRandom.nextInt(size)}，拒绝采样无偏，见 ADR-0018）。
 * <p>
 * 常用字符集提供静态常量：{@link #DIGITS}、{@link #UNAMBIGUOUS_ALPHANUMERIC}。
 * 字符池映射归基础设施层，领域层只持有字符集字符串本身。
 *
 * @see Type
 * @see com.soda.component.domain.gateway.RandomStringGenerator
 */
public record Alphabet(String value) implements Type {

    /**
     * 纯数字字符集：0-9。
     */
    public static final Alphabet DIGITS = new Alphabet("0123456789");

    /**
     * 数字 + 字母字符集（去除易混淆字符、仅大写）：数字剔除 0、1，字母剔除 I、O
     * （形近组：0/O、1/I/l——大写 L 带脚可区分，保留；无小写字母）。
     */
    public static final Alphabet UNAMBIGUOUS_ALPHANUMERIC = new Alphabet(
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZ");

    public Alphabet {
        ValidateUtils.hasText(value);
        ValidateUtils.noDuplicateChars(value);
        ValidateUtils.minValue(value.length(), 2, true);
    }

    /**
     * 字符集大小。
     */
    public int size() {
        return value.length();
    }

    /**
     * 严格索引取字符 — 0 ≤ {@code index} < {@link #size()}，越界抛 IAE（业务参数校验，
     * ADR-0015：索引值不合法即失败，暴露调用方取模/越界 bug）。
     * <p>
     * 生成器侧应使用无偏的 {@code SecureRandom.nextInt(size())} 产生索引后调用本方法。
     */
    public char charAt(int index) {
        Assert.isTrue(index >= 0 && index < value.length(), "index out of bounds: " + index);
        return value.charAt(index);
    }

    @JsonValue
    public String value() {
        return value;
    }
}
