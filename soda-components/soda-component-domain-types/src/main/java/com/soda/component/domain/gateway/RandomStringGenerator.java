package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import com.soda.component.domain.types.RandomString;

/**
 * 随机字符串生成器 Gateway — 按指定长度与字符集生成随机字符串。
 * <p>
 * 字符集由调用方以 {@link Alphabet} 指定（领域拥有，见 ADR-0018），
 * 随机源由实现层决定。通用能力，不绑定任何业务场景。
 *
 * @see Gateway
 */
public interface RandomStringGenerator extends Gateway {

    /**
     * 生成长度为 {@code length}、字符取自 {@code alphabet} 的随机字符串。
     *
     * @param length   长度（正整数）
     * @param alphabet 字符集（输出字符池）
     * @return 随机字符串
     */
    RandomString generate(PositiveInt length, Alphabet alphabet);
}
