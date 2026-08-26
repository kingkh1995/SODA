package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UUID 标识符值对象")
class UuidTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 UUID 创建实例")
        void should_create_when_validUuid() {
            var id = new Uuid(VALID_UUID);
            assertThat(id.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("大写归一化为小写")
        void should_normalizeToLowercase_when_uppercase() {
            var id = new Uuid("550E8400-E29B-41D4-A716-446655440000");
            assertThat(id.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("大小写混合归一化为小写")
        void should_normalizeToLowercase_when_mixedCase() {
            var id = new Uuid("550e8400-e29b-41D4-A716-446655440000");
            assertThat(id.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Java UUID 格式创建实例")
        void should_create_when_javaUtilUuid() {
            var juid = java.util.UUID.randomUUID();
            assertThat(new Uuid(juid.toString()).value()).isEqualTo(juid.toString());
        }

        @Test
        @DisplayName("random() 生成合法实例")
        void should_generateValidInstance_when_random() {
            var id = Uuid.random();
            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("固定不同值不等")
        void should_notBeEqual_when_distinctFixedValues() {
            assertThat(new Uuid(VALID_UUID))
                    .isNotEqualTo(new Uuid("00000000-0000-0000-0000-000000000001"));
        }

        @Test
        @DisplayName("固定同值相等")
        void should_beEqual_when_sameFixedValue() {
            var fixed = "11111111-2222-3333-4444-555555555555";
            assertThat(new Uuid(fixed)).isEqualTo(new Uuid(fixed));
        }

        @Test
        @DisplayName("identifier() 返回字符串值")
        void should_returnString_when_identifier() {
            var id = new Uuid(VALID_UUID);
            assertThat(id.identifier()).isEqualTo(VALID_UUID);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new Uuid(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串拒绝")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new Uuid("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Uuid("  " + VALID_UUID + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("缺少连字符拒绝")
        void should_throw_when_noHyphens() {
            assertThatThrownBy(() -> new Uuid("550e8400e29b41d4a716446655440000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("分段长度错误拒绝")
        void should_throw_when_wrongSectionLength() {
            assertThatThrownBy(() -> new Uuid("550e8400-e29b-41d4-a716-44665544000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("非法十六进制拒绝")
        void should_throw_when_invalidHex() {
            assertThatThrownBy(() -> new Uuid("550e8400-e29b-41d4-a716-44665544000g"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Uuid(VALID_UUID)).isEqualTo(new Uuid(VALID_UUID));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Uuid(VALID_UUID)).isNotEqualTo(Uuid.random());
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new Uuid(VALID_UUID);
            var b = new Uuid(VALID_UUID);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new Uuid(VALID_UUID);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, Uuid.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeAsBareString() throws Exception {
            var json = MAPPER.writeValueAsString(new Uuid(VALID_UUID));
            assertThat(json).isEqualTo("\"" + VALID_UUID + "\"");
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("12345", Uuid.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 委托给字符串比较")
        void should_orderByStringCompare() {
            var a = new Uuid("00000000-0000-0000-0000-000000000001");
            var b = new Uuid("00000000-0000-0000-0000-000000000002");
            assertThat(a.compareTo(b) < 0).isTrue();
            assertThat(b.compareTo(a) > 0).isTrue();
            assertThat(a.compareTo(a) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new Uuid("a3bb4e8c-8f3c-4e73-8e80-5b4de7f25fc8");
            var b = new Uuid("a3bb4e8c-8f3c-4e73-8e80-5b4de7f25fc8");
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
            assertThat(new Uuid(VALID_UUID)).hasToString("Uuid[value=550e8400-e29b-41d4-a716-446655440000]");
        }
    }
}
