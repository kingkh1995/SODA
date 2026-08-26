package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Fen 值对象")
class FenTest {

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            assertThat(new Fen(1500).value()).isEqualTo(1500);
        }

        @Test
        @DisplayName("负值创建实例（退款）")
        void should_create_when_negative() {
            assertThat(new Fen(-100).value()).isEqualTo(-100);
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

        @Test
        @DisplayName("int 边界值创建实例")
        void should_create_when_intBoundaries() {
            assertThat(new Fen(Integer.MAX_VALUE).value()).isEqualTo(Integer.MAX_VALUE);
            assertThat(new Fen(Integer.MIN_VALUE).value()).isEqualTo(Integer.MIN_VALUE);
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
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Fen(1500)).isEqualTo(new Fen(1500));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Fen(1500)).isNotEqualTo(new Fen(1501));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new Fen(1500)).hasSameHashCodeAs(new Fen(1500));
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

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var positive = new Fen(1500);
            var json = MAPPER.writeValueAsString(positive);
            assertThat(MAPPER.readValue(json, Fen.class)).isEqualTo(positive);

            var negative = new Fen(-100);
            var negativeJson = MAPPER.writeValueAsString(negative);
            assertThat(MAPPER.readValue(negativeJson, Fen.class)).isEqualTo(negative);
        }

        @Test
        @DisplayName("序列化为裸数字")
        void should_serializeToBareNumber() throws Exception {
            var json = MAPPER.writeValueAsString(new Fen(42));
            assertThat(json).isEqualTo("42");
        }

        @Test
        @DisplayName("从裸数字反序列化")
        void should_deserializeFromBareNumber() throws Exception {
            assertThat(MAPPER.readValue("42", Fen.class)).isEqualTo(new Fen(42));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", Fen.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 按数值比较")
        void should_compareByNumericValue() {
            assertThat(new Fen(4).compareTo(new Fen(6)) < 0).isTrue();
            assertThat(new Fen(6).compareTo(new Fen(6)) == 0).isTrue();
            assertThat(new Fen(8).compareTo(new Fen(6)) > 0).isTrue();
            assertThat(new Fen(-5).compareTo(new Fen(4)) < 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new Fen(42);
            var same = new Fen(42);
            assertThat(a.compareTo(same) == 0).isTrue();
            assertThat(a).isEqualTo(same);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new Fen(42)).hasToString("Fen[value=42]");
        }
    }
}
