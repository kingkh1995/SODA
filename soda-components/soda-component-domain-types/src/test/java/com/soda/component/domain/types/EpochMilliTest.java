package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.time.Duration;
import java.time.Instant;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EpochMilli 值对象")
class EpochMilliTest {

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("long 规范值")
        void should_exposeLongValue() {
            assertThat(new EpochMilli(1738080000000L).value()).isEqualTo(1738080000000L);
        }

        @Test
        @DisplayName("MIN 常量：epoch 起点 0L")
        void should_exposeMinConstant() {
            assertThat(EpochMilli.MIN.value()).isEqualTo(0L);
            assertThat(EpochMilli.MIN.instant()).isEqualTo(Instant.EPOCH);
        }

        @Test
        @DisplayName("零值边界通过（>= 0 包含 0）")
        void should_accept_when_zero() {
            assertThat(new EpochMilli(0L).value()).isEqualTo(0L);
        }

        @Test
        @DisplayName("负值拒绝（构造期抛 IAE）")
        void should_throw_when_negativeValue() {
            assertThatThrownBy(() -> new EpochMilli(-1L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new EpochMilli(Long.MIN_VALUE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("from(Instant) 拒绝：负 epoch 毫秒在 MySQL 模式无法落库")
        void should_throw_when_fromInstantNegative() {
            // epoch 之前 1 秒的 Instant：JDK Instant.toEpochMilli 不会溢出（仅 Instant.MIN/MAX 溢出），
            // 能走到构造器守卫，验证 IAE 由 EpochMilli 自身抛出
            var negativeInstant = Instant.EPOCH.minusSeconds(1L);
            assertThatThrownBy(() -> EpochMilli.from(negativeInstant))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("from(Instant.EPOCH) 接受（边界值 0L）")
        void should_accept_when_fromInstantEpoch() {
            assertThat(EpochMilli.from(Instant.EPOCH)).isEqualTo(EpochMilli.MIN);
        }

        @Test
        @DisplayName("from(Instant) 构造")
        void should_createFromInstant() {
            var instant = Instant.parse("2026-08-09T12:00:00Z");
            assertThat(EpochMilli.from(instant)).isEqualTo(new EpochMilli(instant.toEpochMilli()));
        }

        @Test
        @DisplayName("now() 近当前时刻")
        void should_createNow() {
            var now = EpochMilli.now();
            assertThat(now).isNotNull();
            assertThat(Math.abs(now.value() - Instant.now().toEpochMilli())).isLessThan(2_000L);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("基于值的相等语义：相同值相等、hashCode 一致、不同值不等")
        void should_beValueBased() {
            assertThat(new EpochMilli(1000L)).isEqualTo(new EpochMilli(1000L));
            assertThat(new EpochMilli(1000L)).hasSameHashCodeAs(new EpochMilli(1000L));
            assertThat(new EpochMilli(1000L)).isNotEqualTo(new EpochMilli(1001L));
        }
    }

    @Nested
    @DisplayName("Instant 互逆")
    class Inverse {

        @Test
        @DisplayName("毫秒精度 Instant 经 from→instant 无损还原")
        void should_roundTripLosslessly_when_millisPrecision() {
            var instant = Instant.parse("2026-08-09T12:00:00.123Z");
            assertThat(EpochMilli.from(instant).instant()).isEqualTo(instant);
        }

        @Test
        @DisplayName("亚毫秒截断到毫秒（毫秒单位契约）")
        void should_truncateSubMillis() {
            var em = EpochMilli.from(Instant.parse("2026-08-09T12:00:00.123456789Z"));
            assertThat(em.value()).isEqualTo(Instant.parse("2026-08-09T12:00:00.123Z").toEpochMilli());
            assertThat(em.instant()).isEqualTo(Instant.parse("2026-08-09T12:00:00.123Z"));
        }
    }

    @Nested
    @DisplayName("偏移与谓词")
    class Arithmetic {

        @Test
        @DisplayName("plus/minus(Duration) 偏移")
        void should_offsetByDuration() {
            var base = EpochMilli.from(Instant.parse("2026-08-09T12:00:00Z"));
            assertThat(base.plus(Duration.ofMinutes(5)))
                    .isEqualTo(EpochMilli.from(Instant.parse("2026-08-09T12:05:00Z")));
            assertThat(EpochMilli.from(Instant.parse("2026-08-09T12:05:00Z")).minus(Duration.ofMinutes(5)))
                    .isEqualTo(base);
        }

        @Test
        @DisplayName("isAfter/isBefore 谓词")
        void should_evaluatePredicates_whenComparing() {
            var earlier = EpochMilli.from(Instant.parse("2026-08-09T12:00:00Z"));
            var later = EpochMilli.from(Instant.parse("2026-08-09T13:00:00Z"));
            assertThat(later.isAfter(earlier)).isTrue();
            assertThat(earlier.isBefore(later)).isTrue();
            assertThat(earlier.isAfter(earlier)).isFalse();
            assertThat(earlier.isBefore(earlier)).isFalse();
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = new EpochMilli(1738080000000L);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, EpochMilli.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为裸 long 标量（非对象）—— @JsonValue 继承自 LongLiteralType")
        void should_serializeAsBareLongScalar() throws Exception {
            assertThat(MAPPER.writeValueAsString(new EpochMilli(1738080000000L)))
                    .isEqualTo("1738080000000");
        }

        @Test
        @DisplayName("从裸 long 反序列化")
        void should_deserializeFromBareLong() throws Exception {
            assertThat(MAPPER.readValue("1738080000000", EpochMilli.class))
                    .isEqualTo(new EpochMilli(1738080000000L));
        }

        @Test
        @DisplayName("拒绝对象形式")
        void should_rejectObjectForm() {
            assertThatThrownBy(() -> MAPPER.readValue("{\"value\":1738080000000}", EpochMilli.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("自然序按毫秒")
        void should_compareByMillis() {
            var a = new EpochMilli(1000L);
            var b = new EpochMilli(2000L);
            assertThat(a.compareTo(b)).isNegative();
            assertThat(b.compareTo(a)).isPositive();
            assertThat(a.compareTo(a)).isZero();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new EpochMilli(3000L);
            var same = new EpochMilli(3000L);
            assertThat(a.compareTo(same)).isZero();
            assertThat(a).isEqualTo(same);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new EpochMilli(1738080000000L)).hasToString("EpochMilli[value=1738080000000]");
        }
    }
}
