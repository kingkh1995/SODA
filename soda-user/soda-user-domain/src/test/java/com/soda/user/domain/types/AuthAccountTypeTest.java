package com.soda.user.domain.types;

import com.soda.component.domain.testutil.EnumDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthAccountType 枚举")
class AuthAccountTypeTest extends EnumDomainPrimitiveContractTest<AuthAccountType> {

    @Override
    protected EnumContract<AuthAccountType> contract() {
        return new EnumContract<>(AuthAccountType.class, "\"INVALID\"");
    }

    @Nested
    @DisplayName("查找")
    class Lookup {

        @ParameterizedTest(name = "of({0}) → {0}")
        @CsvSource({"P", "S", "E", "O"})
        @DisplayName("合法名逐一解析")
        void should_lookup_when_validName(String name) {
            assertThat(AuthAccountType.of(name)).isEqualTo(AuthAccountType.valueOf(name));
        }

        @Test
        @DisplayName("null / 空 / 未知名拒绝")
        void should_throw_when_invalidName() {
            assertThatThrownBy(() -> AuthAccountType.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> AuthAccountType.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> AuthAccountType.of("X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(AuthAccountType.values())
                    .containsExactly(AuthAccountType.P, AuthAccountType.S, AuthAccountType.E, AuthAccountType.O);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @ParameterizedTest(name = "{0} → desc={1}")
        @CsvSource(textBlock = """
                    P,     password
                    S,     sms
                    E,     email
                    O,     oauth
                """)
        @DisplayName("desc 逐常量")
        void should_exposeDesc(String name, String desc) {
            assertThat(AuthAccountType.valueOf(name).desc()).isEqualTo(desc);
        }

        @Test
        @DisplayName("toString 为 name()")
        void should_haveNameToString() {
            assertThat(AuthAccountType.P).hasToString("P");
        }
    }
}
