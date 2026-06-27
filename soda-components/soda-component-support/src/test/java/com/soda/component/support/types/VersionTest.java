package com.soda.component.support.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.component.support.testutil.JacksonTestUtil.assertRoundTrip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Version 值对象")
class VersionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("of(0) 创建 PRIMARY")
        void should_create_when_zero() {
            assertThat(Version.of(0)).isSameAs(Version.PRIMARY);
        }

        @Test
        @DisplayName("of(42) 创建实例")
        void should_create_when_validValue() {
            assertThat(Version.of(42).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("parse 创建实例")
        void should_create_when_parse() {
            assertThat(Version.parse("5")).isEqualTo(Version.of(5));
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("of(-1) 拒绝")
        void should_throw_when_negativeValue() {
            assertThatThrownBy(() -> Version.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse(null) 拒绝")
        void should_throw_when_parseNull() {
            assertThatThrownBy(() -> Version.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> Version.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("缓存")
    class Cache {

        @Test
        @DisplayName("of(0) 同 PRIMARY")
        void should_sameAsPrimary() {
            assertThat(Version.of(0)).isSameAs(Version.PRIMARY);
        }

        @Test
        @DisplayName("缓存范围内相同实例")
        void should_sameInstance_withinRange() {
            assertThat(Version.of(5)).isSameAs(Version.of(5));
        }

        @Test
        @DisplayName("缓存范围外不同实例")
        void should_differentInstance_beyondRange() {
            assertThat(Version.of(10000)).isNotSameAs(Version.of(10000));
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(Version.of(3)).isEqualTo(Version.of(3));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(Version.of(1)).isNotEqualTo(Version.of(2));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(Version.of(42)).hasToString("Version[value=42]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            assertRoundTrip(Version.of(42), Version.class);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", Version.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 按数值比较")
        void should_compareByNumericValue() {
            assertThat(Version.of(1).compareTo(Version.of(2)) < 0).isTrue();
            assertThat(Version.of(5).compareTo(Version.of(5)) == 0).isTrue();
            assertThat(Version.of(8).compareTo(Version.of(6)) > 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = Version.of(42);
            var same = Version.of(42);
            assertThat(a.compareTo(same) == 0).isTrue();
            assertThat(a).isEqualTo(same);
        }
    }
}
