package com.soda.component.application;

import com.soda.component.api.error.NotFoundException;
import com.soda.component.api.error.PreconditionFailedException;
import com.soda.component.domain.Aggregate;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.EntityGateway;
import com.soda.component.domain.Identifier;
import com.soda.component.domain.IntLiteralType;
import com.soda.component.domain.Stateful;
import com.soda.component.domain.Versioned;
import jakarta.annotation.Nullable;
import org.springframework.util.Assert;

/**
 * ApplicationService 抽象基类。
 * <p>
 * 默认持有主体聚合的 {@link EntityGateway}（作为默认属性）与 {@link DomainEventBus}，并提供"加载或失败"的
 * {@link #require}、终态守卫 {@link #requireNotTerminal} 与条件请求版本守卫 {@link #requireIfMatch}
 * （{@link Aggregate} 能力：身份 + 状态机 + 领域事件源）便捷方法，消除各用例中
 * {@code findById(...).orElse(null) + assertNotNull} 与终态判断的重复样板。
 * <p>
 * 用例末梢持久化统一走 {@link #saveAndPublishEvents}，消除
 * {@code gateway.save(agg); domainEventBus.publishAll(agg.flushEvents());} 二件套。
 *
 * @param <T>  主体聚合类型，必须实现 {@link Aggregate}
 * @param <ID> 标识符类型，必须实现 {@link Identifier}
 * @param <G>  网关类型，必须继承 {@link EntityGateway}（保留模块专用方法，如 existsByXxx）
 * @see EntityGateway
 */
public abstract class AbstractAppService<T extends Aggregate<ID>, ID extends Identifier<?>, G extends EntityGateway<T, ID>> {

    /**
     * 主体聚合的持久化网关（默认属性）。
     */
    protected final G gateway;
    /**
     * 领域事件总线 — 用例持久化后经 {@link #saveAndPublishEvents} 发布聚合刷出的所有事件。
     */
    protected final DomainEventBus domainEventBus;
    /**
     * 实体类型，用于生成异常消息（如 "User not found: 1"）。
     */
    private final Class<T> clazz;

    protected AbstractAppService(Class<T> clazz, G gateway, DomainEventBus domainEventBus) {
        this.clazz = clazz;
        this.gateway = gateway;
        this.domainEventBus = domainEventBus;
    }

    /**
     * 按 ID 加载实体；不存在时抛 {@link NotFoundException#entityNotFound}（404，见 ADR-0015）。
     *
     * @param id 实体标识符，非 null
     * @return 实体，总为非 null
     * @throws NotFoundException 实体不存在时
     */
    protected final T require(ID id) {
        return gateway.findById(id)
                .orElseThrow(() -> NotFoundException.entityNotFound(clazz.getSimpleName(), id.identifier()));
    }

    /**
     * 条件请求版本守卫 — 按 ID 加载实体并断言其版本等于客户端期望版本，返回实体（见 ADR-0037）。
     * <p>
     * 基于 {@link #require}：先按 ID 加载（不存在抛 404），再经 {@link Versioned#ifMatch} 比对；
     * 聚合未实现 {@link Versioned} 时拒绝（条件请求不适用于无版本列的实体，fail-loud 而非静默放行，
     * 抛裸 ISE 属防御编程、译 500）。
     * 失配抛 {@link PreconditionFailedException#versionMismatch}（412，见 ADR-0039）。
     *
     * @param id              实体标识符，非 null
     * @param expectedVersion 客户端期望版本（If-Match 归一化结果）；{@code null} = 未携带 → 放行
     * @return 断言后的实体，总为非 null
     * @throws NotFoundException           实体不存在时
     * @throws PreconditionFailedException 版本失配时
     */
    protected final T requireIfMatch(ID id, @Nullable IntLiteralType expectedVersion) {
        var entity = require(id);
        if (expectedVersion == null) {
            return entity;
        }
        if (entity instanceof Versioned versioned) {
            if (!versioned.ifMatch(expectedVersion)) {
                throw PreconditionFailedException.versionMismatch(
                        expectedVersion.value(), versioned.getVersion().value());
            }
        } else {
            throw new IllegalStateException(entity.getClass().getSimpleName() + " not versioned");
        }
        return entity;
    }

    /**
     * 终态（吸收态）守卫 — 按 ID 加载实体并断言其非终态，返回实体。
     * <p>
     * 基于 {@link #require}：先按 ID 加载（不存在抛 404），再断言非终态。
     * 应用层通用前置：终态实体禁止一切写（状态机吸收态语义——{@link Aggregate} 实现
     * {@link Stateful}，主体聚合自动具备 {@code isTerminal()}，普通实体不实现本契约）。
     * 非终态检查是唯一跨状态机一致的概念（初始态/禁用态为状态机专属或业务状态，不进通用契约）。
     * 抛 {@link IllegalArgumentException}（业务拒绝，与领域 R 吸收态 IAE 一致；
     * 持久层兜底为网关内行终态判定裸抛的 ISE，防御编程不携消息）。
     * 用例级状态前置（如 CC 发码要求启用态）仍为用例业务断言，不属于本守卫。
     *
     * @param id 实体标识符，非 null
     * @return 断言后的实体，总为非 null
     * @throws NotFoundException        实体不存在时
     * @throws IllegalArgumentException 实体处于终态（吸收态）时
     */
    protected final T requireNotTerminal(ID id) {
        var entity = require(id);
        Assert.isTrue(!entity.isTerminal(), entity.getClass().getSimpleName() + " is in terminal state, no writes allowed");
        return entity;
    }

    /**
     * 持久化聚合并发布其刷出的所有领域事件 — 替代用例末梢重复的
     * {@code gateway.save(agg); domainEventBus.publishAll(agg.flushEvents());} 二件套。
     * <p>
     * 聚合未注册事件时 {@link Aggregate#flushEvents} 返回空列表，本方法安全（发布空操作为无操作）。
     *
     * @param agg 待持久化的聚合，非 null
     */
    protected final void saveAndPublishEvents(T agg) {
        gateway.save(agg);
        domainEventBus.publishAll(agg.flushEvents());
    }
}
