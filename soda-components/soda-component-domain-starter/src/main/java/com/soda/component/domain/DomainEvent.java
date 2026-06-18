package com.soda.component.domain;

import java.io.Serializable;
import java.time.Instant;

/**
 * 领域事件的基接口。
 * <p>
 * 子类<strong>必须</strong>是 {@code record} — 不可变、线程安全、值语义。
 * {@code entityId} 和 {@code occurredAt} 作为 record 组件自动实现接口方法。
 * <p>
 * 例如：
 * <pre>{@code
 * public record UserCreated(UserId entityId, Instant occurredAt, String newName)
 *         implements DomainEvent<UserId> {}
 * }</pre>
 *
 * @param <ID> 所属实体的标识符类型
 * @see DomainEventBus
 * @see Entity
 */
public interface DomainEvent<ID extends Identifier<?>> extends Serializable {

    /** 来源实体的标识符。 */
    ID entityId();

    /** 事件发生时间。 */
    Instant occurredAt();
}
