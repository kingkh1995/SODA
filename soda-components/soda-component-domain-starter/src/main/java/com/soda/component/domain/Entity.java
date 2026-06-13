package com.soda.component.domain;

import lombok.Getter;

import java.util.Objects;

/**
 * 领域实体的抽象基类。
 * <p>
 * 实体是具有连续身份标识（identity thread）的领域对象。
 *
 * @param <ID> 标识符类型
 */
@Getter
public abstract class Entity<ID extends Identifier<?>> {

    private final ID id;

    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }
}
