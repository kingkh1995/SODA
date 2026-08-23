package com.soda.component.domain;

/**
 * 状态机枚举标记接口 — 生命周期状态（初始态/终态概念）枚举的统一契约。
 * <p>
 * 状态机形态（ADR-0017）：单一维度枚举 + 聚合根命令迁移方法；终态为吸收态，
 * 进入后无任何操作可执行。本接口把「终态判定」提升为框架级契约，供领域守卫
 * 与基础设施兜底统一消费——终态行不可写守卫按 {@link #terminal()} 泛化，
 * 新增终态只需枚举成员标记 terminal，守卫零改动（ADR-0023）。
 * <p>
 * 实现类：{@code UserState}（R 终态）、{@code VerificationState}（U 终态）。
 * <b>不提供</b> {@code isInitial()}/{@code isNew()}：初始态语义随状态机而异
 * （如未来「待激活」态会使当前创建态不再是初始态）；{@code isNew} 与 Spring
 * {@code Persistable#isNew()}（持久化状态检测）同名异义，见 ADR-0023。
 *
 * @see EnumType
 * @see Type
 */
public interface StateEnumType extends EnumType {

    /**
     * 是否为终态（吸收态）— 进入后无任何操作可执行。
     *
     * @return 终态返回 {@code true}
     */
    default boolean terminal() {
        return false;
    }
}
