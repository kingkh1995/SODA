package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LongId 值对象")
class LongIdTest extends ComparableDomainPrimitiveContractTest<LongId> {

    @Override
    protected Contract<LongId> contract() {
        return new Contract<>(LongId.class, () -> new LongId(42), "42",
                "LongId[value=42]", "\"not-a-number\"", () -> new LongId(43));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            assertThat(new LongId(42).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(LongId.parse("99")).isEqualTo(new LongId(99));
        }

    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("0 拒绝（minValue exclusive）")
        void should_throw_when_valueIsZero() {
            assertThatThrownBy(() -> new LongId(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("负值拒绝")
        void should_throw_when_negativeValue() {
            assertThatThrownBy(() -> new LongId(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> LongId.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> LongId.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("标识符")
    class Identity {

        @Test
        @DisplayName("identifier() 返回类型化底层值")
        void should_returnTypedValue_when_identifier() {
            assertThat(new LongId(42).identifier()).isEqualTo(42L);
        }
    }
}
