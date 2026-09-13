package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SocialAuthAccountId;
import com.soda.user.domain.types.SocialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialAuthAccountId 值对象")
class SocialAuthAccountIdTest extends ComparableDomainPrimitiveContractTest<SocialAuthAccountId> {

    @Override
    protected Contract<SocialAuthAccountId> contract() {
        return new Contract<>(SocialAuthAccountId.class, () -> SocialAuthAccountId.of("O:GE:12345"),
                "\"O:GE:12345\"", "SocialAuthAccountId[value=O:GE:12345]", "\"invalid\"",
                () -> SocialAuthAccountId.of("O:GE:12346"));
    }

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
            assertThat(SocialAuthAccountId.of("O:GE:1").accountType()).isEqualTo(AuthAccountType.O);
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
    @DisplayName("路由")
    class Routing {
        @Test
        @DisplayName("JSON 反序列化以本子类为声明类型")
        void should_routeToSocialSubtype_when_json() throws Exception {
            assertThat(MAPPER.readValue("\"O:GE:1\"", SocialAuthAccountId.class))
                    .isEqualTo(SocialAuthAccountId.of("O:GE:1"));
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
    @DisplayName("标识符")
    class Identity {
        @Test
        @DisplayName("identifier 返回规范化编码值")
        void should_returnCanonicalValue_when_identifier() {
            assertThat(SocialAuthAccountId.of("O:GE:12345").identifier()).isEqualTo("O:GE:12345");
        }
    }
}
