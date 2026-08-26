package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ciphertext —— JWE 信封自验证")
class CiphertextTest {

    private static String jwe(String headerJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        return String.join(".", header, "cek", "iv", "ct", "tag");
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("接受 alg=dir + enc=A256GCM + kid 的五段式密文")
        void should_create_when_validJwe() {
            var jwe = jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"phone-2024\"}");
            assertThat(new Ciphertext(jwe).value()).isEqualTo(jwe);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new Ciphertext(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝空白输入")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new Ciphertext("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝四段式（非 JWE compact）")
        void should_throw_when_fourSegments() {
            assertThatThrownBy(() -> new Ciphertext("a.b.c.d"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝裸 base64 密文（旧格式无信封元数据）")
        void should_throw_when_bareBase64() {
            assertThatThrownBy(() -> new Ciphertext(Base64.getEncoder().encodeToString(new byte[64])))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝非 dir 算法")
        void should_throw_when_wrongAlg() {
            assertThatThrownBy(() -> new Ciphertext(jwe("{\"alg\":\"RSA-OAEP\",\"enc\":\"A256GCM\",\"kid\":\"k\"}")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝缺失 kid（轮换期无法选钥）")
        void should_throw_when_missingKid() {
            assertThatThrownBy(() -> new Ciphertext(jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\"}")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            var header = "{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"k1\"}";
            assertThat(new Ciphertext(jwe(header))).isEqualTo(new Ciphertext(jwe(header)));
        }

        @Test
        @DisplayName("不同 kid 不等")
        void should_notBeEqual_when_differentKid() {
            assertThat(new Ciphertext(jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"k1\"}")))
                    .isNotEqualTo(new Ciphertext(jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"k2\"}")));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var header = "{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"k1\"}";
            assertThat(new Ciphertext(jwe(header))).hasSameHashCodeAs(new Ciphertext(jwe(header)));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（裸字符串形态）")
        void should_roundTrip() throws Exception {
            var original = new Ciphertext(jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"phone-2024\"}"));
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, Ciphertext.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"a.b.c.d\"", Ciphertext.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 输出完整密文（字面值非攻击素材，无遮蔽义务）")
        void should_haveCorrectToString() {
            var value = jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"phone-2024\"}");
            assertThat(new Ciphertext(value)).hasToString("Ciphertext[value=" + value + "]");
        }
    }
}
