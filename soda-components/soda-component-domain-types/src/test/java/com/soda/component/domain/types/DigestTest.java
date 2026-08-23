package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Digest —— 32 字节等值摘要自验证")
class DigestTest {

    private static final String HEX_64 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private static String base64Of32Bytes() {
        return Base64.getEncoder().encodeToString(new byte[32]);
    }

    @Test
    @DisplayName("接受 hex-64 摘要")
    void should_acceptHex() {
        assertThat(Digest.of(HEX_64).value()).isEqualTo(HEX_64);
    }

    @Test
    @DisplayName("fromBase64 接受标准 base64 的 32 字节摘要并归一化为小写 hex")
    void should_acceptBase64ViaFromFactory() {
        assertThat(Digest.fromBase64(base64Of32Bytes()).value()).isEqualTo("0".repeat(64));
    }

    @Test
    @DisplayName("fromBase64 拒绝解码后非 32 字节的输入")
    void should_rejectWrongLength() {
        var b31 = Base64.getEncoder().encodeToString(new byte[31]);
        assertThatThrownBy(() -> Digest.fromBase64(b31)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝 63 位 hex")
    void should_rejectShortHex() {
        assertThatThrownBy(() -> Digest.of(HEX_64.substring(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝裸口令哈希串（PHC 归 PasswordHash）")
    void should_rejectPhcString() {
        assertThatThrownBy(() -> Digest.of("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of 与 fromBase64 均拒绝 url-safe base64（仅接受标准字母表，ADR-0033 注记 2）")
    void should_rejectBase64Url() {
        var bytes = new byte[32];
        Arrays.fill(bytes, (byte) 0xfb);
        var urlSafe = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        assertThat(urlSafe).contains("-").contains("_");
        assertThatThrownBy(() -> Digest.of(urlSafe)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Digest.fromBase64(urlSafe)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝空白输入")
    void should_rejectBlank() {
        assertThatThrownBy(() -> Digest.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("拒绝大写 hex（唯一允许的字面值为小写规范形）")
    void should_rejectUppercaseHex() {
        assertThatThrownBy(() -> Digest.of(HEX_64.toUpperCase(Locale.ROOT)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("fromBase64 与 of(hex) 构建等值（跨编码收敛同一规范形）")
    void should_convergeEncodingsToCanonicalForm() {
        assertThat(Digest.fromBase64(base64Of32Bytes())).isEqualTo(Digest.of("0".repeat(64)));
    }
}
