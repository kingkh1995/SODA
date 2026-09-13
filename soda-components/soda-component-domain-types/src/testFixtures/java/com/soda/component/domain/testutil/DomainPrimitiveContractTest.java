package com.soda.component.domain.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.util.function.Supplier;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DP 契约基类 —— 吸收「序列化 / 调试 / 相等性」三个必测分组（见
 * {@code docs/conventions/dp-test-conventions.md} §1.1、§2）。
 * <p>
 * 子类只需声明 {@link Contract} 样例（一行），组名与断言形态由本类统一承载；
 * 类型专属分组（构造 / 校验 / 转换 / 路由 …）仍留在子类，不受本契约约束。
 * <p>
 * <b>共享 Mapper</b>：断言一律经组件模块的 {@link JacksonTestUtil#MAPPER}（全库两个 Mapper 定义之一），
 * 下游模块不得另起一套序列化口径。
 * <p>
 * 仅 {@code Comparable} DP 追加「比较」组 —— 见 {@link ComparableDomainPrimitiveContractTest}；
 * 枚举 DP 只取「序列化」组 —— 见 {@link EnumDomainPrimitiveContractTest}。
 *
 * @param <T> 被测 DP 类型
 */
public abstract class DomainPrimitiveContractTest<T> {

    /**
     * 契约样例。子类在 {@link #contract()} 中一行构造。
     * <p>
     * 形状冻结：六元组不再加字段 / 钩子——新维度另立分组手写，禁经本基类蔓延。
     *
     * @param type        反序列化声明类型（密封族取边界声明类型，如 {@code VerificationRecipient.class}）
     * @param sample      规范样例工厂 —— 每次调用须产出等值实例（相等性组据此断言值语义而非身份）
     * @param json        {@code sample} 的精确 wire 形态串
     * @param debug       {@code sample} 的精确 {@code toString()} 全串
     * @param invalidJson 必须被拒绝的 JSON
     * @param other       与 {@code sample} 不等的合法样例工厂
     */
    public record Contract<T>(
            Class<T> type,
            Supplier<T> sample,
            String json,
            String debug,
            String invalidJson,
            Supplier<T> other) {
    }

    /**
     * 被测 DP 的契约样例声明。
     */
    protected abstract Contract<T> contract();

    /**
     * 规范样例（每次调用重新构造，供相等性组取两个独立实例）。
     */
    protected final T sample() {
        return contract().sample().get();
    }

    /**
     * 与规范样例不等的合法样例。
     */
    protected final T other() {
        return contract().other().get();
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("序列化形状精确匹配声明的 wire 形态")
        void should_haveDeclaredWireShape() {
            assertThat(MAPPER.writeValueAsString(sample())).isEqualTo(contract().json());
        }

        @Test
        @DisplayName("round-trip 等价")
        void should_roundTrip() {
            assertThat(MAPPER.readValue(contract().json(), contract().type())).isEqualTo(sample());
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue(contract().invalidJson(), contract().type()))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(sample()).isEqualTo(sample());
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(sample()).isNotEqualTo(other());
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(sample()).hasSameHashCodeAs(sample());
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 精确全串")
        void should_haveExactToString() {
            assertThat(sample()).hasToString(contract().debug());
        }
    }
}
