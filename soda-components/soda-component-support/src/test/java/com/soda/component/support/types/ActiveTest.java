package com.soda.component.support.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Active 值对象")
class ActiveTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(true) 返回 TRUE")
        void should_returnTrue_when_ofTrue() {
            assertThat(Active.of(true)).isSameAs(Active.TRUE);
        }

        @Test
        @DisplayName("of(false) 返回 FALSE")
        void should_returnFalse_when_ofFalse() {
            assertThat(Active.of(false)).isSameAs(Active.FALSE);
        }

        @Test
        @DisplayName("parse(\"true\") 返回 TRUE")
        void should_returnTrue_when_parseTrue() {
            assertThat(Active.parse("true")).isSameAs(Active.TRUE);
        }

        @Test
        @DisplayName("parse(\"false\") 返回 FALSE")
        void should_returnFalse_when_parseFalse() {
            assertThat(Active.parse("false")).isSameAs(Active.FALSE);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> Active.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> Active.parse("not-a-boolean"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("缓存")
    class Cache {

        @Test
        @DisplayName("TRUE 与 of(true) 相同")
        void should_sameTrue() {
            assertThat(Active.TRUE).isSameAs(Active.of(true));
        }

        @Test
        @DisplayName("FALSE 与 of(false) 相同")
        void should_sameFalse() {
            assertThat(Active.FALSE).isSameAs(Active.of(false));
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(Active.TRUE).isEqualTo(Active.of(true));
            assertThat(Active.FALSE).isEqualTo(Active.of(false));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(Active.TRUE).isNotEqualTo(Active.FALSE);
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(Active.TRUE).hasSameHashCodeAs(Active.of(true));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(Active.TRUE).hasToString("Active[value=true]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var json = MAPPER.writeValueAsString(Active.TRUE);
            assertThat(json).isEqualTo("true");
            assertThat(MAPPER.readValue(json, Active.class)).isEqualTo(Active.TRUE);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-boolean\"", Active.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }
}
