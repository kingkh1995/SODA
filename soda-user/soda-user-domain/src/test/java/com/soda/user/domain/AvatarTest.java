package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Avatar 值对象")
class AvatarTest {

    private static final String VALID_URL = "https://example.com/avatar.png";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法 URL 创建实例")
        void should_create_when_validUrl() {
            var a = new Avatar(VALID_URL);
            assertThat(a.value()).isEqualTo(VALID_URL);
        }

        @Test
        @DisplayName("http 协议接受")
        void should_create_when_httpScheme() {
            assertThatCode(() -> new Avatar("http://example.com/avatar.png"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "not a url"})
        @DisplayName("null/空/非法字符串拒绝")
        void should_throw_when_invalid(String invalid) {
            assertThatThrownBy(() -> new Avatar(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/relative/path", "relative", "files/avatar.png"})
        @DisplayName("相对路径拒绝")
        void should_throw_when_relativePath(String relative) {
            assertThatThrownBy(() -> new Avatar(relative))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"file:///local/1.png", "ftp://files/avatar.png", "data:image/png;base64,abc"})
        @DisplayName("非 http/https 协议拒绝")
        void should_throw_when_nonHttpScheme(String nonHttp) {
            assertThatThrownBy(() -> new Avatar(nonHttp))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Avatar("  " + VALID_URL + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同 URL 相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Avatar(VALID_URL)).isEqualTo(new Avatar(VALID_URL));
        }

        @Test
        @DisplayName("不同 URL 不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Avatar("https://a.com/1.png")).isNotEqualTo(new Avatar("https://a.com/2.png"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new Avatar(VALID_URL)).hasSameHashCodeAs(new Avatar(VALID_URL));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_formatToString() {
            assertThat(new Avatar("https://example.com/avatar.png"))
                    .hasToString("Avatar[value=https://example.com/avatar.png]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化与反序列化")
        void should_roundTrip() throws Exception {
            var original = new Avatar(VALID_URL);
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, Avatar.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-uri\"", Avatar.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }
}
