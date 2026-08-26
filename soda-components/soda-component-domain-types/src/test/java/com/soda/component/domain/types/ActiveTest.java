package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Active 值对象")
class ActiveTest {

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
    @DisplayName("校验与异常")
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
    @DisplayName("相等性与 hashCode")
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
        void should_haveConsistentHashCode_when_sameValue() {
            assertThat(Active.TRUE).hasSameHashCodeAs(Active.of(true));
        }
    }

    @Nested
    @DisplayName("缓存")
    class Cache {

        @Test
        @DisplayName("of(true) 复用 TRUE 缓存实例")
        void should_returnCachedInstance_when_ofTrue() {
            assertThat(Active.TRUE).isSameAs(Active.of(true));
        }

        @Test
        @DisplayName("of(false) 复用 FALSE 缓存实例")
        void should_returnCachedInstance_when_ofFalse() {
            assertThat(Active.FALSE).isSameAs(Active.of(false));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() {
            var original = Active.TRUE;
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, Active.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为裸布尔")
        void should_serializeToBareBoolean() {
            var json = MAPPER.writeValueAsString(Active.TRUE);
            assertThat(json).isEqualTo("true");
        }

        @Test
        @DisplayName("从裸布尔反序列化")
        void should_deserializeFromBareBoolean() {
            assertThat(MAPPER.readValue("true", Active.class)).isEqualTo(Active.TRUE);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-boolean\"", Active.class))
                    .isInstanceOf(JacksonException.class);
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
}
