package com.soda.user.domain;

import com.soda.component.domain.types.RandomString;
import com.soda.user.domain.types.VerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.time.Instant;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("验证码值对象")
class VerificationCodeTest {

    private static final RandomString CODE = new RandomString("123456");
    private static final Instant EXPIRE_AT = Instant.parse("2026-08-09T12:00:00Z");

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

        @Test
        @DisplayName("null code 拒绝")
        void should_throw_when_codeIsNull() {
            assertThatThrownBy(() -> new VerificationCode(null, EXPIRE_AT))
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
    @DisplayName("匹配与过期")
    class Matching {
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

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同参数相等")
        void should_beEqual_when_sameParams() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT)).isEqualTo(VerificationCode.from(CODE, EXPIRE_AT));
        }

        @Test
        @DisplayName("不同参数不等")
        void should_notBeEqual_when_differentParams() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT))
                    .isNotEqualTo(VerificationCode.from(new RandomString("654321"), EXPIRE_AT));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(VerificationCode.from(CODE, EXPIRE_AT))
                    .hasSameHashCodeAs(VerificationCode.from(CODE, EXPIRE_AT));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确，含所有字段")
        void should_haveCorrectToString() {
            assertThat(VerificationCode.from(new RandomString("123456"), Instant.EPOCH))
                    .hasToString("VerificationCode[code=123456, expireAt=1970-01-01T00:00:00Z]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（code 序列化为字符串）")
        void should_roundTrip() throws Exception {
            var original = VerificationCode.from(CODE, EXPIRE_AT);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("code").contains("expireAt");
            assertThat(MAPPER.readValue(json, VerificationCode.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为 JSON 对象（code 为裸字符串）")
        void should_serializeToJsonObject() throws Exception {
            var original = VerificationCode.from(CODE, EXPIRE_AT);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("\"code\":\"123456\"");
            assertThat(json).contains("\"expireAt\":\"2026-08-09T12:00:00Z\"");
        }

        @Test
        @DisplayName("从 JSON 对象反序列化")
        void should_deserializeFromJsonObject() throws Exception {
            var original = VerificationCode.from(CODE, EXPIRE_AT);
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, VerificationCode.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝（空对象）")
        void should_throw_when_emptyJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", VerificationCode.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
