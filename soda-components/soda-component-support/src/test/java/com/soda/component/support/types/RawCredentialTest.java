package com.soda.component.support.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RawCredential 敏感值对象")
class RawCredentialTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var cred = new RawCredential("mySecret123");
            assertThat(cred.internalValue()).isEqualTo("mySecret123");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new RawCredential(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空字符串拒绝")
        void should_throw_when_valueIsEmpty() {
            assertThatThrownBy(() -> new RawCredential(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("不同实例不等（identity-based）")
        void should_notBeEqual_when_differentInstance() {
            assertThat(new RawCredential("x")).isNotEqualTo(new RawCredential("x"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 脱敏")
        void should_maskToString() {
            assertThat(new RawCredential("secret")).hasToString("RawCredential[***]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化拒绝")
        void should_throw_when_serialize() {
            assertThatThrownBy(() -> MAPPER.writeValueAsString(new RawCredential("s")))
                    .isInstanceOf(JsonProcessingException.class);
        }


        @Test
        @DisplayName("反序列化可恢复内部值")
        void should_preserveValue_when_deserialize() throws Exception {
            var restored = MAPPER.readValue("\"secret\"", RawCredential.class);
            assertThat(restored.internalValue()).isEqualTo("secret");
        }
    }
}
