package com.soda.component.support.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static com.soda.component.support.testutil.JacksonTestUtil.assertRoundTrip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PositiveInt 值对象")
class PositiveIntTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(1) 创建 ONE")
        void should_create_when_one() {
            assertThat(PositiveInt.of(1)).isSameAs(PositiveInt.ONE);
        }

        @Test
        @DisplayName("of(1000) 创建大值")
        void should_create_when_largeValue() {
            assertThat(PositiveInt.of(1000).value()).isEqualTo(1000);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(PositiveInt.parse("6")).isEqualTo(PositiveInt.of(6));
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("of(0) 拒绝")
        void should_throw_when_zero() {
            assertThatThrownBy(() -> PositiveInt.of(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(-1) 拒绝")
        void should_throw_when_negative() {
            assertThatThrownBy(() -> PositiveInt.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> PositiveInt.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> PositiveInt.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("缓存")
    class Cache {

        @Test
        @DisplayName("缓存范围内相同实例")
        void should_sameInstance_withinRange() {
            assertThat(PositiveInt.of(5)).isSameAs(PositiveInt.of(5));
        }

        @Test
        @DisplayName("缓存上限相同实例")
        void should_sameInstance_atUpperBound() {
            assertThat(PositiveInt.of(100)).isSameAs(PositiveInt.of(100));
        }

        @Test
        @DisplayName("缓存范围外不同实例")
        void should_differentInstance_beyondRange() {
            assertThat(PositiveInt.of(1000)).isNotSameAs(PositiveInt.of(1000));
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(PositiveInt.of(6)).isEqualTo(PositiveInt.of(6));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(PositiveInt.of(6)).hasSameHashCodeAs(PositiveInt.of(6));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(PositiveInt.of(42)).hasToString("PositiveInt[value=42]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            assertRoundTrip(PositiveInt.of(6), PositiveInt.class);
        }

        @Test
        @DisplayName("序列化为裸数字")
        void should_serializeToBareNumber() throws Exception {
            var json = MAPPER.writeValueAsString(PositiveInt.of(42));
            assertThat(json).isEqualTo("42");
        }

        @Test
        @DisplayName("从裸数字反序列化")
        void should_deserializeFromBareNumber() throws Exception {
            assertThat(MAPPER.readValue("42", PositiveInt.class)).isEqualTo(PositiveInt.of(42));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", PositiveInt.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 按数值比较")
        void should_compareByNumericValue() {
            assertThat(PositiveInt.of(4).compareTo(PositiveInt.of(6)) < 0).isTrue();
            assertThat(PositiveInt.of(6).compareTo(PositiveInt.of(6)) == 0).isTrue();
            assertThat(PositiveInt.of(8).compareTo(PositiveInt.of(6)) > 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = PositiveInt.of(42);
            var same = PositiveInt.of(42);
            assertThat(a.compareTo(same) == 0).isTrue();
            assertThat(a).isEqualTo(same);
        }
    }
}
