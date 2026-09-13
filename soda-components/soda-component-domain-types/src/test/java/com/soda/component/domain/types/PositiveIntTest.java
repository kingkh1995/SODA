package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PositiveInt 值对象")
class PositiveIntTest extends ComparableDomainPrimitiveContractTest<PositiveInt> {

    @Override
    protected Contract<PositiveInt> contract() {
        return new Contract<>(PositiveInt.class, () -> PositiveInt.of(6), "6",
                "PositiveInt[value=6]", "\"not-a-number\"", () -> PositiveInt.of(7));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(1) 创建单位值（值等价于 ONE）")
        void should_create_when_one() {
            assertThat(PositiveInt.of(1)).isEqualTo(PositiveInt.ONE);
        }

        @Test
        @DisplayName("of(1000) 创建大值")
        void should_create_when_largeValue() {
            assertThat(PositiveInt.of(1000).value()).isEqualTo(1000);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(PositiveInt.parse("6")).isEqualTo(PositiveInt.of(6));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("of(0) 拒绝")
        void should_throw_when_zero() {
            assertThatThrownBy(() -> PositiveInt.of(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(-1) 拒绝")
        void should_throw_when_negative() {
            assertThatThrownBy(() -> PositiveInt.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> PositiveInt.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> PositiveInt.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
