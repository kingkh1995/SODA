package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

/**
 * 头像 URL DP — URI 格式校验，不可变、自校验、可比较。
 *
 * @see Type
 */
public record Avatar(@JsonValue String value) implements Type {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Avatar {
        ValidateUtils.validUri(value);
    }

}
