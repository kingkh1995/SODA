package com.soda.user.domain;

import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * 头像 URL DP — URI 格式校验，不可变、自校验、可比较。
 *
 * @see Type
 */
public record Avatar(String value) implements Type {

    public Avatar {
        ValidateUtils.validUrl(ParseUtils.parseUri(value));
    }

    @JsonValue
    public String value() { return this.value; }

}
