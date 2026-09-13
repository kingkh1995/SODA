package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UUID 标识符值对象")
class UuidTest extends ComparableDomainPrimitiveContractTest<Uuid> {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Override
    protected Contract<Uuid> contract() {
        return new Contract<>(Uuid.class, () -> new Uuid(VALID_UUID), "\"" + VALID_UUID + "\"",
                "Uuid[value=" + VALID_UUID + "]", "12345",
                () -> new Uuid("650e8400-e29b-41d4-a716-446655440000"));
    }

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
        @DisplayName("Java UUID 格式创建实例")
        void should_create_when_javaUtilUuid() {
            var juid = java.util.UUID.randomUUID();
            assertThat(new Uuid(juid.toString()).value()).isEqualTo(juid.toString());
        }

        @Test
        @DisplayName("random() 生成规范小写 UUID")
        void should_generateCanonicalInstance_when_random() {
            assertThat(Uuid.random().value())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }

    @Nested
    @DisplayName("归一化")
    class Normalization {

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
        @DisplayName("不同大小写形式归一化后等值")
        void should_beEqual_when_caseDiffers() {
            assertThat(new Uuid("550E8400-E29B-41D4-A716-446655440000"))
                    .isEqualTo(new Uuid(VALID_UUID));
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
    @DisplayName("标识符")
    class Identity {

        @Test
        @DisplayName("identifier() 返回规范字符串值")
        void should_returnCanonicalString_when_identifier() {
            var id = new Uuid(VALID_UUID);
            assertThat(id.identifier()).isEqualTo(VALID_UUID);
        }
    }
}
