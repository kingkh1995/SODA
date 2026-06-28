package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationCodePolicy 值对象")
class VerificationCodePolicyTest {

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var policy = new VerificationCodePolicy(6, Duration.ofMinutes(5));
            assertThat(policy.codeLength()).isEqualTo(6);
            assertThat(policy.expiry()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("默认短信策略常量值正确")
        void should_haveCorrectDefaults_when_defaultSms() {
            assertThat(VerificationCodePolicy.DEFAULT_SMS.codeLength()).isEqualTo(6);
            assertThat(VerificationCodePolicy.DEFAULT_SMS.expiry()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("默认邮箱策略常量值正确")
        void should_haveCorrectDefaults_when_defaultEmail() {
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.codeLength()).isEqualTo(8);
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.expiry()).isEqualTo(Duration.ofMinutes(30));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("codeLength 为 0 拒绝")
        void should_throw_when_codeLengthIsZero() {
            assertThatThrownBy(() -> new VerificationCodePolicy(0, Duration.ofMinutes(5)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codeLength 为 21 拒绝")
        void should_throw_when_codeLengthIsTwentyOne() {
            assertThatThrownBy(() -> new VerificationCodePolicy(21, Duration.ofMinutes(5)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("expiry 为 0 分钟拒绝")
        void should_throw_when_expiryIsZero() {
            assertThatThrownBy(() -> new VerificationCodePolicy(6, Duration.ofMinutes(0)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("expiry 为负数拒绝")
        void should_throw_when_expiryIsNegative() {
            assertThatThrownBy(() -> new VerificationCodePolicy(6, Duration.ofMinutes(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null expiry 拒绝")
        void should_throw_when_expiryIsNull() {
            assertThatThrownBy(() -> new VerificationCodePolicy(6, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同参数相等")
        void should_beEqual_when_sameParams() {
            assertThat(new VerificationCodePolicy(6, Duration.ofMinutes(5)))
                    .isEqualTo(new VerificationCodePolicy(6, Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("不同参数不等")
        void should_notBeEqual_when_differentParams() {
            assertThat(new VerificationCodePolicy(6, Duration.ofMinutes(5)))
                    .isNotEqualTo(new VerificationCodePolicy(8, Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new VerificationCodePolicy(6, Duration.ofMinutes(5)))
                    .hasSameHashCodeAs(new VerificationCodePolicy(6, Duration.ofMinutes(5)));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确，含所有字段")
        void should_haveCorrectToString() {
            assertThat(VerificationCodePolicy.DEFAULT_SMS)
                    .hasToString("VerificationCodePolicy[codeLength=6, expiry=PT5M]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（JSON 对象格式）")
        void should_roundTrip() throws Exception {
            var original = new VerificationCodePolicy(6, Duration.ofMinutes(5));
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("codeLength").contains("expiry");
            assertThat(MAPPER.readValue(json, VerificationCodePolicy.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝（空对象）")
        void should_throw_when_emptyJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", VerificationCodePolicy.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }
}
