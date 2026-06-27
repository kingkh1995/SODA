package com.soda.user.domain;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UserId 值对象")
class UserIdTest {

    private static final long VALID_ID = 1L;

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法 ID 创建实例")
        void should_create_when_validValue() {
            var id = new UserId(VALID_ID);
            assertThat(id.value()).isEqualTo(VALID_ID);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("0 值拒绝（minValue exclusive）")
        void should_throw_when_zero() {
            assertThatThrownBy(() -> new UserId(0L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1, -100})
        @DisplayName("负值拒绝")
        void should_throw_when_negative(long invalid) {
            assertThatThrownBy(() -> new UserId(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("解析")
    class Parse {
        @Test
        @DisplayName("有效字符串解析")
        void should_parse_when_validString() {
            assertThat(UserId.parse("1")).isEqualTo(new UserId(VALID_ID));
        }

        @Test
        @DisplayName("null 字符串拒绝")
        void should_throw_when_nullString() {
            assertThatThrownBy(() -> UserId.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("非数字字符串拒绝")
        void should_throw_when_notANumber() {
            assertThatThrownBy(() -> UserId.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new UserId(VALID_ID)).isEqualTo(new UserId(VALID_ID));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new UserId(1L)).isNotEqualTo(new UserId(2L));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new UserId(42L)).hasSameHashCodeAs(new UserId(42L));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_formatToString() {
            assertThat(new UserId(42L)).hasToString("UserId[value=42]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化与反序列化")
        void should_roundTrip() throws Exception {
            var original = new UserId(42L);
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, UserId.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为数字")
        void should_serializeAsNumber() throws Exception {
            assertThat(MAPPER.writeValueAsString(new UserId(42L))).isEqualTo("42");
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", UserId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 按 Long 顺序比较")
        void should_compareByLongOrder() {
            assertThat(new UserId(1L).compareTo(new UserId(2L)) < 0).isTrue();
            assertThat(new UserId(2L).compareTo(new UserId(1L)) > 0).isTrue();
            assertThat(new UserId(1L).compareTo(new UserId(1L)) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new UserId(42L);
            var b = new UserId(42L);
            assertThat(a.compareTo(b) == 0).isTrue();
            assertThat(a).isEqualTo(b);
        }
    }
}
