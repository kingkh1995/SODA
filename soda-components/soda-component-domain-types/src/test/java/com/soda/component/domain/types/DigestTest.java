package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Digest —— 32 字节等值摘要自验证")
class DigestTest {

    private static final String HEX_64 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

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
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Digest(HEX_64)).isEqualTo(new Digest(HEX_64));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Digest(HEX_64)).isNotEqualTo(new Digest("0".repeat(64)));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new Digest(HEX_64)).hasSameHashCodeAs(new Digest(HEX_64));
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
        @DisplayName("of 与 fromBase64 均拒绝 url-safe base64（仅接受标准字母表，ADR-0033 注记 2）")
        void should_throw_when_base64Url() {
            var bytes = new byte[32];
            Arrays.fill(bytes, (byte) 0xfb);
            var urlSafe = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            assertThat(urlSafe).contains("-").contains("_");
            assertThatThrownBy(() -> new Digest(urlSafe)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Digest.fromBase64(urlSafe)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = new Digest(HEX_64);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, Digest.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-digest\"", Digest.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new Digest(HEX_64)).hasToString("Digest[value=" + HEX_64 + "]");
        }
    }
}
