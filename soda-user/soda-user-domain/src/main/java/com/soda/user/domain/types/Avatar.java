package com.soda.user.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;


/**
 * 头像 URL DP — URI 格式校验，不可变、自校验。
 *
 * @see StringLiteralType
 */
public record Avatar(String value) implements StringLiteralType {

    public Avatar {
        ValidateUtils.validUrl(ParseUtils.parseUri(value));
    }

}
