package com.soda.component.support.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("随机字符串值对象")
class RandomStringTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var rs = new RandomString("AbCd123");
            assertThat(rs.value()).isEqualTo("AbCd123");
        }

        @Test
        @DisplayName("任意非空白字符串接受")
        void should_create_when_anyNonBlank() {
            var rs = new RandomString("任何非空白字符串_123!");
            assertThat(rs.value()).isEqualTo("任何非空白字符串_123!");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new RandomString(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new RandomString("abc")).isEqualTo(new RandomString("abc"));
        }


        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new RandomString("abc")).isNotEqualTo(new RandomString("xyz"));
        }
        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new RandomString("abc");
            var b = new RandomString("abc");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new RandomString("abc")).hasToString("RandomString[value=abc]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new RandomString("Abc123");
            JacksonTestUtil.assertRoundTrip(original, RandomString.class);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", RandomString.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    // ——— 业务方法 ———

    @Test
    @DisplayName("matches() 匹配相同字符串")
    void should_match_when_sameValue() {
        var rs = new RandomString("abc");
        assertThat(rs.matches("abc")).isTrue();
    }

    @Test
    @DisplayName("matches() 不匹配不同字符串")
    void should_notMatch_when_differentValue() {
        var rs = new RandomString("abc");
        assertThat(rs.matches("xyz")).isFalse();
    }
}
