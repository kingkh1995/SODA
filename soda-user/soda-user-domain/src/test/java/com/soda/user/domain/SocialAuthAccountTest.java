package com.soda.user.domain;

import com.soda.component.domain.types.Active;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SocialAuthAccountId;
import com.soda.user.domain.types.SocialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SocialAuthAccount} 单元测试。
 */
@DisplayName("SocialAuthAccount 社交认证账户")
class SocialAuthAccountTest {

    private static final SocialAuthAccountId ID = SocialAuthAccountId.from(SocialType.GE, "open123");

    private static SocialAuthAccount accountWith(Active active) {
        return SocialAuthAccount.builder().id(ID).active(active).build();
    }

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("构造时设置 ID")
        void should_setId_when_constructed() {
            assertThat(accountWith(Active.TRUE).getId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("账户类型返回 O")
        void should_returnTypeO_when_getAccountType() {
            assertThat(accountWith(Active.TRUE).getAccountType()).isEqualTo(AuthAccountType.O);
        }

        @Test
        @DisplayName("socialType 从 ID 派生")
        void should_deriveSocialType_when_getSocialType() {
            assertThat(accountWith(Active.TRUE).getSocialType()).isEqualTo(SocialType.GE);
        }

        @Test
        @DisplayName("openId 从 ID 派生")
        void should_deriveOpenId_when_getOpenId() {
            assertThat(accountWith(Active.TRUE).getOpenId()).isEqualTo("open123");
        }

        @Test
        @DisplayName("Active.TRUE 时启用")
        void should_beActive_when_activeIsTrue() {
            assertThat(accountWith(Active.TRUE).isActive()).isTrue();
        }

        @Test
        @DisplayName("Active.FALSE 时停用")
        void should_beInactive_when_activeIsFalse() {
            assertThat(accountWith(Active.FALSE).isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("createBuilder 缺 socialType 拒绝")
        void should_throw_when_socialTypeIsNull() {
            assertThatThrownBy(() -> SocialAuthAccount.createBuilder()
                    .socialType(null)
                    .openId("open123")
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("createBuilder 缺 openId 拒绝")
        void should_throw_when_openIdIsNull() {
            assertThatThrownBy(() -> SocialAuthAccount.createBuilder()
                    .socialType(SocialType.GE)
                    .openId(null)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("恢复路径 active 为 null 拒绝")
        void should_throw_when_activeIsNull() {
            assertThatThrownBy(() -> SocialAuthAccount.builder().id(ID).active(null).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class Factories {

        @Test
        @DisplayName("createBuilder 从社交平台与 openId 派生 ID 并默认启用")
        void should_setDefaults_when_usingCreateBuilder() {
            var account = SocialAuthAccount.createBuilder()
                    .socialType(SocialType.GE)
                    .openId("open123")
                    .build();

            assertThat(account.getId()).isEqualTo(ID);
            assertThat(account.isActive()).isTrue();
        }

        @Test
        @DisplayName("builder 恢复所有字段")
        void should_restoreAllFields_when_usingBuilder() {
            var account = SocialAuthAccount.builder()
                    .id(ID)
                    .active(Active.FALSE)
                    .build();

            assertThat(account.getId()).isEqualTo(ID);
            assertThat(account.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = SocialAuthAccount.createBuilder()
                    .socialType(SocialType.GE)
                    .openId("open123")
                    .build();
            var json = MAPPER.writeValueAsString(original);

            assertThat(MAPPER.readValue(json, SocialAuthAccount.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("缺少 id 的 JSON 拒绝")
        void should_reject_when_missingId() {
            var json = """
                    {"active":true}
                    """;

            assertThatThrownBy(() -> MAPPER.readValue(json, SocialAuthAccount.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同字段相等，不同 ID 不等")
        void should_beEqual_when_sameFields() {
            var same = accountWith(Active.TRUE);
            var equal = accountWith(Active.TRUE);
            var diffId = SocialAuthAccount.builder()
                    .id(SocialAuthAccountId.from(SocialType.GE, "otherOpen"))
                    .active(Active.TRUE)
                    .build();

            assertThat(same).isEqualTo(equal);
            assertThat(same).isNotEqualTo(diffId);
        }

    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 包含类名")
        void should_containClassName_when_toString() {
            assertThat(accountWith(Active.TRUE).toString()).contains("SocialAuthAccount@");
        }
    }
}
