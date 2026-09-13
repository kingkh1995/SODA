package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EpochMilli 值对象")
class EpochMilliTest extends ComparableDomainPrimitiveContractTest<EpochMilli> {

    @Override
    protected Contract<EpochMilli> contract() {
        return new Contract<>(EpochMilli.class, () -> new EpochMilli(1738080000000L), "1738080000000",
                "EpochMilli[value=1738080000000]", "{\"value\":1738080000000}",
                () -> new EpochMilli(1738080000001L));
    }

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
            assertThat(EpochMilli.MIN.toInstant()).isEqualTo(Instant.EPOCH);
        }

        @Test
        @DisplayName("零值边界通过（>= 0 包含 0）")
        void should_accept_when_zero() {
            assertThat(new EpochMilli(0L).value()).isEqualTo(0L);
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
    @DisplayName("校验与异常")
    class Validation {

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
    }

    @Nested
    @DisplayName("Instant 互逆")
    class Inverse {

        @Test
        @DisplayName("毫秒精度 Instant 经 from→toInstant 无损还原")
        void should_roundTripLosslessly_when_millisPrecision() {
            var instant = Instant.parse("2026-08-09T12:00:00.123Z");
            assertThat(EpochMilli.from(instant).toInstant()).isEqualTo(instant);
        }

        @Test
        @DisplayName("亚毫秒截断到毫秒（毫秒单位契约）")
        void should_truncateSubMillis() {
            var em = EpochMilli.from(Instant.parse("2026-08-09T12:00:00.123456789Z"));
            assertThat(em.value()).isEqualTo(Instant.parse("2026-08-09T12:00:00.123Z").toEpochMilli());
            assertThat(em.toInstant()).isEqualTo(Instant.parse("2026-08-09T12:00:00.123Z"));
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
}
