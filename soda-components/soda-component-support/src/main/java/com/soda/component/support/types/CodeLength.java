package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.TypeConfig;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * 验证码长度 DP — 正整数值，不可变、自校验、可比较。
 * <p>
 * 校验规则：取值范围 [1, 100]。
 *
 * @see Type
 */
public record CodeLength(@JsonValue int value) implements Type, Comparable<CodeLength> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MIN = Math.max(1, TypeConfig.PROVIDER.codeLengthMin());
    private static final int MAX = Math.max(Math.max(100, MIN), TypeConfig.PROVIDER.codeLengthMax());

    public CodeLength {
        ValidateUtils.range(MIN, MAX, value);
    }

    /** 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。 */
    public static CodeLength parse(String s) {
        return new CodeLength(ParseUtils.parseInt(s));
    }


    @Override
    public int compareTo(CodeLength other) {
        return Integer.compare(this.value, other.value);
    }
}
