package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Fen 值对象")
class FenTest extends ComparableDomainPrimitiveContractTest<Fen> {

    @Override
    protected Contract<Fen> contract() {
        return new Contract<>(Fen.class, () -> new Fen(1500), "1500",
                "Fen[value=1500]", "\"not-a-number\"", () -> new Fen(1501));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @ParameterizedTest(name = "{0}")
        @ValueSource(ints = {1500, -100, Integer.MAX_VALUE, Integer.MIN_VALUE})
        @DisplayName("int 值创建实例（正 / 负 / int 边界）")
        void should_create_when_intValue(int value) {
            assertThat(new Fen(value).value()).isEqualTo(value);
        }

        @Test
        @DisplayName("ZERO 常量为 0 分")
        void should_haveZeroConstant() {
            assertThat(Fen.ZERO.value()).isZero();
            assertThat(Fen.ZERO).isEqualTo(new Fen(0));
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parseValidString() {
            assertThat(Fen.parse("1500")).isEqualTo(new Fen(1500));
        }

        @Test
        @DisplayName("fromYuan 转换元为分")
        void should_create_when_fromYuan() {
            assertThat(Fen.fromYuan(new BigDecimal("15.5"))).isEqualTo(new Fen(1550));
        }

        @Test
        @DisplayName("fromYuan 指定舍入模式")
        void should_create_when_fromYuanWithRounding() {
            assertThat(Fen.fromYuan(new BigDecimal("1.005"), RoundingMode.HALF_UP))
                    .isEqualTo(new Fen(101));
            assertThat(Fen.fromYuan(new BigDecimal("1.004"), RoundingMode.HALF_UP))
                    .isEqualTo(new Fen(100));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> Fen.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> Fen.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 空字符串拒绝")
        void should_throw_when_parseEmptyString() {
            assertThatThrownBy(() -> Fen.parse(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromYuan(null) 拒绝")
        void should_throw_when_fromYuanNull() {
            assertThatThrownBy(() -> Fen.fromYuan(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromYuan 超出 int 分范围拒绝")
        void should_throw_when_fromYuanOverflow() {
            assertThatThrownBy(() -> Fen.fromYuan(new BigDecimal("21474836.48")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between");
        }
    }

    @Nested
    @DisplayName("转换")
    class Conversion {

        @Test
        @DisplayName("toYuan 分转元精确")
        void should_convertToYuan() {
            assertThat(new Fen(1500).toYuan()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(new Fen(-100).toYuan()).isEqualByComparingTo(new BigDecimal("-1.00"));
        }

        @Test
        @DisplayName("toDisplayString 格式正确")
        void should_haveCorrectDisplayString() {
            assertThat(new Fen(1500).toDisplayString()).isEqualTo("15.00元");
        }
    }
}
