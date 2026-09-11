package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConcurrencyVersion 值对象")
class ConcurrencyVersionTest {

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(0) 创建 INITIAL")
        void should_create_when_zero() {
            assertThat(ConcurrencyVersion.of(0)).isSameAs(ConcurrencyVersion.INITIAL);
        }

        @Test
        @DisplayName("of(42) 创建实例")
        void should_create_when_validValue() {
            assertThat(ConcurrencyVersion.of(42).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(ConcurrencyVersion.parse("5")).isEqualTo(ConcurrencyVersion.of(5));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("of(-1) 拒绝")
        void should_throw_when_negativeValue() {
            assertThatThrownBy(() -> ConcurrencyVersion.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> ConcurrencyVersion.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(null) 拒绝（缺失版本不可表示，放行语义归调用方）")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> ConcurrencyVersion.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> ConcurrencyVersion.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(ConcurrencyVersion.of(3)).isEqualTo(ConcurrencyVersion.of(3));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(ConcurrencyVersion.of(1)).isNotEqualTo(ConcurrencyVersion.of(2));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(ConcurrencyVersion.of(3)).hasSameHashCodeAs(ConcurrencyVersion.of(3));
        }
    }

    @Nested
    @DisplayName("缓存")
    class Cache {

        @Test
        @DisplayName("of(0) 同 INITIAL")
        void should_shareInitial_when_zero() {
            assertThat(ConcurrencyVersion.of(0)).isSameAs(ConcurrencyVersion.INITIAL);
        }

        @Test
        @DisplayName("缓存范围内相同实例")
        void should_beSameInstance_when_withinRange() {
            assertThat(ConcurrencyVersion.of(5)).isSameAs(ConcurrencyVersion.of(5));
        }

        @Test
        @DisplayName("缓存范围外不同实例")
        void should_beDifferentInstance_when_beyondRange() {
            assertThat(ConcurrencyVersion.of(10000)).isNotSameAs(ConcurrencyVersion.of(10000));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = ConcurrencyVersion.of(42);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, ConcurrencyVersion.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为裸数字")
        void should_serializeToBareNumber() throws Exception {
            var json = MAPPER.writeValueAsString(ConcurrencyVersion.of(42));
            assertThat(json).isEqualTo("42");
        }

        @Test
        @DisplayName("从裸数字反序列化")
        void should_deserializeFromBareNumber() throws Exception {
            assertThat(MAPPER.readValue("42", ConcurrencyVersion.class)).isEqualTo(ConcurrencyVersion.of(42));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", ConcurrencyVersion.class))
                    .isInstanceOf(JacksonException.class);
        }

    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 按数值比较")
        void should_compareByNumericValue() {
            assertThat(ConcurrencyVersion.of(1).compareTo(ConcurrencyVersion.of(2)) < 0).isTrue();
            assertThat(ConcurrencyVersion.of(5).compareTo(ConcurrencyVersion.of(5)) == 0).isTrue();
            assertThat(ConcurrencyVersion.of(8).compareTo(ConcurrencyVersion.of(6)) > 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = ConcurrencyVersion.of(42);
            var same = ConcurrencyVersion.of(42);
            assertThat(a.compareTo(same) == 0).isTrue();
            assertThat(a).isEqualTo(same);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(ConcurrencyVersion.of(42)).hasToString("ConcurrencyVersion[value=42]");
        }
    }
}
