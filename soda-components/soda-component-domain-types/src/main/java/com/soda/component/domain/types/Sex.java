package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 性别枚举。取值：M（male）、F（female）。
 * <p>
 * 组件层通用共享枚举——供 web 层校验注解（如 {@code @EnumName}）按短名引用而不越模块边界。
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Sex implements EnumType {

    M("male"),
    F("female");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Sex of(String name) {
        return ParseUtils.parseEnum(Sex.class, name);
    }
}
