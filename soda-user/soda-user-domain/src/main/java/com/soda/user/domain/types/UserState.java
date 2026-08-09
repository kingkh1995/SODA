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
public enum UserState implements EnumType {

    E("enabled"),
    D("disabled"),
    R("deregistered");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserState of(String name) {
        return ParseUtils.parseEnum(UserState.class, name);
    }
}
