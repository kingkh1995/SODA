package com.soda.user.domain;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import com.soda.component.domain.types.RandomString;
import com.soda.user.domain.types.VerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("验证码值对象")
class VerificationCodeTest extends DomainPrimitiveContractTest<VerificationCode> {

    private static final RandomString CODE = new RandomString("123456");
    private static final Instant EXPIRE_AT = Instant.parse("2026-08-09T12:00:00Z");

    @Override
    protected Contract<VerificationCode> contract() {
        return new Contract<>(VerificationCode.class, () -> VerificationCode.from(CODE, Instant.EPOCH),
                "{\"code\":\"123456\",\"expireAt\":\"1970-01-01T00:00:00Z\"}",
                "VerificationCode[code=123456, expireAt=1970-01-01T00:00:00Z]", "{}",
                () -> VerificationCode.from(new RandomString("654321"), Instant.EPOCH));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from 工厂直存 RandomString 并解包为 String")
        void should_createViaFactory_when_validValue() {
            var code = VerificationCode.from(CODE, EXPIRE_AT);
            assertThat(code.code()).isEqualTo(CODE.value());
            assertThat(code.expireAt()).isEqualTo(EXPIRE_AT);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {
        @Test
        @DisplayName("null code 拒绝")
        void should_throw_when_codeIsNull() {
            assertThatThrownBy(() -> new VerificationCode(null, EXPIRE_AT))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白 code 拒绝")
        void should_throw_when_codeIsBlank() {
            assertThatThrownBy(() -> new VerificationCode("  ", EXPIRE_AT))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null expireAt 拒绝")
        void should_throw_when_expireAtIsNull() {
            assertThatThrownBy(() -> new VerificationCode(CODE.value(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("业务方法")
    class RichMethods {
        @Test
        @DisplayName("匹配返回 true")
        void should_match_when_sameCode() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT).matches(CODE)).isTrue();
        }

        @Test
        @DisplayName("不匹配返回 false")
        void should_notMatch_when_differentCode() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT).matches(new RandomString("654321"))).isFalse();
        }

        @Test
        @DisplayName("过期前未过期")
        void should_notExpired_when_beforeExpireAt() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT).expiredAt(EXPIRE_AT.minusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("恰好 expireAt 时刻未过期（严格 after 判定），+1ns 过期")
        void should_notExpired_when_atExpireAt() {
            var code = VerificationCode.from(CODE, EXPIRE_AT);
            assertThat(code.expiredAt(EXPIRE_AT)).isFalse();
            assertThat(code.expiredAt(EXPIRE_AT.plusNanos(1))).isTrue();
        }

        @Test
        @DisplayName("过期后已过期")
        void should_expired_when_afterExpireAt() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT).expiredAt(EXPIRE_AT.plusSeconds(1))).isTrue();
        }
    }
}
