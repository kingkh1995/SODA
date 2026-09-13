package com.soda.user.domain.types;

import com.soda.component.domain.testutil.EnumDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserVerificationScene 验证场景枚举")
class UserVerificationSceneTest extends EnumDomainPrimitiveContractTest<UserVerificationScene> {

    @Override
    protected EnumContract<UserVerificationScene> contract() {
        return new EnumContract<>(UserVerificationScene.class, "\"XXX\"");
    }

    @Nested
    @DisplayName("查找")
    class Lookup {

        @ParameterizedTest(name = "of({0}) → {0}")
        @CsvSource({"UCC", "UPR", "ULG", "URG"})
        @DisplayName("合法名逐一解析")
        void should_lookup_when_validName(String name) {
            assertThat(UserVerificationScene.of(name)).isEqualTo(UserVerificationScene.valueOf(name));
        }

        @Test
        @DisplayName("null / 空 / 未知名拒绝")
        void should_throw_when_invalidName() {
            assertThatThrownBy(() -> UserVerificationScene.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> UserVerificationScene.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> UserVerificationScene.of("XXX"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(UserVerificationScene.values()).containsExactly(
                    UserVerificationScene.UCC, UserVerificationScene.UPR,
                    UserVerificationScene.ULG, UserVerificationScene.URG);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @ParameterizedTest(name = "{0} → desc={1}")
        @CsvSource(textBlock = """
                    UCC,   user-credential-change
                    UPR,   user-password-reset
                    ULG,   user-login
                    URG,   user-register
                """)
        @DisplayName("desc 逐常量")
        void should_exposeDesc(String name, String desc) {
            assertThat(UserVerificationScene.valueOf(name).desc()).isEqualTo(desc);
        }

        @Test
        @DisplayName("toString 为 name()")
        void should_haveNameToString() {
            assertThat(UserVerificationScene.UCC).hasToString("UCC");
        }
    }
}
