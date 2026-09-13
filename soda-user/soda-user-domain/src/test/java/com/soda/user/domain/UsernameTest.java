package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.user.domain.types.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Username 值对象")
class UsernameTest extends ComparableDomainPrimitiveContractTest<Username> {

    private static final String VALID_USERNAME = "testuser";
    private static final String LONG_USERNAME = "a".repeat(31);

    @Override
    protected Contract<Username> contract() {
        return new Contract<>(Username.class, () -> new Username(VALID_USERNAME), "\"testuser\"",
                "Username[value=testuser]", "{}", () -> new Username("ztestuser"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法用户名创建实例")
        void should_create_when_validValue() {
            var u = new Username(VALID_USERNAME);
            assertThat(u.value()).isEqualTo(VALID_USERNAME);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "abc", "user name!", "用户名"})
        @DisplayName("null/空/非法字符串拒绝")
        void should_throw_when_invalid(String invalid) {
            assertThatThrownBy(() -> new Username(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超长拒绝")
        void should_throw_when_tooLong() {
            assertThatThrownBy(() -> new Username(LONG_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Username("  " + VALID_USERNAME + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
