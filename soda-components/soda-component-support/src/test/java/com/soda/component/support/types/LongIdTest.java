package com.soda.component.support.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.component.support.testutil.JacksonTestUtil.assertRoundTrip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LongId 值对象")
class LongIdTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            assertThat(new LongId(42).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(LongId.parse("99")).isEqualTo(new LongId(99));
        }

    }

    @Nested
    @DisplayName("校验")
    class Validation {


        @Test
        @DisplayName("0 拒绝（minValue exclusive）")
        void should_throw_when_valueIsZero() {
            assertThatThrownBy(() -> new LongId(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("负值拒绝")
        void should_throw_when_negativeValue() {
            assertThatThrownBy(() -> new LongId(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> LongId.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> LongId.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new LongId(42)).isEqualTo(new LongId(42));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new LongId(1)).isNotEqualTo(new LongId(2));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new LongId(42)).hasSameHashCodeAs(new LongId(42));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new LongId(42)).hasToString("LongId[value=42]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            assertRoundTrip(new LongId(42), LongId.class);
        }

        @Test
        @DisplayName("序列化为裸数字")
        void should_serializeAsNumber() throws Exception {
            assertThat(MAPPER.writeValueAsString(new LongId(42))).isEqualTo("42");
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", LongId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 按数值比较")
        void should_compareByNumericValue() {
            assertThat(new LongId(1).compareTo(new LongId(2)) < 0).isTrue();
            assertThat(new LongId(2).compareTo(new LongId(2)) == 0).isTrue();
            assertThat(new LongId(3).compareTo(new LongId(2)) > 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new LongId(42);
            var same = new LongId(42);
            assertThat(a.compareTo(same) == 0).isTrue();
            assertThat(a).isEqualTo(same);
        }
    }
}
