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
public enum Sex implements EnumType {

    M("Male"),
    F("Female");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Sex of(String name) {
        return ParseUtils.parseEnum(Sex.class, name);
    }
}
