package com.soda.component.support.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailContent 值对象")
class EmailContentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        @DisplayName("主题最大长度 255 可创建")
        void should_create_when_subjectMaxLength() {
            var subj = "a".repeat(255);
            var c = new EmailContent(subj, "body");
            assertThat(c.subject()).hasSize(255);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 主题拒绝")
        void should_throw_when_subjectIsNull() {
            assertThatThrownBy(() -> new EmailContent(null, "body"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空主题拒绝")
        void should_throw_when_subjectIsEmpty() {
            assertThatThrownBy(() -> new EmailContent("", "body"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null 正文拒绝")
        void should_throw_when_bodyIsNull() {
            assertThatThrownBy(() -> new EmailContent("subject", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空正文拒绝")
        void should_throw_when_bodyIsEmpty() {
            assertThatThrownBy(() -> new EmailContent("subject", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("主题超过 255 字符拒绝")
        void should_throw_when_subjectTooLong() {
            assertThatThrownBy(() -> new EmailContent("a".repeat(256), "body"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValues() {
            assertThat(new EmailContent("a", "b")).isEqualTo(new EmailContent("a", "b"));
        }

        @Test
        @DisplayName("不同主题不等")
        void should_notBeEqual_when_differentSubject() {
            assertThat(new EmailContent("a", "b")).isNotEqualTo(new EmailContent("c", "b"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new EmailContent("a", "b")).hasSameHashCodeAs(new EmailContent("a", "b"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new EmailContent("a", "b")).hasToString("EmailContent[subject=a, body=b]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（JSON 对象格式）")
        void should_roundTrip() throws Exception {
            var original = new EmailContent("Welcome", "Thank you");
            JacksonTestUtil.assertRoundTrip(original, EmailContent.class);
        }

        @Test
        @DisplayName("非法 JSON 拒绝（空对象）")
        void should_throw_when_emptyJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", EmailContent.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }
}
