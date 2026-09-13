package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ciphertext —— JWE 信封自验证")
class CiphertextTest extends DomainPrimitiveContractTest<Ciphertext> {

    private static final String VALID_JWE = jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"phone-2024\"}");

    private static String jwe(String headerJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        return String.join(".", header, "cek", "iv", "ct", "tag");
    }

    @Override
    protected Contract<Ciphertext> contract() {
        return new Contract<>(Ciphertext.class, () -> new Ciphertext(VALID_JWE), "\"" + VALID_JWE + "\"",
                "Ciphertext[value=" + VALID_JWE + "]", "\"a.b.c.d\"",
                () -> new Ciphertext(jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"other\"}")));
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

        static Stream<String> invalidJwe() {
            return Stream.of(
                    "a.b.c.d",
                    Base64.getEncoder().encodeToString(new byte[64]),
                    jwe("{\"alg\":\"RSA-OAEP\",\"enc\":\"A256GCM\",\"kid\":\"k\"}"),
                    jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\"}"),
                    "   ");
        }

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new Ciphertext(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("invalidJwe")
        @DisplayName("非法 JWE 输入拒绝（段数 / 裸密文 / 算法 / kid / 空白）")
        void should_throw_when_invalidJwe(String value) {
            assertThatThrownBy(() -> new Ciphertext(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
