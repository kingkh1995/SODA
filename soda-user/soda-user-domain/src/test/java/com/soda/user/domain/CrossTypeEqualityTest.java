package com.soda.user.domain;

import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PositiveInt;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.SocialAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨类型不等式契约（清单法）——本模块非枚举 record/class DP 两两恒不等。
 * <p>
 * 密封 AuthAccountId 子类的不等由此统一承载（四个 ID 子类均在清单内）；
 * 相等语义由 Lombok canEqual / record 构造性保证，套件防「自定义 equals
 * 遗漏 class 检查」的回归。新增 DP 只需在 {@link #inventory()} 登记一行规范实例。
 * <p>
 * 范围：排除枚举与 SecretValue（理由同组件模块清单套件）；
 * UserId 与组件层 LongId 的跨模块对亦入清单（旧逐对手写件的覆盖面不回退）。
 */
@DisplayName("跨类型不等式（DP 清单全对）")
class CrossTypeEqualityTest {

    /**
     * 单向断言即可：对称性由 final 类 + Lombok canEqual 构造保证。
     */
    private static List<Object> inventory() {
        return List.of(
                new Avatar("https://example.com/avatar.png"),
                Email.of("test@example.com"),
                new LongId(42),
                Mobile.of("13800138000"),
                PasswordAuthAccountId.of("P:42"),
                SmsAuthAccountId.of("S:13800138000"),
                EmailAuthAccountId.of("E:admin@test.com"),
                SocialAuthAccountId.of("O:GE:12345"),
                new UserId(42),
                new Username("alice"),
                new Nickname("nick"),
                new VerificationCode("123456", Instant.parse("2026-08-15T12:00:00Z")),
                new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS),
                new VerificationSource("UCC", "42"),
                new SmsRecipient(Mobile.of("13800138000")),
                new EmailRecipient(Email.of("test@example.com")));
    }

    @Test
    @DisplayName("清单内任意两类实例 equals 恒不相等")
    void should_neverEqualAcrossInventoryTypes() {
        var dps = inventory();
        // 清单自检：同一类不得重复登记（登记笔误即时暴露）
        assertThat(dps.stream().map(Object::getClass).distinct()).hasSize(dps.size());

        for (int i = 0; i < dps.size(); i++) {
            for (int j = i + 1; j < dps.size(); j++) {
                var left = dps.get(i);
                var right = dps.get(j);
                if (left.getClass() == right.getClass()) {
                    continue;
                }
                assertThat(left.equals(right))
                        .as("%s vs %s", left.getClass().getSimpleName(), right.getClass().getSimpleName())
                        .isFalse();
            }
        }
    }
}
