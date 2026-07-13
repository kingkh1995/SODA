package com.soda.user.domain;

import com.soda.component.domain.types.LongId;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.SocialAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.Username;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨类型不等式测试 — 验证不同 DP 类型之间永不相等。
 *
 * @see <a href="file:dp-conventions.md">DP Conventions</a>
 */
class CrossTypeEqualityTest {

    @Test
    void password_notEqualToSmsAuthAccountId() {
        assertThat(PasswordAuthAccountId.of("P:42"))
                .isNotEqualTo(SmsAuthAccountId.of("S:13800138000"));
    }

    @Test
    void password_notEqualToEmailAuthAccountId() {
        assertThat(PasswordAuthAccountId.of("P:42"))
                .isNotEqualTo(EmailAuthAccountId.of("E:admin@test.com"));
    }

    @Test
    void password_notEqualToSocialAuthAccountId() {
        assertThat(PasswordAuthAccountId.of("P:42"))
                .isNotEqualTo(SocialAuthAccountId.of("O:GE:12345"));
    }

    @Test
    void sms_notEqualToEmailAuthAccountId() {
        assertThat(SmsAuthAccountId.of("S:13800138000"))
                .isNotEqualTo(EmailAuthAccountId.of("E:admin@test.com"));
    }

    @Test
    void sms_notEqualToSocialAuthAccountId() {
        assertThat(SmsAuthAccountId.of("S:13800138000"))
                .isNotEqualTo(SocialAuthAccountId.of("O:GE:12345"));
    }

    @Test
    void email_notEqualToSocialAuthAccountId() {
        assertThat(EmailAuthAccountId.of("E:admin@test.com"))
                .isNotEqualTo(SocialAuthAccountId.of("O:GE:12345"));
    }

    @Test
    void userId_notEqualToNickname() {
        assertThat(new UserId(42))
                .isNotEqualTo(new Nickname("42"));
    }

    @Test
    void userId_notEqualToUsername() {
        assertThat(new UserId(42))
                .isNotEqualTo(new Username("0042"));
    }

    @Test
    void userId_notEqualToLongId() {
        assertThat(new UserId(1L))
                .isNotEqualTo(new LongId(1L));
    }
}
