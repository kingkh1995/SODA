package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Percentage 值对象")
class PercentageTest extends ComparableDomainPrimitiveContractTest<Percentage> {

    @Override
    protected Contract<Percentage> contract() {
        return new Contract<>(Percentage.class, () -> Percentage.of("12.34"), "\"12.34\"",
                "Percentage[value=12.34]", "{}", () -> Percentage.of("56.78"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法字符串创建实例")
        void should_create_when_validString() {
            var pct = Percentage.of("12.34");
            assertThat(pct.value()).isEqualTo("12.34");
        }

        @Test
        @DisplayName("from(BigDecimal) 创建实例")
        void should_create_when_validBigDecimal() {
            var pct = Percentage.from(new BigDecimal("12.34"));
            assertThat(pct.value()).isEqualTo("12.34");
        }

        @Test
        @DisplayName("from 舍入创建实例")
        void should_create_when_fromWithRounding() {
            var pct = Percentage.from(new BigDecimal("12.345"), RoundingMode.HALF_UP);
            assertThat(pct.value()).isEqualTo("12.35");
        }

        @Test
        @DisplayName("整数字面量创建")
        void should_create_when_integerValue() {
            var pct = Percentage.of("50");
            assertThat(pct.value()).isEqualTo("50.00");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> Percentage.of((String) null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空字符串拒绝")
        void should_throw_when_valueIsEmpty() {
            assertThatThrownBy(() -> Percentage.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("非法字符串拒绝")
        void should_throw_when_valueIsNotNumeric() {
            assertThatThrownBy(() -> Percentage.of("abc"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超精度拒绝（scale > 2）")
        void should_throw_when_scaleExceedsMax() {
            assertThatThrownBy(() -> Percentage.of("12.345"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("负数拒绝")
        void should_throw_when_valueIsNegative() {
            assertThatThrownBy(() -> Percentage.of("-1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超过 100 拒绝")
        void should_throw_when_valueExceeds100() {
            assertThatThrownBy(() -> Percentage.of("100.01"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("归一化")
    class Normalization {
        @Test
        @DisplayName("等值归一化")
        void should_beNormalized_when_trailingZeros() {
            assertThat(Percentage.of("12.00")).isEqualTo(Percentage.of("12"));
        }

        @Test
        @DisplayName("不同精度归一化")
        void should_beNormalized_when_differentPrecision() {
            assertThat(Percentage.of("12.10")).isEqualTo(Percentage.of("12.1"));
        }

        @Test
        @DisplayName("下界 0 归一化为 \"0.00\"")
        void should_normalizeZero_when_lowerBound() {
            assertThat(Percentage.of("0").value()).isEqualTo("0.00");
        }

        @Test
        @DisplayName("上界 100 归一化为 \"100.00\"")
        void should_normalizeHundred_when_upperBound() {
            assertThat(Percentage.of("100").value()).isEqualTo("100.00");
        }
    }

    @Nested
    @DisplayName("转换")
    class Conversion {
        @Test
        @DisplayName("toFraction 正确转换")
        void should_convertToFraction_when_validValue() {
            var pct = Percentage.of("12.34");
            assertThat(pct.toFraction()).isEqualByComparingTo("0.1234");
        }

        @Test
        @DisplayName("toFraction 0% 返回 0")
        void should_toFraction_beZero_when_zero() {
            assertThat(Percentage.of("0").toFraction()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("toFraction 100% 返回 1")
        void should_toFraction_beOne_when_max() {
            assertThat(Percentage.of("100").toFraction()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("toDisplayString 0%")
        void should_toDisplayString_when_zero() {
            assertThat(Percentage.of("0").toDisplayString()).isEqualTo("0.00%");
        }

        @Test
        @DisplayName("toDisplayString 100%")
        void should_toDisplayString_when_max() {
            assertThat(Percentage.of("100").toDisplayString()).isEqualTo("100.00%");
        }
    }
}
