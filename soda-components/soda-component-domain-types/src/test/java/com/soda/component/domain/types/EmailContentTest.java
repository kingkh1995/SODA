package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("EmailContent 值对象")
class EmailContentTest extends DomainPrimitiveContractTest<EmailContent> {

    @Override
    protected Contract<EmailContent> contract() {
        return new Contract<>(EmailContent.class, () -> new EmailContent("a", "b"),
                "{\"subject\":\"a\",\"body\":\"b\"}", "EmailContent[subject=a, body=b]",
                "{}", () -> new EmailContent("c", "b"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法主题和正文创建实例")
        void should_create_when_validValue() {
            var c = new EmailContent("Welcome", "Thank you for registering");
            assertThat(c.subject()).isEqualTo("Welcome");
            assertThat(c.body()).isEqualTo("Thank you for registering");
        }

        @Test
        @DisplayName("主题最大长度可创建")
        void should_create_when_subjectMaxLength() {
            var subj = "a".repeat(EmailContent.SUBJECT_MAX_LENGTH);
            var c = new EmailContent(subj, "body");
            assertThat(c.subject()).hasSize(EmailContent.SUBJECT_MAX_LENGTH);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        static Stream<Arguments> missingFields() {
            return Stream.of(
                    arguments(null, "body"),
                    arguments("", "body"),
                    arguments("   ", "body"),
                    arguments("subject", null),
                    arguments("subject", ""),
                    arguments("subject", "   "));
        }

        @ParameterizedTest(name = "[{index}] subject={0}, body={1}")
        @MethodSource("missingFields")
        @DisplayName("主题或正文为 null / 空 / 空白拒绝")
        void should_throw_when_fieldMissing(String subject, String body) {
            assertThatThrownBy(() -> new EmailContent(subject, body))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("主题超过最大长度拒绝")
        void should_throw_when_subjectTooLong() {
            assertThatThrownBy(() -> new EmailContent("a".repeat(EmailContent.SUBJECT_MAX_LENGTH + 1), "body"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
