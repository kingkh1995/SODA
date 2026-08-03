package com.soda.user.domain.gateway;

import com.soda.component.domain.EntityGateway;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.User;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.Username;

import java.util.Optional;

/**
 * 用户聚合的持久化契约（防腐层接口）。
 * <p>
 * 继承 {@link EntityGateway} 提供基础 CRUD（save、remove、findById、findAllById），
 * 扩展按业务字段查询的 {@code findByXxx} 抽象方法；{@code existsByXxx}
 * 为基于 findBy 的 default 组合。
 * <p>
 * 实现类位于基础设施层（{@code soda-user-infrastructure}），由基础设施层保证 select4update 语义，application层不感知。
 *
 * @see EntityGateway
 * @see User
 */
public interface UserGateway extends EntityGateway<User, UserId> {

    /**
     * 按用户名查找用户。
     */
    Optional<User> findByUsername(Username username);

    /**
     * 按手机号查找用户。
     */
    Optional<User> findByMobile(Mobile mobile);

    /**
     * 按邮箱查找用户。
     */
    Optional<User> findByEmail(Email email);

    /**
     * 用户名是否存在（基于 {@link #findByUsername(Username)} 的 default 组合）。
     */
    default boolean existsByUsername(Username username) {
        return findByUsername(username).isPresent();
    }

    /**
     * 手机号是否存在（基于 {@link #findByMobile(Mobile)} 的 default 组合）。
     */
    default boolean existsByMobile(Mobile mobile) {
        return findByMobile(mobile).isPresent();
    }

    /**
     * 邮箱是否存在（基于 {@link #findByEmail(Email)} 的 default 组合）。
     */
    default boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }
}
