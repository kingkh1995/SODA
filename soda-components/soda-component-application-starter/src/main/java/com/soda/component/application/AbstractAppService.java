package com.soda.component.application;

import com.soda.component.domain.Entity;
import com.soda.component.domain.EntityGateway;
import com.soda.component.domain.Identifier;
import org.springframework.util.Assert;

/**
 * ApplicationService 抽象基类。
 * <p>
 * 默认持有主体聚合的 {@link EntityGateway}（作为默认属性），并提供"加载或失败"的
 * {@link #require} 便捷方法，消除各用例中 {@code findById(...).orElse(null) + assertNotNull}
 * 的重复样板。
 * <p>
 * 失败策略：网关加载未找到是客户端可预期场景（参数校验桶），抛出 {@link IllegalArgumentException}，
 * 消息为 {@code 实体类型名 + " not found: " + 实体标识符}（如 "User not found: 1"），
 * 实体类型名来自构造器传入的 {@code Class}（见 ADR-0015：临时约定，接线时演进为带 reason 的业务异常）。
 *
 * @param <T>  实体类型，必须实现 {@link Entity}
 * @param <ID> 标识符类型，必须实现 {@link Identifier}
 * @param <G>  网关类型，必须继承 {@link EntityGateway}（保留模块专用方法，如 existsByXxx）
 * @see EntityGateway
 */
public abstract class AbstractAppService<T extends Entity<ID>, ID extends Identifier<?>,
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
}
