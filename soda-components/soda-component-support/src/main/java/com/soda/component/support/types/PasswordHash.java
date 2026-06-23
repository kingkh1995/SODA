package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;
import java.util.regex.Pattern;

/**
 * 密码哈希 DP — BCrypt 哈希值，不可变、自校验、可比较。
 * <p>
 * 校验规则：非 blank，格式符合 BCrypt（{@code $2[aby]$ rounds$salt+hash}）。
 *
 * @see Type
 */
public record PasswordHash(@JsonValue String value) implements Type, Comparable<PasswordHash> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    public PasswordHash {
        ValidateUtils.nonBlank(value);
        ValidateUtils.matches(BCRYPT_PATTERN, value);
    }


    @Override
    public int compareTo(PasswordHash other) {
        return this.value.compareTo(other.value);
    }
}
