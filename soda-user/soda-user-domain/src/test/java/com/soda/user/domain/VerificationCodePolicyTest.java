package com.soda.user.domain;

import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import com.soda.user.domain.types.VerificationCodePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

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
            var policy = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            assertThat(policy.codeLength()).isEqualTo(PositiveInt.of(6));
            assertThat(policy.expiry()).isEqualTo(Duration.ofMinutes(5));
            assertThat(policy.codeAlphabet()).isEqualTo(Alphabet.DIGITS);
        }

        @Test
        @DisplayName("自定义策略创建实例（嵌套 DP 组合）")
        void should_createViaFactory_when_customPolicy() {
            var policy = new VerificationCodePolicy(PositiveInt.of(4), Duration.ofMinutes(10), Alphabet.ALPHANUMERIC);
            assertThat(policy.codeLength()).isEqualTo(PositiveInt.of(4));
            assertThat(policy.expiry()).isEqualTo(Duration.ofMinutes(10));
            assertThat(policy.codeAlphabet()).isEqualTo(Alphabet.ALPHANUMERIC);
        }

        @Test
        @DisplayName("默认短信策略常量值正确")
        void should_haveCorrectDefaults_when_defaultSms() {
            assertThat(VerificationCodePolicy.DEFAULT_SMS.codeLength()).isEqualTo(PositiveInt.of(6));
            assertThat(VerificationCodePolicy.DEFAULT_SMS.expiry()).isEqualTo(Duration.ofMinutes(5));
            assertThat(VerificationCodePolicy.DEFAULT_SMS.codeAlphabet()).isEqualTo(Alphabet.DIGITS);
        }

        @Test
        @DisplayName("默认邮箱策略常量值正确")
        void should_haveCorrectDefaults_when_defaultEmail() {
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.codeLength()).isEqualTo(PositiveInt.of(8));
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.expiry()).isEqualTo(Duration.ofMinutes(30));
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.codeAlphabet()).isEqualTo(Alphabet.ALPHANUMERIC);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("codeLength 为 0 拒绝（PositiveInt 校验）")
        void should_throw_when_codeLengthIsZero() {
            assertThatThrownBy(() -> new VerificationCodePolicy(PositiveInt.of(0), Duration.ofMinutes(5), Alphabet.DIGITS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codeLength 为 21 拒绝（业务范围 ≤20）")
        void should_throw_when_codeLengthIsTwentyOne() {
            assertThatThrownBy(() -> new VerificationCodePolicy(PositiveInt.of(21), Duration.ofMinutes(5), Alphabet.DIGITS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("expiry 为 0 分钟拒绝")
        void should_throw_when_expiryIsZero() {
            assertThatThrownBy(() -> new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(0), Alphabet.DIGITS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("expiry 为负数拒绝")
        void should_throw_when_expiryIsNegative() {
            assertThatThrownBy(() -> new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(-1), Alphabet.DIGITS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null expiry 拒绝")
        void should_throw_when_expiryIsNull() {
            assertThatThrownBy(() -> new VerificationCodePolicy(PositiveInt.of(6), null, Alphabet.DIGITS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null codeLength 拒绝")
        void should_throw_when_codeLengthIsNull() {
            assertThatThrownBy(() -> new VerificationCodePolicy(null, Duration.ofMinutes(5), Alphabet.DIGITS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null codeAlphabet 拒绝")
        void should_throw_when_codeAlphabetIsNull() {
            assertThatThrownBy(() -> new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同参数相等")
        void should_beEqual_when_sameParams() {
            assertThat(new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS))
                    .isEqualTo(new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS));
        }

        @Test
        @DisplayName("不同参数不等")
        void should_notBeEqual_when_differentParams() {
            assertThat(new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS))
                    .isNotEqualTo(new VerificationCodePolicy(PositiveInt.of(8), Duration.ofMinutes(30), Alphabet.ALPHANUMERIC));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS))
                    .hasSameHashCodeAs(new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确，含所有字段")
        void should_haveCorrectToString() {
            assertThat(VerificationCodePolicy.DEFAULT_SMS).hasToString(
                    "VerificationCodePolicy[codeLength=PositiveInt[value=6], expiry=PT5M, codeAlphabet=Alphabet[value=0123456789]]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（嵌套 DP 序列化为基本类型值）")
        void should_roundTrip() throws Exception {
            var original = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("codeLength").contains("expiry").contains("codeAlphabet");
            assertThat(MAPPER.readValue(json, VerificationCodePolicy.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("DEFAULT_EMAIL 常量 round-trip（字母数字字符集）")
        void should_roundTrip_when_defaultEmail() throws Exception {
            var original = VerificationCodePolicy.DEFAULT_EMAIL;
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("\"codeLength\":8");
            assertThat(json).contains("\"codeAlphabet\":\"" + Alphabet.ALPHANUMERIC.value() + "\"");
            assertThat(MAPPER.readValue(json, VerificationCodePolicy.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("自定义策略 round-trip（非默认长度/有效期/字符集组合）")
        void should_roundTrip_when_customPolicy() throws Exception {
            var original = new VerificationCodePolicy(PositiveInt.of(4), Duration.ofMinutes(10), Alphabet.LETTERS);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("\"codeLength\":4");
            assertThat(json).contains("\"expiry\":\"PT10M\"");
            assertThat(json).contains("\"codeAlphabet\":\"" + Alphabet.LETTERS.value() + "\"");
            assertThat(MAPPER.readValue(json, VerificationCodePolicy.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为 JSON 对象（codeLength 为数字、codeAlphabet 为裸字符串）")
        void should_serializeToJsonObject() throws Exception {
            var original = VerificationCodePolicy.DEFAULT_SMS;
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("\"codeLength\":6");
            assertThat(json).contains("\"expiry\":\"PT5M\"");
            assertThat(json).contains("\"codeAlphabet\":\"0123456789\"");
        }

        @Test
        @DisplayName("从 JSON 对象反序列化")
        void should_deserializeFromJsonObject() throws Exception {
            var original = VerificationCodePolicy.DEFAULT_SMS;
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, VerificationCodePolicy.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝（空对象）")
        void should_throw_when_emptyJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", VerificationCodePolicy.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
