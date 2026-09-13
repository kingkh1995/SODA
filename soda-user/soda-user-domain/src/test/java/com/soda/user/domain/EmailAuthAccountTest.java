package com.soda.user.domain;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EmailAuthAccount} 单元测试。
 */
@DisplayName("EmailAuthAccount 邮箱认证账户")
class EmailAuthAccountTest {

    private static final Email EMAIL = Email.of("test@example.com");
    private static final EmailAuthAccountId ID = EmailAuthAccountId.from(EMAIL);

    private static EmailAuthAccount accountWith(Active active) {
        return EmailAuthAccount.builder().id(ID).active(active).build();
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
        @DisplayName("账户类型返回 E")
        void should_returnTypeE_when_getAccountType() {
            assertThat(accountWith(Active.TRUE).getAccountType()).isEqualTo(AuthAccountType.E);
        }

        @Test
        @DisplayName("email 从 ID 派生")
        void should_deriveEmail_when_getEmail() {
            assertThat(accountWith(Active.TRUE).getEmail()).isEqualTo(EMAIL);
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

        @Test
        @DisplayName("默认策略为邮箱默认策略常量")
        void should_useDefaultEmailPolicy_when_defaultPolicy() {
            assertThat(EmailAuthAccount.DEFAULT_POLICY).isEqualTo(VerificationCodePolicy.DEFAULT_EMAIL);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("createBuilder 缺 email 拒绝")
        void should_throw_when_emailIsNull() {
            assertThatThrownBy(() -> EmailAuthAccount.createBuilder().email(null).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("恢复路径 active 为 null 拒绝")
        void should_throw_when_activeIsNull() {
            assertThatThrownBy(() -> EmailAuthAccount.builder().id(ID).active(null).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class Factories {

        @Test
        @DisplayName("createBuilder 从 email 派生 ID 并默认启用")
        void should_setDefaults_when_usingCreateBuilder() {
            var account = EmailAuthAccount.createBuilder()
                    .email(EMAIL)
                    .build();

            assertThat(account.getId()).isEqualTo(EmailAuthAccountId.from(EMAIL));
            assertThat(account.isActive()).isTrue();
        }

        @Test
        @DisplayName("builder 恢复所有字段")
        void should_restoreAllFields_when_usingBuilder() {
            var account = EmailAuthAccount.builder()
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
            var account = accountWith(Active.TRUE);
            var json = MAPPER.writeValueAsString(account);

            assertThat(MAPPER.readValue(json, EmailAuthAccount.class)).isEqualTo(account);
        }

        @Test
        @DisplayName("缺少 id 的 JSON 拒绝")
        void should_reject_when_missingId() {
            var json = """
                    {"active":true}
                    """;

            assertThatThrownBy(() -> MAPPER.readValue(json, EmailAuthAccount.class))
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
            var diffId = EmailAuthAccount.builder()
                    .id(EmailAuthAccountId.from(Email.of("other@example.com")))
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
            assertThat(accountWith(Active.TRUE).toString()).contains("EmailAuthAccount@");
        }
    }
}
