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
 * {@link MobileNumber} 约束的行为测试：组合 {@link Pattern} 的违规归属、
 * null 语义与容器元素校验。
 */
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
    void acceptsValidNumber() {
        assertTrue(validator.validate(new Bean("13812345678", List.of())).isEmpty());
    }

    @Test
    void rejectsInvalidNumberWithOwnMessage() {
        Set<ConstraintViolation<Bean>> violations = validator.validate(new Bean("12345", List.of()));
        assertEquals(1, violations.size());
        ConstraintViolation<Bean> violation = violations.iterator().next();
        assertEquals("must be a valid mobile number", violation.getMessage());
        assertEquals(MobileNumber.class, violation.getConstraintDescriptor().getAnnotation().annotationType());
        assertEquals("mobile", violation.getPropertyPath().toString());
    }

    @Test
    void treatsNullAsValid() {
        assertTrue(validator.validate(new Bean(null, List.of())).isEmpty());
    }

    @Test
    void validatesEachContainerElement() {
        Set<ConstraintViolation<Bean>> violations =
                validator.validate(new Bean("13812345678", List.of("13812345678", "abc")));
        assertEquals(1, violations.size());
        assertEquals("mobiles[1].<list element>",
                violations.iterator().next().getPropertyPath().toString());
    }

    private record Bean(
            @MobileNumber String mobile,
            List<@MobileNumber String> mobiles) {
    }
}
