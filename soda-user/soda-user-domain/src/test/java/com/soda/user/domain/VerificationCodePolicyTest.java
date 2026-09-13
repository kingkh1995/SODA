package com.soda.user.domain;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import com.soda.user.domain.types.VerificationCodePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;

@DisplayName("VerificationCodePolicy 值对象")
class VerificationCodePolicyTest extends DomainPrimitiveContractTest<VerificationCodePolicy> {

    private static final Duration MINUTES_5 = Duration.ofMinutes(5);

    @Override
    protected Contract<VerificationCodePolicy> contract() {
        return new Contract<>(VerificationCodePolicy.class, () -> VerificationCodePolicy.DEFAULT_SMS,
                "{\"codeLength\":6,\"expiry\":\"PT5M\",\"codeAlphabet\":\"0123456789\"}",
                "VerificationCodePolicy[codeLength=PositiveInt[value=6], expiry=PT5M, codeAlphabet=Alphabet[value=0123456789]]",
                "{}", () -> VerificationCodePolicy.DEFAULT_EMAIL);
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var policy = new VerificationCodePolicy(PositiveInt.of(6), MINUTES_5, Alphabet.DIGITS);
            assertThat(policy.codeLength()).isEqualTo(PositiveInt.of(6));
            assertThat(policy.expiry()).isEqualTo(MINUTES_5);
            assertThat(policy.codeAlphabet()).isEqualTo(Alphabet.DIGITS);
        }

        @Test
        @DisplayName("自定义策略创建实例（非默认长度/有效期/字符集）")
        void should_create_when_customPolicy() {
            var policy = new VerificationCodePolicy(PositiveInt.of(4), Duration.ofMinutes(10), Alphabet.UNAMBIGUOUS_ALPHANUMERIC);
            assertThat(policy.codeLength()).isEqualTo(PositiveInt.of(4));
            assertThat(policy.expiry()).isEqualTo(Duration.ofMinutes(10));
            assertThat(policy.codeAlphabet()).isEqualTo(Alphabet.UNAMBIGUOUS_ALPHANUMERIC);
        }

        @Test
        @DisplayName("默认短信策略常量值正确")
        void should_haveCorrectDefaults_when_defaultSms() {
            assertThat(VerificationCodePolicy.DEFAULT_SMS.codeLength()).isEqualTo(PositiveInt.of(6));
            assertThat(VerificationCodePolicy.DEFAULT_SMS.expiry()).isEqualTo(MINUTES_5);
            assertThat(VerificationCodePolicy.DEFAULT_SMS.codeAlphabet()).isEqualTo(Alphabet.DIGITS);
        }

        @Test
        @DisplayName("默认邮箱策略常量值正确")
        void should_haveCorrectDefaults_when_defaultEmail() {
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.codeLength()).isEqualTo(PositiveInt.of(8));
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.expiry()).isEqualTo(Duration.ofMinutes(30));
            assertThat(VerificationCodePolicy.DEFAULT_EMAIL.codeAlphabet()).isEqualTo(Alphabet.UNAMBIGUOUS_ALPHANUMERIC);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidPolicies")
        @DisplayName("非法策略拒绝（长度范围 / 有效期 / null 字段）")
        void should_throw_when_invalidPolicy(String scenario, ThrowingCallable construction) {
            assertThatThrownBy(construction)
                    .isInstanceOf(IllegalArgumentException.class);
        }

        static Stream<Arguments> invalidPolicies() {
            return Stream.of(
                    Arguments.of("codeLength=0（PositiveInt 拒绝）", (ThrowingCallable) () ->
                            new VerificationCodePolicy(PositiveInt.of(0), MINUTES_5, Alphabet.DIGITS)),
                    Arguments.of("codeLength=21（业务范围 ≤20）", (ThrowingCallable) () ->
                            new VerificationCodePolicy(PositiveInt.of(21), MINUTES_5, Alphabet.DIGITS)),
                    Arguments.of("expiry=0 分钟", (ThrowingCallable) () ->
                            new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(0), Alphabet.DIGITS)),
                    Arguments.of("expiry 负数", (ThrowingCallable) () ->
                            new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(-1), Alphabet.DIGITS)),
                    Arguments.of("null expiry", (ThrowingCallable) () ->
                            new VerificationCodePolicy(PositiveInt.of(6), null, Alphabet.DIGITS)),
                    Arguments.of("null codeLength", (ThrowingCallable) () ->
                            new VerificationCodePolicy(null, MINUTES_5, Alphabet.DIGITS)),
                    Arguments.of("null codeAlphabet", (ThrowingCallable) () ->
                            new VerificationCodePolicy(PositiveInt.of(6), MINUTES_5, null)));
        }
    }
}
