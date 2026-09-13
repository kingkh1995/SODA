package com.soda.user.domain;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import com.soda.user.domain.types.Avatar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Avatar 值对象")
class AvatarTest extends DomainPrimitiveContractTest<Avatar> {

    private static final String VALID_URL = "https://example.com/avatar.png";

    @Override
    protected Contract<Avatar> contract() {
        return new Contract<>(Avatar.class, () -> new Avatar(VALID_URL), "\"" + VALID_URL + "\"",
                "Avatar[value=" + VALID_URL + "]", "\"not-a-uri\"",
                () -> new Avatar("https://a.com/2.png"));
    }

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

        @Test
        @DisplayName("相对路径拒绝")
        void should_throw_when_relativePath() {
            assertThatThrownBy(() -> new Avatar("/relative/path"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Avatar("relative"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Avatar("files/avatar.png"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("非 http/https 协议拒绝")
        void should_throw_when_nonHttpScheme() {
            assertThatThrownBy(() -> new Avatar("file:///local/1.png"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Avatar("ftp://files/avatar.png"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Avatar("data:image/png;base64,abc"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Avatar("  " + VALID_URL + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
