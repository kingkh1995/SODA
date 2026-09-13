package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.component.domain.util.TypeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConcurrencyVersion 值对象")
class ConcurrencyVersionTest extends ComparableDomainPrimitiveContractTest<ConcurrencyVersion> {

    @Override
    protected Contract<ConcurrencyVersion> contract() {
        return new Contract<>(ConcurrencyVersion.class, () -> ConcurrencyVersion.of(42), "42",
                "ConcurrencyVersion[value=42]", "\"not-a-number\"", () -> ConcurrencyVersion.of(43));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(0) 创建初始版本（值等价于 INITIAL）")
        void should_create_when_zero() {
            assertThat(ConcurrencyVersion.of(0)).isEqualTo(ConcurrencyVersion.INITIAL);
        }

        @Test
        @DisplayName("of(42) 创建实例")
        void should_create_when_validValue() {
            assertThat(ConcurrencyVersion.of(42).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(ConcurrencyVersion.parse("5")).isEqualTo(ConcurrencyVersion.of(5));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("of(-1) 拒绝")
        void should_throw_when_negativeValue() {
            assertThatThrownBy(() -> ConcurrencyVersion.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> ConcurrencyVersion.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> ConcurrencyVersion.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("from(null) 拒绝")
        void should_throw_when_fromNull() {
            assertThatThrownBy(() -> ConcurrencyVersion.from(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("转换")
    class Conversion {

        @Test
        @DisplayName("from(Integer) 与 of(int) 值等价")
        void should_beValueEquivalent_when_fromInteger() {
            assertThat(ConcurrencyVersion.from(3)).isEqualTo(ConcurrencyVersion.of(3));
        }

        @Test
        @DisplayName("from(Integer) 与 of(int) hashCode 一致")
        void should_haveSameHashCode_when_fromInteger() {
            assertThat(ConcurrencyVersion.from(3)).hasSameHashCodeAs(ConcurrencyVersion.of(3));
        }
    }

    @Nested
    @DisplayName("缓存透明性")
    class Cache {

        private static final int LOW = 0;

        private static final int HIGH = TypeConfig.PROVIDER.versionCacheHigh();

        @Test
        @DisplayName("下界值：无论是否命中缓存均值等价")
        void should_beValueEquivalent_when_atLowerBound() throws Exception {
            assertValueEquivalent(LOW);
        }

        @Test
        @DisplayName("上限值：无论是否命中缓存均值等价")
        void should_beValueEquivalent_when_atUpperBound() throws Exception {
            assertValueEquivalent(HIGH);
        }

        @Test
        @DisplayName("上限 +1：缓存外回落仍值等价")
        void should_beValueEquivalent_when_beyondUpperBound() throws Exception {
            assertValueEquivalent(HIGH + 1);
        }

        private void assertValueEquivalent(int value) throws Exception {
            var left = ConcurrencyVersion.of(value);
            var right = ConcurrencyVersion.of(value);
            assertThat(left).isEqualTo(right);
            assertThat(left).hasSameHashCodeAs(right);
            assertThat(MAPPER.readValue(MAPPER.writeValueAsString(left), ConcurrencyVersion.class)).isEqualTo(left);
        }
    }

    @Nested
    @DisplayName("步进")
    class Next {

        @Test
        @DisplayName("next() 返回值 +1 的新实例")
        void should_increment_when_next() {
            assertThat(ConcurrencyVersion.of(41).next()).isEqualTo(ConcurrencyVersion.of(42));
        }

        @Test
        @DisplayName("next() 不修改原实例")
        void should_notMutate_when_next() {
            var version = ConcurrencyVersion.of(41);
            assertThat(version.next()).isEqualTo(ConcurrencyVersion.of(42));
            assertThat(version).isEqualTo(ConcurrencyVersion.of(41));
        }
    }
}
