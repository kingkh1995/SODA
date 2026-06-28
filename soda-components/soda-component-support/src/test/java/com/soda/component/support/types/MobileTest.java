package com.soda.component.support.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("手机号值对象")
class MobileTest {

    private static final String VALID_MOBILE = "13800138000";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法手机号创建实例")
        void should_create_when_validMobile() {
            var mobile = new Mobile(VALID_MOBILE);
            assertThat(mobile.value()).isEqualTo(VALID_MOBILE);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new Mobile(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Mobile("  " + VALID_MOBILE + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1380013800", "138001380000", "12300138000", "1380013800a", "abc", "  "})
        @DisplayName("非法手机号格式拒绝")
        void should_throw_when_invalidPattern(String invalid) {
            assertThatThrownBy(() -> new Mobile(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Mobile(VALID_MOBILE)).isEqualTo(new Mobile(VALID_MOBILE));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Mobile("13800138000")).isNotEqualTo(new Mobile("13900139000"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new Mobile(VALID_MOBILE);
            var b = new Mobile(VALID_MOBILE);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new Mobile(VALID_MOBILE)).hasToString("Mobile[value=13800138000]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new Mobile(VALID_MOBILE);
            JacksonTestUtil.assertRoundTrip(original, Mobile.class);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("12345", Mobile.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }
}
