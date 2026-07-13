package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("短信内容值对象")
class SmsContentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法内容创建实例")
        void should_create_when_validContent() {
            var content = new SmsContent("您的验证码是123456");
            assertThat(content.value()).isEqualTo("您的验证码是123456");
        }

        @Test
        @DisplayName("最大长度创建实例")
        void should_create_when_maxLength() {
            var content = new SmsContent("a".repeat(70));
            assertThat(content.value().length()).isEqualTo(70);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new SmsContent(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超长字符串拒绝")
        void should_throw_when_tooLong() {
            assertThatThrownBy(() -> new SmsContent("a".repeat(71)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new SmsContent("hello")).isEqualTo(new SmsContent("hello"));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new SmsContent("hello")).isNotEqualTo(new SmsContent("world"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new SmsContent("hello");
            var b = new SmsContent("hello");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new SmsContent("hello")).hasToString("SmsContent[value=hello]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new SmsContent("hello");
            JacksonTestUtil.assertRoundTrip(original, SmsContent.class);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", SmsContent.class))
                    .isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeToBareString() throws Exception {
            var json = MAPPER.writeValueAsString(new SmsContent("hello"));
            assertThat(json).isEqualTo("\"hello\"");
        }

        @Test
        @DisplayName("从裸字符串反序列化")
        void should_deserializeFromBareString() throws Exception {
            assertThat(MAPPER.readValue("\"hello\"", SmsContent.class)).isEqualTo(new SmsContent("hello"));
        }
    }
}
