package com.soda.user.domain;

import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.SocialType;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;

@DisplayName("SocialAuthAccountId 值对象")
class SocialAuthAccountIdTest {

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(SocialType, openId) 创建实例")
        void should_createWithPrefix_when_fromSocialTypeAndOpenId() {
            var id = SocialAuthAccountId.from(SocialType.GE, "open123");
            assertThat(id.value()).isEqualTo("O:GE:open123");
            assertThat(id.socialType()).isEqualTo(SocialType.GE);
            assertThat(id.openId()).isEqualTo("open123");
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            assertThat(SocialAuthAccountId.from(SocialType.GE, "open123"))
                    .isEqualTo(SocialAuthAccountId.of("O:GE:open123"));
        }

        @Test
        @DisplayName("of 正确解析字符串")
        void should_create_when_validString() {
            var id = SocialAuthAccountId.of("O:GE:1");
            assertThat(id.value()).isEqualTo("O:GE:1");
            assertThat(id.socialType()).isEqualTo(SocialType.GE);
            assertThat(id.openId()).isEqualTo("1");
        }

        @Test
        @DisplayName("authAccountType 返回 O")
        void should_returnO_when_authAccountType() {
            assertThat(SocialAuthAccountId.ACCOUNT_TYPE).isEqualTo(AuthAccountType.O);
        }

        @Test
        @DisplayName("所有社交类型均可构造")
        void should_create_when_allSocialTypes() {
            for (var type : SocialType.values()) {
                var id = SocialAuthAccountId.from(type, "testOpenId");
                assertThat(id.socialType()).isEqualTo(type);
                assertThat(id.openId()).isEqualTo("testOpenId");
            }
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("of(null) 抛出异常")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> SocialAuthAccountId.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @DisplayName("of 非法字符串抛出异常")
        @ValueSource(strings = {"", "O:", "O:GE", "O:GE:", "social:GE:1", "O:UNKNOWN:1"})
        void should_throw_when_invalidString(String invalid) {
            assertThatThrownBy(() -> SocialAuthAccountId.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(SocialAuthAccountId.from(SocialType.GE, "1"))
                    .isEqualTo(SocialAuthAccountId.from(SocialType.GE, "1"));
        }

        @Test
        @DisplayName("不同社交类型不等")
        void should_notBeEqual_when_differentSocialType() {
            assertThat(SocialAuthAccountId.from(SocialType.GE, "1"))
                    .isNotEqualTo(SocialAuthAccountId.from(SocialType.DT, "1"));
        }

        @Test
        @DisplayName("不同 openId 不等")
        void should_notBeEqual_when_differentOpenId() {
            assertThat(SocialAuthAccountId.from(SocialType.GE, "1"))
                    .isNotEqualTo(SocialAuthAccountId.from(SocialType.GE, "2"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode_when_equal() {
            var a = SocialAuthAccountId.of("O:GE:12345");
            var b = SocialAuthAccountId.of("O:GE:12345");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 包含 value")
        void should_containValue_when_toString() {
            assertThat(SocialAuthAccountId.of("O:GE:12345"))
                    .hasToString("SocialAuthAccountId[value=O:GE:12345]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化反序列化")
        void should_roundTrip_when_validJson() throws Exception {
            var original = SocialAuthAccountId.from(SocialType.GE, "open123");
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).isEqualTo("\"O:GE:open123\"");
            var restored = MAPPER.readValue(json, SocialAuthAccountId.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 抛出异常")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"invalid\"", SocialAuthAccountId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 委托给字符串比较")
        void should_delegateToStringCompare_when_compareTo() {
            var a = SocialAuthAccountId.from(SocialType.GE, "1");
            var b = SocialAuthAccountId.from(SocialType.GE, "2");
            assertThat(a.compareTo(b) < 0).isTrue();
            assertThat(b.compareTo(a) > 0).isTrue();
            assertThat(a.compareTo(a) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals_when_compareTo() {
            var a = SocialAuthAccountId.of("O:GE:12345");
            var b = SocialAuthAccountId.of("O:GE:12345");
            assertThat(a.compareTo(b) == 0).isTrue();
        }
    }
}
