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
 * {@link EnumName} 约束的行为测试：单值校验、null 语义与容器元素校验。
 */
@DisplayName("EnumName 枚举常量名约束")
class EnumNameTest {

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
    @DisplayName("枚举常量名通过校验")
    void should_accept_when_valueMatchesEnumConstantName() {
        assertThat(validator.validate(new Bean("RED", List.of()))).isEmpty();
    }

    @Test
    @DisplayName("null 视为合法，由其他约束负责")
    void should_treatNullAsValid_when_colorIsNull() {
        assertThat(validator.validate(new Bean(null, List.of()))).isEmpty();
    }

    @Test
    @DisplayName("空容器视为合法")
    void should_accept_when_containerIsEmpty() {
        assertThat(validator.validate(new Bean("RED", List.of()))).isEmpty();
    }

    @Test
    @DisplayName("非枚举常量名拒绝")
    void should_reject_when_valueNotAnEnumConstantName() {
        var violations = validator.validate(new Bean("BLUE", List.of()));
        assertThat(violations).hasSize(1);
        var violation = violations.iterator().next();
        assertThat(violation.getMessage()).startsWith("must be one of the constants of");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("color");
    }

    @Test
    @DisplayName("容器元素逐个校验并定位违规下标")
    void should_validateEachContainerElement_when_containerContainsUnknownName() {
        var violations = validator.validate(new Bean("RED", List.of("RED", "BLUE")));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("colors[1].<list element>");
    }

    private enum Color {
        RED, GREEN
    }

    private record Bean(
            @EnumName(Color.class) String color,
            List<@EnumName(Color.class) String> colors) {
    }
}
