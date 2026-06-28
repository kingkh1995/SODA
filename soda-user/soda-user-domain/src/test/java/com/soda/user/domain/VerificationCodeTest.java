package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationCode 值对象")
class VerificationCodeTest {

    private static final String CODE = "123456";
    private static final Instant FUTURE = Instant.now().plus(1, ChronoUnit.HOURS);

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var vc = new VerificationCode(CODE, FUTURE, false);
            assertThat(vc.code()).isEqualTo(CODE);
            assertThat(vc.expireAt()).isEqualTo(FUTURE);
            assertThat(vc.used()).isFalse();
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null code 拒绝")
        void should_throw_when_codeIsNull() {
            assertThatThrownBy(() -> new VerificationCode(null, FUTURE, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空字符串 code 拒绝")
        void should_throw_when_codeIsBlank() {
            assertThatThrownBy(() -> new VerificationCode("", FUTURE, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null expireAt 拒绝")
        void should_throw_when_expireAtIsNull() {
            assertThatThrownBy(() -> new VerificationCode(CODE, null, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValues() {
            assertThat(new VerificationCode(CODE, FUTURE, false))
                    .isEqualTo(new VerificationCode(CODE, FUTURE, false));
        }

        @Test
        @DisplayName("不同 code 不等")
        void should_notBeEqual_when_differentCode() {
            assertThat(new VerificationCode("A", FUTURE, false))
                    .isNotEqualTo(new VerificationCode("B", FUTURE, false));
        }

        @Test
        @DisplayName("不同 used 标志不等")
        void should_notBeEqual_when_differentUsed() {
            assertThat(new VerificationCode(CODE, FUTURE, false))
                    .isNotEqualTo(new VerificationCode(CODE, FUTURE, true));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new VerificationCode(CODE, FUTURE, false))
                    .hasSameHashCodeAs(new VerificationCode(CODE, FUTURE, false));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 包含所有字段名和值")
        void should_containAllFields() {
            var vc = new VerificationCode(CODE, FUTURE, false);
            assertThat(vc.toString())
                    .contains("code=" + CODE)
                    .contains("used=false");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（JSON 对象格式）")
        void should_roundTrip() throws Exception {
            var original = new VerificationCode(CODE, FUTURE, false);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("code").contains("expireAt").contains("used");
            assertThat(MAPPER.readValue(json, VerificationCode.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝（空对象）")
        void should_throw_when_emptyJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", VerificationCode.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }
}
