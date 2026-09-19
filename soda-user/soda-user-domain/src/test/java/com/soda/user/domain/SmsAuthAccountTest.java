package com.soda.user.domain;

import static com.soda.user.domain.DomainTestUtil.MAPPER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;

/** {@link SmsAuthAccount} 单元测试。 */
@DisplayName("SmsAuthAccount 短信认证账户")
class SmsAuthAccountTest {

    private static final Mobile MOBILE = Mobile.of("13800138000");
    private static final SmsAuthAccountId ID = SmsAuthAccountId.from(MOBILE);

    private static SmsAuthAccount accountWith(Active active) {
        return SmsAuthAccount.builder().id(ID).active(active).build();
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
        @DisplayName("账户类型返回 S")
        void should_returnTypeS_when_getAccountType() {
            assertThat(accountWith(Active.TRUE).getAccountType()).isEqualTo(AuthAccountType.S);
        }

        @Test
        @DisplayName("mobile 从 ID 派生")
        void should_deriveMobile_when_getMobile() {
            assertThat(accountWith(Active.TRUE).getMobile()).isEqualTo(MOBILE);
        }

        @Test
        @DisplayName("Active.TRUE 时启用")
        void should_beActive_when_activeIsTrue() {
            assertThat(accountWith(Active.TRUE).isActive()).isTrue();
        }

        @Test
        @DisplayName("默认策略为短信默认策略常量")
        void should_useDefaultSmsPolicy_when_defaultPolicy() {
            assertThat(SmsAuthAccount.DEFAULT_POLICY).isEqualTo(VerificationCodePolicy.DEFAULT_SMS);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("createBuilder 缺 mobile 拒绝")
        void should_throw_when_mobileIsNull() {
            assertThatThrownBy(() -> SmsAuthAccount.createBuilder().mobile(null).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("恢复路径 active 为 null 拒绝")
        void should_throw_when_activeIsNull() {
            assertThatThrownBy(() -> SmsAuthAccount.builder().id(ID).active(null).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class Factories {

        @Test
        @DisplayName("createBuilder 从 mobile 派生 ID 并默认启用")
        void should_setDefaults_when_usingCreateBuilder() {
            var account = SmsAuthAccount.createBuilder().mobile(MOBILE).build();

            assertThat(account.getId()).isEqualTo(ID);
            assertThat(account.isActive()).isTrue();
        }

        @Test
        @DisplayName("builder 恢复所有字段")
        void should_restoreAllFields_when_usingBuilder() {
            var account = SmsAuthAccount.builder().id(ID).active(Active.FALSE).build();

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
            var account = SmsAuthAccount.createBuilder().mobile(MOBILE).build();
            var json = MAPPER.writeValueAsString(account);
            var restored = MAPPER.readValue(json, SmsAuthAccount.class);

            // 实体不重写 equals（引用相等，见 ADR-0040）：round-trip 的「不丢字段」保证由逐字段比对承担
            assertThat(restored.getId()).isEqualTo(account.getId());
            assertThat(restored.isActive()).isEqualTo(account.isActive());
        }

        @Test
        @DisplayName("缺少 id 的 JSON 拒绝")
        void should_reject_when_missingId() {
            var json =
                    """
                    {"active":true}
                    """;

            assertThatThrownBy(() -> MAPPER.readValue(json, SmsAuthAccount.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("同 id 不同实例不相等（引用相等，见 ADR-0040）")
        void should_notBeEqual_when_sameFieldsButDifferentInstances() {
            var same = accountWith(Active.TRUE);
            var equal = accountWith(Active.TRUE);

            assertThat(same.getId()).isEqualTo(equal.getId());
            assertThat(same).isNotSameAs(equal);
            assertThat(same).isNotEqualTo(equal);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 包含类名")
        void should_containClassName_when_toString() {
            assertThat(accountWith(Active.TRUE).toString()).contains("SmsAuthAccount@");
        }
    }
}
