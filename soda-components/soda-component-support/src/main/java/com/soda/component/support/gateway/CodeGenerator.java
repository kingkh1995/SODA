package com.soda.component.support.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.support.types.CodeLength;
import com.soda.component.support.types.CodeValue;

/**
 * 验证码/令牌生成器 Gateway — 生成指定长度的字母数字码。
 * <p>
 * 通用契约，实现层提供具体的生成算法。
 *
 * @see Gateway
 */
public interface CodeGenerator extends Gateway {

    /** 生成长度为 {@code length} 的字母数字码。 */
    CodeValue generate(CodeLength length);
}
