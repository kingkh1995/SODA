package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WanYuan 值对象")
class WanYuanTest {

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
        @DisplayName("fromYuan 创建归一化实例")
        void should_create_when_fromYuan() {
            var amount = WanYuan.fromYuan(new BigDecimal("15000"));
            assertThat(amount.value()).isEqualTo("1.50");
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
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同归一化值相等")
        void should_beEqual_when_sameValue() {
            assertThat(WanYuan.of("1")).isEqualTo(WanYuan.of("1"));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(WanYuan.of("1")).isNotEqualTo(WanYuan.of("2"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(WanYuan.of("1")).hasSameHashCodeAs(WanYuan.of("1"));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = WanYuan.of("1.5");
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, WanYuan.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeToBareString() throws Exception {
            var json = MAPPER.writeValueAsString(WanYuan.of("1.5"));
            assertThat(json).isEqualTo("\"1.50\"");
        }

        @Test
        @DisplayName("从裸字符串反序列化")
        void should_deserializeFromBareString() throws Exception {
            assertThat(MAPPER.readValue("\"1.50\"", WanYuan.class)).isEqualTo(WanYuan.of("1.5"));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", WanYuan.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("数值序自然排序")
        void should_orderNaturally_when_valuesDiffer() {
            var low = WanYuan.of("1.50");
            var high = WanYuan.of("9.99");
            assertThat(low.compareTo(high)).isNegative();
            assertThat(high.compareTo(low)).isPositive();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var amount = WanYuan.of("1.50");
            var same = WanYuan.of("1.50");
            assertThat(amount.compareTo(same)).isZero();
            assertThat(amount).isEqualTo(same);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(WanYuan.of("1.5")).hasToString("WanYuan[value=1.50]");
        }
    }
}
