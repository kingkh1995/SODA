package com.soda.component.web.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EnumName} 约束的行为测试：单值校验、null 语义与容器元素校验。
 */
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
    void acceptsEnumConstantName() {
        assertTrue(validator.validate(new Bean("RED", List.of())).isEmpty());
    }

    @Test
    void rejectsUnknownName() {
        Set<ConstraintViolation<Bean>> violations = validator.validate(new Bean("BLUE", List.of()));
        assertEquals(1, violations.size());
        ConstraintViolation<Bean> violation = violations.iterator().next();
        assertTrue(violation.getMessage().startsWith("must be one of the constants of"));
        assertEquals("color", violation.getPropertyPath().toString());
    }

    @Test
    void treatsNullAsValid() {
        assertTrue(validator.validate(new Bean(null, List.of())).isEmpty());
    }

    @Test
    void validatesEachContainerElement() {
        Set<ConstraintViolation<Bean>> violations =
                validator.validate(new Bean("RED", List.of("RED", "BLUE")));
        assertEquals(1, violations.size());
        assertEquals("colors[1].<list element>",
                violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void emptyContainerIsValid() {
        assertTrue(validator.validate(new Bean("RED", List.of())).isEmpty());
    }

    private enum Color {
        RED, GREEN
    }

    private record Bean(
            @EnumName(Color.class) String color,
            List<@EnumName(Color.class) String> colors) {
    }
}
