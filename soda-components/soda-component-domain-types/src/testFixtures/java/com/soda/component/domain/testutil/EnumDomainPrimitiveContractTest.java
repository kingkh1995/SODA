package com.soda.component.domain.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 枚举 DP 契约基类 —— 只吸收「序列化」组（dp-test-conventions §1.5）。
 * <p>
 * 枚举无 {@code equals} 语义（身份即等值）、{@code toString()} = {@code name()} 已由「显示」组覆盖，
 * 故不取「相等性」「调试」组；「查找」「显示」仍留在子类。
 * <p>
 * 形状 / round-trip 逐 {@code values()} 常量断言（枚举字面量全集），比单样例更强且不随常量增删漂移。
 *
 * @param <E> 被测枚举 DP 类型
 */
public abstract class EnumDomainPrimitiveContractTest<E extends Enum<E>> {

    /**
     * 枚举契约样例。
     *
     * @param type        反序列化声明类型
     * @param invalidJson 必须被拒绝的非法枚举名 JSON
     */
    public record EnumContract<E extends Enum<E>>(Class<E> type, String invalidJson) {
    }

    /**
     * 被测枚举的契约样例声明。
     */
    protected abstract EnumContract<E> contract();

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("序列化形状为 name 裸串（逐常量）")
        void should_haveBareNameShape() {
            for (E constant : contract().type().getEnumConstants()) {
                assertThat(MAPPER.writeValueAsString(constant))
                        .isEqualTo("\"" + constant.name() + "\"");
            }
        }

        @Test
        @DisplayName("round-trip 一致（逐常量）")
        void should_roundTrip() {
            for (E constant : contract().type().getEnumConstants()) {
                assertThat(MAPPER.readValue(MAPPER.writeValueAsString(constant), contract().type()))
                        .isEqualTo(constant);
            }
        }

        @Test
        @DisplayName("非法枚举名拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue(contract().invalidJson(), contract().type()))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
