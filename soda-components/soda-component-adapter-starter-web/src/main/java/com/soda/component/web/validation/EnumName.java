package com.soda.component.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注解的元素必须是指定枚举类型中某一常量的 {@link Enum#name()}。
 * <p>
 * 适用于 {@link String} 类型元素；也可标注在容器元素类型上，逐个校验元素
 * （如 {@code List<@EnumName String>}）。常与 {@code @Nullable} 组合使用：
 * null 由 {@code @Nullable} 负责，此注解仅校验非 null 值是否匹配枚举常量名。
 *
 * <pre>{@code
 * // 使用示例
 * @Nullable @EnumName(Sex.class)
 * String sex;
 * }</pre>
 *
 * @see EnumValidator
 */
@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumValidator.class)
public @interface EnumName {

    String message() default "must be one of the constants of {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 目标枚举类型，其常量名即为合法取值。
     */
    Class<? extends Enum<?>> value();
}
