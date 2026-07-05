package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Percentage 值对象")
class PercentageTest {

    private static final ObjectMapper MAPPER = JacksonTestUtil.mapper();

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
        @DisplayName("parse 创建实例")
        void should_create_when_parseValidString() {
            var pct = Percentage.of("12.34");
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
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
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

        @Test
        @DisplayName("边界值 0 合法")
        void should_accept_when_valueIsZero() {
            var pct = Percentage.of("0");
            assertThat(pct.value()).isEqualTo("0.00");
        }

        @Test
        @DisplayName("边界值 100 合法")
        void should_accept_when_valueIs100() {
            var pct = Percentage.of("100");
            assertThat(pct.value()).isEqualTo("100.00");
        }
    }

    @Nested
    @DisplayName("归一化幂等")
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
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(Percentage.of("12.34")).isEqualTo(Percentage.of("12.34"));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(Percentage.of("12.34")).isNotEqualTo(Percentage.of("56.78"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(Percentage.of("12.34")).hasSameHashCodeAs(Percentage.of("12.34"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(Percentage.of("12.34")).hasToString("Percentage[value=12.34]");
        }
    }

    @Nested
    @DisplayName("常量")
    class Constants {
        @Test
        @DisplayName("ZERO 为 0%")
        void should_zeroBeZero() {
            assertThat(Percentage.of("0").value()).isEqualTo("0.00");
        }

        @Test
        @DisplayName("MAX 为 100%")
        void should_maxBe100() {
            assertThat(Percentage.of("100").value()).isEqualTo("100.00");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = Percentage.of("12.34");
            JacksonTestUtil.assertRoundTrip(original, Percentage.class);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", Percentage.class))
                    .isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeToBareString() throws Exception {
            var json = MAPPER.writeValueAsString(Percentage.of("12.34"));
            assertThat(json).isEqualTo("\"12.34\"");
        }

        @Test
        @DisplayName("从裸字符串反序列化")
        void should_deserializeFromBareString() throws Exception {
            assertThat(MAPPER.readValue("\"12.34\"", Percentage.class))
                    .isEqualTo(Percentage.of("12.34"));
        }
    }

    @Nested
    @DisplayName("富血方法")
    class RichMethods {
        @Test
        @DisplayName("toFraction 正确转换")
        void should_toFraction_beCorrect() {
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
        void should_toDisplayString_zero() {
            assertThat(Percentage.of("0").toDisplayString()).isEqualTo("0.00%");
        }

        @Test
        @DisplayName("toDisplayString 100%")
        void should_toDisplayString_max() {
            assertThat(Percentage.of("100").toDisplayString()).isEqualTo("100.00%");
        }
    }

    @Nested
    @DisplayName("可排序性")
    class ComparableTest {
        @Test
        @DisplayName("自然顺序正确")
        void should_compare_correctly() {
            var low = Percentage.of("12.34");
            var high = Percentage.of("56.78");
            assertThat(low).isLessThan(high);
            assertThat(high).isGreaterThan(low);
        }

        @Test
        @DisplayName("相等值排序一致")
        void should_compareEqual_when_sameValue() {
            assertThat(Percentage.of("50")).isEqualByComparingTo(Percentage.of("50"));
        }
    }
}
