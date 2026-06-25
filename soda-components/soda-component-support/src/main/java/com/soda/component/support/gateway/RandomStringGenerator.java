package com.soda.component.support.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.support.types.PositiveInt;
import com.soda.component.support.types.RandomString;

/**
 * 随机字符串生成器 Gateway — 生成指定长度的随机字符串。
 * <p>
 * 字符集和随机源由实现层决定（数字、字母数字、Base64 URL-safe 等），
 * 领域层只关心长度。
 *
 * @see Gateway
 */
public interface RandomStringGenerator extends Gateway {

    /**
     * 生成长度为 {@code length} 的随机字符串。
     *
     * @param length 长度（正整数）
     * @return 随机字符串
     */
    RandomString generate(PositiveInt length);
}
