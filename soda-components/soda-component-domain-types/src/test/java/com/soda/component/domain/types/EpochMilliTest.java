package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;

import static com.soda.component.domain.testutil.JacksonTestUtil.assertRoundTrip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EpochMilli 值对象")
class EpochMilliTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("long 规范值")
        void should_exposeLongValue() {
            assertThat(new EpochMilli(1738080000000L).value()).isEqualTo(1738080000000L);
        }

        @Test
        @DisplayName("of(Instant) 构造")
        void should_create_fromInstant() {
            var instant = Instant.parse("2026-08-09T12:00:00Z");
            assertThat(EpochMilli.of(instant)).isEqualTo(new EpochMilli(instant.toEpochMilli()));
        }

        @Test
        @DisplayName("now() 近当前时刻")
        void should_create_now() {
            var now = EpochMilli.now();
            assertThat(now).isNotNull();
            assertThat(Math.abs(now.value() - Instant.now().toEpochMilli())).isLessThan(2_000L);
        }
    }

    @Nested
    @DisplayName("Instant 互逆")
    class Inverse {

        @Test
        @DisplayName("毫秒精度 Instant 经 of→instant 无损还原")
        void should_roundTripLosslessly_atMillisPrecision() {
            var instant = Instant.parse("2026-08-09T12:00:00.123Z");
            assertThat(EpochMilli.of(instant).instant()).isEqualTo(instant);
        }

        @Test
        @DisplayName("亚毫秒截断到毫秒（毫秒单位契约）")
        void should_truncateSubMillis() {
            var em = EpochMilli.of(Instant.parse("2026-08-09T12:00:00.123456789Z"));
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
            var base = EpochMilli.of(Instant.parse("2026-08-09T12:00:00Z"));
            assertThat(base.plus(Duration.ofMinutes(5)))
                    .isEqualTo(EpochMilli.of(Instant.parse("2026-08-09T12:05:00Z")));
            assertThat(EpochMilli.of(Instant.parse("2026-08-09T12:05:00Z")).minus(Duration.ofMinutes(5)))
                    .isEqualTo(base);
        }

        @Test
        @DisplayName("isAfter/isBefore 谓词")
        void should_compare() {
            var earlier = EpochMilli.of(Instant.parse("2026-08-09T12:00:00Z"));
            var later = EpochMilli.of(Instant.parse("2026-08-09T13:00:00Z"));
            assertThat(later.isAfter(earlier)).isTrue();
            assertThat(earlier.isBefore(later)).isTrue();
            assertThat(earlier.isAfter(earlier)).isFalse();
            assertThat(earlier.isBefore(earlier)).isFalse();
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("value-based identity")
        void should_beValueBased() {
            assertThat(new EpochMilli(1000L)).isEqualTo(new EpochMilli(1000L));
            assertThat(new EpochMilli(1000L)).hasSameHashCodeAs(new EpochMilli(1000L));
            assertThat(new EpochMilli(1000L)).isNotEqualTo(new EpochMilli(1001L));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("round-trip 一致")
        void should_roundTrip() throws Exception {
            assertRoundTrip(new EpochMilli(1738080000000L), EpochMilli.class);
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
    }
}
