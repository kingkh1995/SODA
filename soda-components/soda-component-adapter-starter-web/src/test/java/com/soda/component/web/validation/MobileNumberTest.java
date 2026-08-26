package com.soda.component.web.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MobileNumber} 约束的行为测试：组合 {@code Pattern} 的违规归属、
 * null 语义与容器元素校验。
 */
@DisplayName("MobileNumber 手机号格式约束")
class MobileNumberTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("合法手机号通过校验")
    void should_accept_when_mobileNumberIsValid() {
        assertThat(validator.validate(new Bean("13812345678", List.of()))).isEmpty();
    }

    @Test
    @DisplayName("null 视为合法，由其他约束负责")
    void should_treatNullAsValid_when_mobileIsNull() {
        assertThat(validator.validate(new Bean(null, List.of()))).isEmpty();
    }

    @Test
    @DisplayName("非法手机号拒绝且违规归属本约束")
    void should_reject_when_mobileIsInvalid() {
        var violations = validator.validate(new Bean("12345", List.of()));
        assertThat(violations).hasSize(1);
        var violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("must be a valid mobile number");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(MobileNumber.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("mobile");
    }

    @Test
    @DisplayName("容器元素逐个校验并定位违规下标")
    void should_validateEachContainerElement_when_containerContainsInvalidValue() {
        var violations = validator.validate(new Bean("13812345678", List.of("13812345678", "abc")));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("mobiles[1].<list element>");
    }

    private record Bean(
            @MobileNumber String mobile,
            List<@MobileNumber String> mobiles) {
    }
}
