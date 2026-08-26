package com.soda.component.domain;

/**
 * 状态机对象契约接口 — 聚合根的状态机声明（与枚举侧 {@link StateEnumType} 对称）。
 * <p>
 * 实现类声明「我是状态机对象」：暴露当前状态枚举（{@link #getState()}，<b>非空契约</b>——
 * 状态机实体恒有状态），终态（吸收态）判定经 {@link #isTerminal()} 委托
 * {@link StateEnumType#terminal()}——终态逻辑单一事实源在状态枚举，聚合侧只是消费接缝。
 * <p>
 * 由 {@link Aggregate} 基类实现（<b>聚合根能力</b>：身份 + 状态机 + 领域事件源，见
 * {@link Aggregate}）：{@code getState()} 由具体聚合经 Lombok {@code @Getter} 生成的
 * {@code getState()} 满足（协变返回各自状态枚举，具体类型在具体聚合上可见，无需泛型参数）——
 * 命名遵循仓库 Getter 约定，零显式方法。普通实体（如 {@code AuthAccount}，仅布尔
 * {@code active}、无生命周期状态枚举）不实现本契约。
 * <p>
 * 消费方：应用层经 {@code AbstractAppService.requireNotTerminal} 统一守卫——终态实体
 * 禁止一切写（IAE，业务拒绝，与领域 R 吸收态 IAE 一致）；持久层兜底为
 * 网关 save 内行终态判定裸抛 ISE（防御编程，不携消息，绕过领域/应用的写路径）。
 * <p>
 * 仅承载跨状态机一致的「终态禁写」概念——初始态语义随状态机而异（恢复实体不在初始态）、
 * 禁用态是业务状态（User 的 D 专有，Verification 无此概念），均不进本契约（见
 * {@link StateEnumType} javadoc 与 ADR-0023）。用例级状态前置（如 CC 发码要求启用态）
 * 仍为用例业务断言，不属于本契约。
 * <p>
 * 新增状态机聚合：状态枚举实现 {@link StateEnumType} + 聚合的 {@code getState()} 返回它
 * （@Getter 即满足），零额外样板。
 *
 * @see StateEnumType
 * @see Aggregate
 */
public interface Stateful {

    /**
     * 状态机当前状态（枚举实现 {@link StateEnumType}，终态判定单一事实源）。
     * <p>
     * 非空契约：状态机实体恒有状态（如 {@code UserState}/{@code VerificationState}），
     * 不允许 {@code null}——普通实体不实现本接口，不存在「无状态的状态机对象」。
     *
     * @return 当前状态枚举，恒非 null
     */
    StateEnumType getState();

    /**
     * 是否为终态（吸收态）— 禁止一切写。默认委托 {@code getState().terminal()}，
     * 新增终态只需枚举成员标记 terminal，本接口与守卫零改动。
     *
     * @return 终态返回 {@code true}
     */
    default boolean isTerminal() {
        return getState().terminal();
    }
}
