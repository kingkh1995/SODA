package com.soda.component.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ValidateUtils} 失败语义契约：所有校验失败统一抛出 {@link IllegalArgumentException}，
 * 错误消息为工具内联的固定默认文案，调用方无需传入消息参数。
 */
@DisplayName("ValidateUtils 校验工具")
class ValidateUtilsTest {

    @Nested
    @DisplayName("非 null 校验")
    class NotNull {

        @Test
        @DisplayName("非 null 值通过")
        void should_pass_when_valueNonNull() {
            ValidateUtils.notNull("value");
        }

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> ValidateUtils.notNull(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("非 blank 校验")
    class HasText {

        @Test
        @DisplayName("合法文本通过")
        void should_pass_when_textValid() {
            ValidateUtils.hasText("valid");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("null / 空串 / 空白拒绝")
        void should_throw_when_blank(String value) {
            assertThatThrownBy(() -> ValidateUtils.hasText(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("最小值校验")
    class MinValue {

        @Test
        @DisplayName("含边界：高于 min 通过")
        void should_pass_when_aboveMinInclusive() {
            ValidateUtils.minValue(10, 5, true);
        }

        @Test
        @DisplayName("排除边界：高于 min 通过")
        void should_pass_when_aboveMinExclusive() {
            ValidateUtils.minValue(6, 5, false);
        }

        @Test
        @DisplayName("含边界：等于 min 通过")
        void should_pass_when_equalToMinInclusive() {
            ValidateUtils.minValue(5, 5, true);
        }

        @Test
        @DisplayName("含边界：低于 min 拒绝")
        void should_throw_when_belowMinInclusive() {
            assertThatThrownBy(() -> ValidateUtils.minValue(3, 5, true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("排除边界：等于 min 拒绝")
        void should_throw_when_equalToMinExclusive() {
            assertThatThrownBy(() -> ValidateUtils.minValue(5, 5, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("排除边界：低于 min 拒绝")
        void should_throw_when_belowMinExclusive() {
            assertThatThrownBy(() -> ValidateUtils.minValue(4, 5, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("正则格式校验")
    class Matches {

        @Test
        @DisplayName("匹配格式的值通过")
        void should_pass_when_formatValid() {
            var digit = Pattern.compile("\\d+");
            ValidateUtils.matches("123", digit);
        }

        @Test
        @DisplayName("不匹配格式的值拒绝")
        void should_throw_when_formatInvalid() {
            var digit = Pattern.compile("\\d+");
            assertThatThrownBy(() -> ValidateUtils.matches("abc", digit))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("小数位数上限校验")
    class MaxScale {

        @Test
        @DisplayName("scale 小于上限通过")
        void should_pass_when_scaleWithinLimit() {
            ValidateUtils.maxScale(new BigDecimal("10.50"), 2);
        }

        @Test
        @DisplayName("scale 等于上限通过")
        void should_pass_when_scaleAtLimit() {
            ValidateUtils.maxScale(new BigDecimal("10.55"), 2);
        }

        @Test
        @DisplayName("scale 超过上限拒绝")
        void should_throw_when_scaleExceedsLimit() {
            assertThatThrownBy(() -> ValidateUtils.maxScale(new BigDecimal("10.555"), 2))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("字符串最大长度校验")
    class MaxLength {

        @Test
        @DisplayName("长度小于上限通过")
        void should_pass_when_withinLimit() {
            ValidateUtils.maxLength("hello", 10);
        }

        @Test
        @DisplayName("长度等于上限通过")
        void should_pass_when_lengthAtLimit() {
            ValidateUtils.maxLength("hello", 5);
        }

        @Test
        @DisplayName("长度超过上限拒绝")
        void should_throw_when_lengthExceedsLimit() {
            assertThatThrownBy(() -> ValidateUtils.maxLength("hello", 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null 值拒绝（先过非 blank 校验）")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> ValidateUtils.maxLength(null, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("区间校验")
    class Range {

        @Test
        @DisplayName("区间内的值通过")
        void should_pass_when_withinRange() {
            ValidateUtils.range(5, 1, 10);
        }

        @Test
        @DisplayName("下边界值通过")
        void should_pass_when_atMinBoundary() {
            ValidateUtils.range(1, 1, 10);
        }

        @Test
        @DisplayName("上边界值通过")
        void should_pass_when_atMaxBoundary() {
            ValidateUtils.range(10, 1, 10);
        }

        @Test
        @DisplayName("低于下界拒绝")
        void should_throw_when_belowMin() {
            assertThatThrownBy(() -> ValidateUtils.range(0, 1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("高于上界拒绝")
        void should_throw_when_aboveMax() {
            assertThatThrownBy(() -> ValidateUtils.range(11, 1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("前缀校验")
    class HasPrefix {

        @Test
        @DisplayName("前缀匹配通过")
        void should_pass_when_prefixMatches() {
            ValidateUtils.hasPrefix("P:42", "P:");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"Q:42"})
        @DisplayName("null / 空串 / 前缀不符拒绝")
        void should_throw_when_prefixMismatch(String value) {
            assertThatThrownBy(() -> ValidateUtils.hasPrefix(value, "P:"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等校验")
    class Equals {

        @Test
        @DisplayName("相等值通过")
        void should_pass_when_valuesEqual() {
            ValidateUtils.equals("a", "a");
        }

        @Test
        @DisplayName("双 null 视为相等（Objects.equals 契约）")
        void should_pass_when_bothNull() {
            ValidateUtils.equals(null, null);
        }

        @Test
        @DisplayName("不等值拒绝")
        void should_throw_when_notEqual() {
            assertThatThrownBy(() -> ValidateUtils.equals("a", "b"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("实际值为 null 拒绝")
        void should_throw_when_actualIsNull() {
            assertThatThrownBy(() -> ValidateUtils.equals(null, "a"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("期望值为 null 拒绝")
        void should_throw_when_expectedIsNull() {
            assertThatThrownBy(() -> ValidateUtils.equals("a", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("错误消息引用操作数原值")
        void should_quoteOperands_when_notEqual() {
            assertThatThrownBy(() -> ValidateUtils.equals("a", "b"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("must equal 'b', got: 'a'");
        }
    }
}
