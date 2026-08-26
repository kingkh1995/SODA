package com.soda.component.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ParseUtils} 解析契约：不可靠输入（null / 类型不符 / 格式非法）统一抛出
 * {@link IllegalArgumentException}；{@code Number} 宽化接受，字符串输入自动 trim。
 */
@DisplayName("ParseUtils 解析工具")
class ParseUtilsTest {

    @Nested
    @DisplayName("解析为 int")
    class ParseInt {

        @Test
        @DisplayName("Integer 输入原样返回")
        void should_returnSameValue_when_integerInput() {
            assertThat(ParseUtils.parseInt(42)).isEqualTo(42);
        }

        @Test
        @DisplayName("Byte 输入宽化为 int")
        void should_widen_when_byteInput() {
            assertThat(ParseUtils.parseInt((byte) 7)).isEqualTo(7);
        }

        @Test
        @DisplayName("Short 输入宽化为 int")
        void should_widen_when_shortInput() {
            assertThat(ParseUtils.parseInt((short) 300)).isEqualTo(300);
        }

        @Test
        @DisplayName("合法数字字符串解析")
        void should_parse_when_validString() {
            assertThat(ParseUtils.parseInt("123")).isEqualTo(123);
        }

        @Test
        @DisplayName("字符串首尾空白自动 trim")
        void should_trim_when_surroundingSpaces() {
            assertThat(ParseUtils.parseInt("  456  ")).isEqualTo(456);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"abc"})
        @DisplayName("null / 非法格式拒绝")
        void should_throw_when_invalid(String value) {
            assertThatThrownBy(() -> ParseUtils.parseInt(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("不支持的类型拒绝")
        void should_throw_when_unsupportedType() {
            assertThatThrownBy(() -> ParseUtils.parseInt(new Object()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("解析为 long")
    class ParseLong {

        @Test
        @DisplayName("Long 输入原样返回")
        void should_returnSameValue_when_longInput() {
            assertThat(ParseUtils.parseLong(42L)).isEqualTo(42L);
        }

        @Test
        @DisplayName("Integer 输入宽化为 long")
        void should_widen_when_intInput() {
            assertThat(ParseUtils.parseLong(99)).isEqualTo(99L);
        }

        @Test
        @DisplayName("Byte 输入宽化为 long")
        void should_widen_when_byteInput() {
            assertThat(ParseUtils.parseLong((byte) 5)).isEqualTo(5L);
        }

        @Test
        @DisplayName("Short 输入宽化为 long")
        void should_widen_when_shortInput() {
            assertThat(ParseUtils.parseLong((short) 200)).isEqualTo(200L);
        }

        @Test
        @DisplayName("合法数字字符串解析（含 Long.MAX_VALUE）")
        void should_parse_when_maxLongString() {
            assertThat(ParseUtils.parseLong(String.valueOf(Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("字符串首尾空白自动 trim")
        void should_trim_when_surroundingSpaces() {
            assertThat(ParseUtils.parseLong("  789  ")).isEqualTo(789L);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"xyz"})
        @DisplayName("null / 非法格式拒绝")
        void should_throw_when_invalid(String value) {
            assertThatThrownBy(() -> ParseUtils.parseLong(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("不支持的类型拒绝")
        void should_throw_when_unsupportedType() {
            assertThatThrownBy(() -> ParseUtils.parseLong(new Object()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("解析为 URI")
    class ParseUri {

        @Test
        @DisplayName("http URL 通过")
        void should_accept_when_httpUrl() {
            ParseUtils.parseUri("http://example.com");
        }

        @Test
        @DisplayName("带路径与查询参数的 https URL 通过")
        void should_accept_when_httpsUrlWithQuery() {
            ParseUtils.parseUri("https://example.com/path?q=1");
        }

        @Test
        @DisplayName("相对 URI 原样保留（仅校验格式，不校验 scheme）")
        void should_keepRelativeUri_when_notAbsolute() {
            assertThat(ParseUtils.parseUri("not-a-uri")).isEqualTo(URI.create("not-a-uri"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null / 空串拒绝")
        void should_throw_when_blank(String value) {
            assertThatThrownBy(() -> ParseUtils.parseUri(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("解析为枚举")
    class ParseEnum {

        @Test
        @DisplayName("合法枚举名返回对应常量")
        void should_returnConstant_when_validName() {
            assertThat(ParseUtils.parseEnum(Thread.State.class, "RUNNABLE")).isEqualTo(Thread.State.RUNNABLE);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"NO_SUCH_STATE"})
        @DisplayName("null / 未知枚举名拒绝")
        void should_throw_when_unknownName(String value) {
            assertThatThrownBy(() -> ParseUtils.parseEnum(Thread.State.class, value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
