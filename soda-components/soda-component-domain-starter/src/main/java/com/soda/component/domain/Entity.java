package com.soda.component.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 领域实体的抽象基类。
 * <p>
 * 实体是具有连续身份标识（identity thread）的领域对象。
 * 两个实体相等当且仅当它们属于同一类型且拥有相同的 {@link Identifier}，
 * 与其他属性的值无关。
 * <p>
 * 具体实体类应通过 Lombok {@code @EqualsAndHashCode} 基于标识符实现
 * {@code equals()} 和 {@code hashCode()}。
 *
 * @param <ID> 标识符类型
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Entity<ID extends Identifier<?>> {

    private final ID id;
}
