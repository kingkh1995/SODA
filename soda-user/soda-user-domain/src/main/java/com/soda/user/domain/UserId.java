package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;
import com.soda.component.support.types.LongId;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * 用户标识符 DP — 服务端生成（DB 自增），不可变、自校验、可比较。
 * <p>
 * 作为 {@link com.soda.component.domain.Entity Entity&lt;UserId&gt;} 的标识符。
 * 值对应 {@code system_user.id} 主键。
 *
 * @see Identifier
 */
public record UserId(@JsonValue long value) implements Identifier<Long>, Comparable<UserId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserId {
        ValidateUtils.minValue(0, false, value);
    }

    /** 从字符串解析构造。格式同 {@link ParseUtils#parseLong}。 */
    public static UserId parse(String s) {
        return new UserId(ParseUtils.parseLong(s));
    }


    @Override
    public Long identifier() {
        return value;
    }

    @Override
    public int compareTo(UserId other) {
        return Long.compare(this.value, other.value);
    }

    /** 转换为通用 {@link LongId}。 */
    public LongId toLongId() {
        return new LongId(value);
    }
}
