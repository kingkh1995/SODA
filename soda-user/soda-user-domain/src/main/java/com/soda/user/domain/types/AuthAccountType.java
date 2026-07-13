package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum AuthAccountType implements EnumType {

    P("Password"),
    S("Sms"),
    E("Email"),
    O("OAuth");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuthAccountType of(String name) {
        return ParseUtils.parseEnum(AuthAccountType.class, name);
    }
}
