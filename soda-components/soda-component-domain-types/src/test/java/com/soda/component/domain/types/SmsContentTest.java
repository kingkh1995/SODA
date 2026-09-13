package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("短信内容值对象")
class SmsContentTest extends DomainPrimitiveContractTest<SmsContent> {

    @Override
    protected Contract<SmsContent> contract() {
        return new Contract<>(SmsContent.class, () -> new SmsContent("hello"), "\"hello\"",
                "SmsContent[value=hello]", "{}", () -> new SmsContent("world"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法内容创建实例")
        void should_create_when_validContent() {
            var content = new SmsContent("您的验证码是123456");
            assertThat(content.value()).isEqualTo("您的验证码是123456");
        }

        @Test
        @DisplayName("最大长度创建实例")
        void should_create_when_maxLength() {
            var content = new SmsContent("a".repeat(SmsContent.MAX_LENGTH));
            assertThat(content.value().length()).isEqualTo(SmsContent.MAX_LENGTH);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new SmsContent(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串拒绝")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new SmsContent("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超长字符串拒绝")
        void should_throw_when_tooLong() {
            assertThatThrownBy(() -> new SmsContent("a".repeat(SmsContent.MAX_LENGTH + 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
