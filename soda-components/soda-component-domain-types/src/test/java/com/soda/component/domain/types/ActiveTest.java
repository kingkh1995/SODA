package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Active 值对象")
class ActiveTest extends DomainPrimitiveContractTest<Active> {

    @Override
    protected Contract<Active> contract() {
        return new Contract<>(Active.class, () -> Active.of(true), "true",
                "Active[value=true]", "\"not-boolean\"", () -> Active.FALSE);
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(true) 返回 TRUE（值等价）")
        void should_returnTrue_when_ofTrue() {
            assertThat(Active.of(true)).isEqualTo(Active.TRUE);
        }

        @Test
        @DisplayName("of(false) 返回 FALSE（值等价）")
        void should_returnFalse_when_ofFalse() {
            assertThat(Active.of(false)).isEqualTo(Active.FALSE);
        }

        @Test
        @DisplayName("parse(\"true\") 返回 TRUE")
        void should_returnTrue_when_parseTrue() {
            assertThat(Active.parse("true")).isEqualTo(Active.TRUE);
        }

        @Test
        @DisplayName("parse(\"false\") 返回 FALSE")
        void should_returnFalse_when_parseFalse() {
            assertThat(Active.parse("false")).isEqualTo(Active.FALSE);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> Active.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> Active.parse("not-a-boolean"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
