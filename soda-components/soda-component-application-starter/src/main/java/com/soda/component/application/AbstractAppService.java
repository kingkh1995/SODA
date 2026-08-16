package com.soda.component.application;

import com.soda.component.domain.Aggregate;
import com.soda.component.domain.EntityGateway;
import com.soda.component.domain.Identifier;
import com.soda.component.domain.Stateful;
import org.springframework.util.Assert;

/**
 * ApplicationService 抽象基类。
 * <p>
 * 默认持有主体聚合的 {@link EntityGateway}（作为默认属性），并提供"加载或失败"的
 * {@link #require} 与终态守卫 {@link #requireNotTerminal}（{@link Aggregate} 能力：
 * 身份 + 状态机 + 领域事件源）便捷方法，消除各用例中
 * {@code findById(...).orElse(null) + assertNotNull} 与终态判断的重复样板。
 * <p>
 * 失败策略：网关加载未找到是客户端可预期场景（参数校验桶），抛出 {@link IllegalArgumentException}，
 * 消息为 {@code 实体类型名 + " not found: " + 实体标识符}（如 "User not found: 1"），
 * 实体类型名来自构造器传入的 {@code Class}（见 ADR-0015：临时约定，接线时演进为带 reason 的业务异常）。
 *
 * @param <T>  主体聚合类型，必须实现 {@link Aggregate}
 * @param <ID> 标识符类型，必须实现 {@link Identifier}
 * @param <G>  网关类型，必须继承 {@link EntityGateway}（保留模块专用方法，如 existsByXxx）
 * @see EntityGateway
 */
public abstract class AbstractAppService<T extends Aggregate<ID>, ID extends Identifier<?>,
        G extends EntityGateway<T, ID>> {

    /**
     * 主体聚合的持久化网关（默认属性）。
     */
    protected final G gateway;
    /**
     * 实体类型，用于生成异常消息（如 "User not found: 1"）。
     */
    private final Class<T> clazz;

    protected AbstractAppService(Class<T> clazz, G gateway) {
        this.clazz = clazz;
        this.gateway = gateway;
    }

    /**
     * 按 ID 加载实体；不存在时抛出 {@link IllegalArgumentException}（消息含实体标识符）。
     *
     * @param id 实体标识符，非 null
     * @return 实体，总为非 null
     */
    protected final T require(ID id) {
        var entity = gateway.findById(id).orElse(null);
        Assert.notNull(entity, clazz.getSimpleName() + " not found: " + id.identifier());
        return entity;
    }

    /**
     * 终态（吸收态）守卫 — 按 ID 加载实体并断言其非终态，返回实体。
     * <p>
     * 基于 {@link #require}：先按 ID 加载（未找到抛 IAE），再断言非终态。
     * 应用层通用前置：终态实体禁止一切写（状态机吸收态语义——{@link Aggregate} 实现
     * {@link Stateful}，主体聚合自动具备 {@code isTerminal()}，普通实体不实现本契约）。
     * 非终态检查是唯一跨状态机一致的概念（初始态/禁用态为状态机专属或业务状态，不进通用契约）。
     * 抛 {@link IllegalArgumentException}（业务拒绝，与领域 R 吸收态 IAE 一致；
     * 持久层兜底为网关内行终态判定裸抛的 ISE，防御编程不携消息）。
     * 用例级状态前置（如 CC 发码要求启用态）仍为用例业务断言，不属于本守卫。
     *
     * @param id 实体标识符，非 null
     * @return 断言后的实体，总为非 null
     * @throws IllegalArgumentException 实体不存在或处于终态（吸收态）时
     */
    protected final T requireNotTerminal(ID id) {
        var entity = require(id);
        Assert.isTrue(!entity.isTerminal(),
                entity.getClass().getSimpleName() + " is in terminal state, no writes allowed");
        return entity;
    }
}
