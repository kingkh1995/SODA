package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("随机字符串值对象")
class RandomStringTest extends DomainPrimitiveContractTest<RandomString> {

    @Override
    protected Contract<RandomString> contract() {
        return new Contract<>(RandomString.class, () -> new RandomString("abc"), "\"abc\"",
                "RandomString[value=abc]", "{}", () -> new RandomString("xyz"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var rs = new RandomString("AbCd123");
            assertThat(rs.value()).isEqualTo("AbCd123");
        }

        @Test
        @DisplayName("任意非空白字符串接受")
        void should_create_when_anyNonBlank() {
            var rs = new RandomString("任何非空白字符串_123!");
            assertThat(rs.value()).isEqualTo("任何非空白字符串_123!");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new RandomString(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串拒绝")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new RandomString("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

}
