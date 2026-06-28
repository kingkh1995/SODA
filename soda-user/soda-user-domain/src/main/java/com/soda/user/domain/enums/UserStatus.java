package com.soda.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.EnumType;
import com.soda.component.support.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum UserStatus implements EnumType {

    E("Enabled"),
    D("Disabled");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserStatus of(String name) {
        return ParseUtils.parseEnum(UserStatus.class, name);
    }

    @JsonValue
    private String jsonValue() {
        return this.name();
    }
}
