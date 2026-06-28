package com.soda.user.domain;

import com.soda.component.support.types.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VerificationCode 业务逻辑")
class VerificationCodeBusinessLogicTest {

    private static final String CODE = "123456";
    private static final Instant FUTURE = Instant.now().plus(1, ChronoUnit.HOURS);
    private static final Instant PAST = Instant.now().minus(1, ChronoUnit.HOURS);

    @Test
    @DisplayName("未来过期时间返回未过期")
    void expired_futureDate_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertThat(vc.expired()).isFalse();
    }

    @Test
    @DisplayName("过去过期时间返回已过期")
    void expired_pastDate_returnsTrue() {
        var vc = new VerificationCode(CODE, PAST, false);
        assertThat(vc.expired()).isTrue();
    }

    @Test
    @DisplayName("expiredAt 精确边界：expireAt 之后 1ns 返回 true")
    void expiredAt_afterExpiry_returnsTrue() {
        var expireAt = Instant.parse("2026-06-28T12:00:00.000Z");
        var vc = new VerificationCode(CODE, expireAt, false);
        assertThat(vc.expiredAt(expireAt.plusNanos(1))).isTrue();
    }

    @Test
    @DisplayName("expiredAt 精确边界：expireAt 之前 1ns 返回 false")
    void expiredAt_beforeExpiry_returnsFalse() {
        var expireAt = Instant.parse("2026-06-28T12:00:00.000Z");
        var vc = new VerificationCode(CODE, expireAt, false);
        assertThat(vc.expiredAt(expireAt.minusNanos(1))).isFalse();
    }

    @Test
    @DisplayName("expiredAt 精确边界：expireAt 同一时刻返回 false（严格 after）")
    void expiredAt_exactExpiry_returnsFalse() {
        var expireAt = Instant.parse("2026-06-28T12:00:00.000Z");
        var vc = new VerificationCode(CODE, expireAt, false);
        assertThat(vc.expiredAt(expireAt)).isFalse();
    }

    @Test
    @DisplayName("新实例 used 为 false")
    void used_fresh_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertThat(vc.used()).isFalse();
    }

    @Test
    @DisplayName("use() 返回新实例且 used 为 true，原实例不变")
    void use_createsNewInstanceWithUsedTrue() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        var used = vc.use();
        assertThat(used).isNotSameAs(vc);
        assertThat(used.code()).isEqualTo(CODE);
        assertThat(used.expireAt()).isEqualTo(FUTURE);
        assertThat(used.used()).isTrue();
        assertThat(vc.used()).isFalse();
    }

    @Test
    @DisplayName("匹配未过期未使用验证码校验通过")
    void verify_matchingCodeNotExpired_returnsTrue() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertThat(vc.verify(new RandomString(CODE))).isTrue();
    }

    @Test
    @DisplayName("错误验证码校验不通过")
    void verify_wrongCode_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertThat(vc.verify(new RandomString("wrong"))).isFalse();
    }

    @Test
    @DisplayName("过期验证码校验不通过")
    void verify_expired_returnsFalse() {
        var vc = new VerificationCode(CODE, PAST, false);
        assertThat(vc.verify(new RandomString(CODE))).isFalse();
    }

    @Test
    @DisplayName("已使用验证码校验不通过")
    void verify_alreadyUsed_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, true);
        assertThat(vc.verify(new RandomString(CODE))).isFalse();
    }

    @Test
    @DisplayName("null 输入校验不通过且不抛异常")
    void verify_nullInput_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertThat(vc.verify(null)).isFalse();
    }
}
