package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WanYuan 值对象")
class WanYuanTest extends ComparableDomainPrimitiveContractTest<WanYuan> {

    @Override
    protected Contract<WanYuan> contract() {
        return new Contract<>(WanYuan.class, () -> WanYuan.of("1.5"), "\"1.50\"",
                "WanYuan[value=1.50]", "{}", () -> WanYuan.of("9.99"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法字符串创建实例")
        void should_create_when_validValue() {
            var amount = WanYuan.of("15000");
            assertThat(amount.value()).isEqualTo("15000.00");
        }

        @Test
        @DisplayName("from(BigDecimal) 创建实例")
        void should_create_when_validBigDecimal() {
            var amount = WanYuan.from(new BigDecimal("15000"));
            assertThat(amount.value()).isEqualTo("15000.00");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> WanYuan.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空字符串拒绝")
        void should_throw_when_valueIsEmpty() {
            assertThatThrownBy(() -> WanYuan.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("非法字符串拒绝")
        void should_throw_when_valueIsNotNumeric() {
            assertThatThrownBy(() -> WanYuan.of("abc"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超精度拒绝（maxScale 2）")
        void should_throw_when_scaleExceedsMax() {
            assertThatThrownBy(() -> WanYuan.of("12.345"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("归一化幂等")
    class Normalization {
        @Test
        @DisplayName("精确等值归一化")
        void should_beNormalized_when_trailingZeros() {
            assertThat(WanYuan.of("15000.00")).isEqualTo(WanYuan.of("15000"));
        }

        @Test
        @DisplayName("不同精度归一化")
        void should_beNormalized_when_differentPrecision() {
            assertThat(WanYuan.of("15000.10")).isEqualTo(WanYuan.of("15000.1"));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "15000, 15000.00",
                "15000.0, 15000.00",
                "15000.00, 15000.00",
                "0, 0.00",
                "-5.0, -5.00"
        })
        @DisplayName("同构值归一化幂等：规范串唯一，重复解析等值")
        void should_beIdempotent_when_isomorphicValues(String raw, String canonical) {
            var normalized = WanYuan.of(raw);
            assertThat(normalized.value()).isEqualTo(canonical);
            assertThat(WanYuan.of(normalized.value())).isEqualTo(normalized);
        }

    }

    @Nested
    @DisplayName("转换")
    class Conversion {
        @Test
        @DisplayName("fromYuan 元转万元创建归一化实例")
        void should_createNormalized_when_fromYuan() {
            var amount = WanYuan.fromYuan(new BigDecimal("15000"));
            assertThat(amount.value()).isEqualTo("1.50");
        }

        @Test
        @DisplayName("fromYuan 显式 HALF_UP 舍入")
        void should_roundHalfUp_when_fromYuanWithRoundingMode() {
            assertThat(WanYuan.fromYuan(new BigDecimal("12350"), RoundingMode.HALF_UP).value())
                    .isEqualTo("1.24");
        }

        @Test
        @DisplayName("fromYuan 显式 DOWN 舍入")
        void should_roundDown_when_fromYuanWithRoundingMode() {
            assertThat(WanYuan.fromYuan(new BigDecimal("12350"), RoundingMode.DOWN).value())
                    .isEqualTo("1.23");
        }

        @Test
        @DisplayName("toYuan 万元转元")
        void should_convertToYuan_when_validValue() {
            assertThat(WanYuan.of("1.50").toYuan()).isEqualByComparingTo("15000");
            assertThat(WanYuan.of("-1.50").toYuan()).isEqualByComparingTo("-15000");
        }

        @Test
        @DisplayName("toDisplayString 展示文本")
        void should_haveCorrectDisplayString_when_validValue() {
            assertThat(WanYuan.of("1.5").toDisplayString()).isEqualTo("1.50万元");
        }
    }
}
