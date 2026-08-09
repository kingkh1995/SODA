package com.soda.component.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验中国大陆手机号格式。
 * <p>
 * 规则：11 位数字，以 1 开头，第二位为 3-9（与领域层 {@code Mobile} DP 一致）。
 * 通过组合 {@link Pattern} 实现，无需自定义校验器；配合
 * {@link ReportAsSingleViolation}，违规时统一报告为此注解并输出本注解的 message。
 *
 * <pre>{@code
 * // 使用示例
 * @NotBlank @Mobile
 * String mobile;
 * }</pre>
 */
@Pattern(regexp = "^1[3-9]\\d{9}$")
@ReportAsSingleViolation
@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface Mobile {

    String message() default "must be a valid mobile number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
