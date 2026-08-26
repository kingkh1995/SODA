package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("手机号值对象")
class MobileTest {

    private static final String VALID_MOBILE = "13800138000";

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法手机号创建实例")
        void should_create_when_validMobile() {
            var mobile = Mobile.of(VALID_MOBILE);
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
            assertThatThrownBy(() -> Mobile.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> Mobile.of("  " + VALID_MOBILE + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1380013800", "138001380000", "12300138000", "1380013800a", "abc", "  "})
        @DisplayName("非法手机号格式拒绝")
        void should_throw_when_invalidPattern(String invalid) {
            assertThatThrownBy(() -> Mobile.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(Mobile.of(VALID_MOBILE)).isEqualTo(Mobile.of(VALID_MOBILE));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(Mobile.of("13800138000")).isNotEqualTo(Mobile.of("13900139000"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = Mobile.of(VALID_MOBILE);
            var b = Mobile.of(VALID_MOBILE);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = Mobile.of(VALID_MOBILE);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, Mobile.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeToBareString() throws Exception {
            var json = MAPPER.writeValueAsString(Mobile.of("13800138000"));
            assertThat(json).isEqualTo("\"13800138000\"");
        }

        @Test
        @DisplayName("从裸字符串反序列化")
        void should_deserializeFromBareString() throws Exception {
            assertThat(MAPPER.readValue("\"13800138000\"", Mobile.class)).isEqualTo(Mobile.of("13800138000"));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("12345", Mobile.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确（脱敏）")
        void should_haveCorrectToString() {
            assertThat(Mobile.of(VALID_MOBILE)).hasToString("Mobile[masked=138****8000]");
        }
    }
}
