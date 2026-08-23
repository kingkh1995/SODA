package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ciphertext —— JWE 信封自验证")
class CiphertextTest {

    private static String jwe(String headerJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        return String.join(".", header, "cek", "iv", "ct", "tag");
    }

    @Test
    @DisplayName("接受 alg=dir + enc=A256GCM + kid 的五段式密文")
    void should_acceptValidJwe() {
        var jwe = jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"phone-2024\"}");
        assertThat(Ciphertext.of(jwe).value()).isEqualTo(jwe);
    }

    @Test
    @DisplayName("拒绝非 dir 算法")
    void should_rejectWrongAlg() {
        assertThatThrownBy(() -> Ciphertext.of(jwe("{\"alg\":\"RSA-OAEP\",\"enc\":\"A256GCM\",\"kid\":\"k\"}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝缺失 kid（轮换期无法选钥）")
    void should_rejectMissingKid() {
        assertThatThrownBy(() -> Ciphertext.of(jwe("{\"alg\":\"dir\",\"enc\":\"A256GCM\"}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝四段式（非 JWE compact）")
    void should_rejectFourSegments() {
        assertThatThrownBy(() -> Ciphertext.of("a.b.c.d"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝裸 base64 密文（旧格式无信封元数据）")
    void should_rejectBareBase64() {
        assertThatThrownBy(() -> Ciphertext.of(Base64.getEncoder().encodeToString(new byte[64])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝空白输入")
    void should_rejectBlank() {
        assertThatThrownBy(() -> Ciphertext.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
