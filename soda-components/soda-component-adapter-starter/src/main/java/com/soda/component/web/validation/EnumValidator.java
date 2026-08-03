package com.soda.component.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link EnumName} 注解的校验器。
 * <p>
 * 收集目标枚举的所有 {@link Enum#name()} 值，校验输入字符串是否匹配其一。
 * null 值视为有效（由 {@code @Nullable}/@NotNull 负责 null 语义）。
 */
public class EnumValidator implements ConstraintValidator<EnumName, String> {

    private Set<String> validNames;

    @Override
    public void initialize(EnumName constraintAnnotation) {
        validNames = Stream.of(constraintAnnotation.value().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 交由 @Nullable/@NotNull 处理
        return value == null || validNames.contains(value);
    }
}
