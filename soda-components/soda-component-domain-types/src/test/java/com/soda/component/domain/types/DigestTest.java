package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Digest —— 32 字节等值摘要自验证")
class DigestTest extends DomainPrimitiveContractTest<Digest> {

    private static final String HEX_64 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    @Override
    protected Contract<Digest> contract() {
        return new Contract<>(Digest.class, () -> new Digest(HEX_64), "\"" + HEX_64 + "\"",
                "Digest[value=" + HEX_64 + "]", "\"not-a-digest\"", () -> new Digest("0".repeat(64)));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("接受 hex-64 摘要")
        void should_create_when_validHexLiteral() {
            assertThat(new Digest(HEX_64).value()).isEqualTo(HEX_64);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new Digest(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝空白输入")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new Digest("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝 63 位 hex")
        void should_throw_when_hexTooShort() {
            assertThatThrownBy(() -> new Digest(HEX_64.substring(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝大写 hex（唯一允许的字面值为小写规范形）")
        void should_throw_when_uppercaseHex() {
            assertThatThrownBy(() -> new Digest(HEX_64.toUpperCase(Locale.ROOT)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝裸口令哈希串（PHC 归 PasswordHash）")
        void should_throw_when_phcString() {
            assertThatThrownBy(() -> new Digest("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("fromBase64 接受标准 base64 的 32 字节摘要并归一化为小写 hex")
        void should_normalizeToLowerCaseHex_when_fromBase64() {
            var base64Of32ZeroBytes = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
            assertThat(Digest.fromBase64(base64Of32ZeroBytes).value()).isEqualTo("0".repeat(64));
        }

        @Test
        @DisplayName("fromBase64 与 of(hex) 构建等值（跨编码收敛同一规范形）")
        void should_convergeToCanonicalForm_when_fromBase64AndOf() {
            var base64Of32ZeroBytes = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
            assertThat(Digest.fromBase64(base64Of32ZeroBytes)).isEqualTo(new Digest("0".repeat(64)));
        }

        @Test
        @DisplayName("fromBase64 拒绝解码后非 32 字节的输入")
        void should_throw_when_decodedNot32Bytes() {
            var b31 = Base64.getEncoder().encodeToString(new byte[31]);
            assertThatThrownBy(() -> Digest.fromBase64(b31)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromBase64 拒绝省略 padding 的非规范输入（43 字符无 '='）")
        void should_throw_when_unpadded() {
            var unpadded = Base64.getEncoder().encodeToString(new byte[32]).substring(0, 43);
            assertThat(unpadded).hasSize(43);   // 规范形态为 44 字符、末尾恰一个 '='
            assertThatThrownBy(() -> Digest.fromBase64(unpadded)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of 与 fromBase64 均拒绝 url-safe base64（仅接受标准字母表）")
        void should_throw_when_base64Url() {
            var bytes = new byte[32];
            Arrays.fill(bytes, (byte) 0xfb);
            var urlSafe = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            assertThat(urlSafe).contains("-").contains("_");
            assertThatThrownBy(() -> new Digest(urlSafe)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Digest.fromBase64(urlSafe)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
