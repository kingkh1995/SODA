package com.soda.user.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserVerificationScene 验证场景枚举")
class UserVerificationSceneTest {

    @Nested
    @DisplayName("查找")
    class Lookup {

        @Test
        @DisplayName("合法助记码查找")
        void should_returnScene_when_knownName() {
            assertThat(UserVerificationScene.of("UCC")).isEqualTo(UserVerificationScene.UCC);
            assertThat(UserVerificationScene.of("UPR")).isEqualTo(UserVerificationScene.UPR);
            assertThat(UserVerificationScene.of("ULG")).isEqualTo(UserVerificationScene.ULG);
            assertThat(UserVerificationScene.of("URG")).isEqualTo(UserVerificationScene.URG);
        }

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_nameIsNull() {
            assertThatThrownBy(() -> UserVerificationScene.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白拒绝")
        void should_throw_when_nameIsBlank() {
            assertThatThrownBy(() -> UserVerificationScene.of("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("未知助记码拒绝")
        void should_throw_when_unknownName() {
            assertThatThrownBy(() -> UserVerificationScene.of("XXX"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @Test
        @DisplayName("desc 返回英文描述")
        void should_returnDesc_when_display() {
            assertThat(UserVerificationScene.UCC.desc()).isEqualTo("user-credential-change");
            assertThat(UserVerificationScene.UPR.desc()).isEqualTo("user-password-reset");
            assertThat(UserVerificationScene.ULG.desc()).isEqualTo("user-login");
            assertThat(UserVerificationScene.URG.desc()).isEqualTo("user-register");
        }

        @Test
        @DisplayName("toString 返回 name")
        void should_returnName_when_toString() {
            assertThat(UserVerificationScene.UCC).hasToString("UCC");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = UserVerificationScene.UCC;
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, UserVerificationScene.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为 name 短串")
        void should_serializeToBareString() throws Exception {
            assertThat(MAPPER.writeValueAsString(UserVerificationScene.UCC)).isEqualTo("\"UCC\"");
        }

        @Test
        @DisplayName("非法名拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"XXX\"", UserVerificationScene.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
