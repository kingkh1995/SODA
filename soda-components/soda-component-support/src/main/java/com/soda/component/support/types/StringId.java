package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * {@code String} 类型标识符 — 通用 DP，适用于业务编码、UUID、订单号等场景。
 * <p>
 * 遵循 DP 规范：不可变、自校验、可比较。
 * <p>
 * 参考 kk-ddd 的 {@code StringId} 设计。
 *
 * @see Identifier
 */
public record StringId(@JsonValue String value) implements Identifier<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    public StringId {
        ValidateUtils.nonBlank(value);
        value = value.trim();
    }

    /** 从不可靠输入构造，null 或 blank 时抛出 {@link IllegalArgumentException}。 */
    public static StringId valueOf(Object value) {
        return new StringId(ParseUtils.parseString(value));
    }

    @Override
    public String identifier() {
        return value;
    }
}
