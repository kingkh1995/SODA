package com.soda.component.support.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UUID 标识符值对象")
class UUIdTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 UUID 创建实例")
        void should_create_when_validUuid() {
            var id = new UUId(VALID_UUID);
            assertThat(id.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("大写归一化为小写")
        void should_normalizeToLowercase_when_uppercase() {
            var id = new UUId("550E8400-E29B-41D4-A716-446655440000");
            assertThat(id.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("大小写混合归一化为小写")
        void should_normalizeToLowercase_when_mixedCase() {
            var id = new UUId("550e8400-e29b-41D4-A716-446655440000");
            assertThat(id.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Java UUID 格式创建实例")
        void should_create_when_javaUtilUuid() {
            var juid = java.util.UUID.randomUUID();
            assertThat(new UUId(juid.toString()).value()).isEqualTo(juid.toString());
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new UUId(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串拒绝")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new UUId("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new UUId("  " + VALID_UUID + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("缺少连字符拒绝")
        void should_throw_when_noHyphens() {
            assertThatThrownBy(() -> new UUId("550e8400e29b41d4a716446655440000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("分段长度错误拒绝")
        void should_throw_when_wrongSectionLength() {
            assertThatThrownBy(() -> new UUId("550e8400-e29b-41d4-a716-44665544000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("非法十六进制拒绝")
        void should_throw_when_invalidHex() {
            assertThatThrownBy(() -> new UUId("550e8400-e29b-41d4-a716-44665544000g"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new UUId(VALID_UUID)).isEqualTo(new UUId(VALID_UUID));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new UUId(VALID_UUID)).isNotEqualTo(UUId.random());
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new UUId(VALID_UUID);
            var b = new UUId(VALID_UUID);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 委托给字符串比较")
        void should_orderByStringCompare() {
            var a = new UUId("00000000-0000-0000-0000-000000000001");
            var b = new UUId("00000000-0000-0000-0000-000000000002");
            assertThat(a.compareTo(b) < 0).isTrue();
            assertThat(b.compareTo(a) > 0).isTrue();
            assertThat(a.compareTo(a) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new UUId("a3bb4e8c-8f3c-4e73-8e80-5b4de7f25fc8");
            var b = new UUId("a3bb4e8c-8f3c-4e73-8e80-5b4de7f25fc8");
            assertThat(a.compareTo(b) == 0).isTrue();
            assertThat(a).isEqualTo(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new UUId(VALID_UUID)).hasToString("UUId[value=550e8400-e29b-41d4-a716-446655440000]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new UUId(VALID_UUID);
            JacksonTestUtil.assertRoundTrip(original, UUId.class);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeAsBareString() throws Exception {
            var json = MAPPER.writeValueAsString(new UUId(VALID_UUID));
            assertThat(json).isEqualTo("\"" + VALID_UUID + "\"");
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("12345", UUId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    // ——— 业务方法 ———

    @Test
    @DisplayName("random() 生成合法实例")
    void should_generateValidInstance_when_random() {
        var id = UUId.random();
        assertThat(id).isNotNull();
        assertThatCode(() -> new UUId(id.value())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("random() 生成不同值")
    void should_generateDistinctValues_when_random() {
        assertThat(UUId.random()).isNotEqualTo(UUId.random());
    }

    @Test
    @DisplayName("identifier() 返回字符串值")
    void should_returnString_when_identifier() {
        var id = new UUId(VALID_UUID);
        assertThat(id.identifier()).isEqualTo(VALID_UUID);
    }
}
