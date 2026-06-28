package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.soda.component.support.types.Mobile;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsAuthAccountId 值对象")
class SmsAuthAccountIdTest {

    private static final Mobile VALID_MOBILE = new Mobile("13800138000");

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(Mobile) 创建实例带 S: 前缀")
        void should_createWithPrefix_when_fromMobile() {
            var id = SmsAuthAccountId.from(VALID_MOBILE);
            assertThat(id.value()).isEqualTo("S:13800138000");
            assertThat(id.mobile()).isEqualTo(VALID_MOBILE);
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            assertThat(SmsAuthAccountId.from(VALID_MOBILE))
                    .isEqualTo(SmsAuthAccountId.of("S:13800138000"));
        }

        @Test
        @DisplayName("of 正确解析字符串")
        void should_create_when_validString() {
            assertThat(SmsAuthAccountId.of("S:13800138000").value()).isEqualTo("S:13800138000");
        }

        @Test
        @DisplayName("authAccountType 返回 S")
        void should_returnS_when_authAccountType() {
            assertThat(SmsAuthAccountId.ACCOUNT_TYPE).isEqualTo(AuthAccountType.S);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("from(null) 抛出异常")
        void should_throw_when_fromNull() {
            assertThatThrownBy(() -> SmsAuthAccountId.from(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(null) 抛出异常")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> SmsAuthAccountId.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @DisplayName("of 非法字符串抛出异常")
        @ValueSource(strings = {"", "S:", "sms:13800138000", "not-a-mobile"})
        void should_throw_when_invalidString(String invalid) {
            assertThatThrownBy(() -> SmsAuthAccountId.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(SmsAuthAccountId.from(new Mobile("13800138000")))
                    .isEqualTo(SmsAuthAccountId.from(new Mobile("13800138000")));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(SmsAuthAccountId.from(new Mobile("13800138000")))
                    .isNotEqualTo(SmsAuthAccountId.from(new Mobile("13900139000")));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode_when_equal() {
            var a = SmsAuthAccountId.of("S:13800138000");
            var b = SmsAuthAccountId.of("S:13800138000");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 包含 value")
        void should_containValue_when_toString() {
            assertThat(SmsAuthAccountId.of("S:13800138000"))
                    .hasToString("SmsAuthAccountId[value=S:13800138000]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化反序列化")
        void should_roundTrip_when_validJson() throws Exception {
            var original = SmsAuthAccountId.from(VALID_MOBILE);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).isEqualTo("\"S:13800138000\"");
            var restored = MAPPER.readValue(json, SmsAuthAccountId.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 抛出异常")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"invalid\"", SmsAuthAccountId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 委托给字符串比较")
        void should_delegateToStringCompare_when_compareTo() {
            var a = SmsAuthAccountId.from(new Mobile("13800138000"));
            var b = SmsAuthAccountId.from(new Mobile("13900139000"));
            assertThat(a.compareTo(b) < 0).isTrue();
            assertThat(b.compareTo(a) > 0).isTrue();
            assertThat(a.compareTo(a) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals_when_compareTo() {
            var a = SmsAuthAccountId.of("S:13800138000");
            var b = SmsAuthAccountId.of("S:13800138000");
            assertThat(a.compareTo(b) == 0).isTrue();
        }
    }
}
