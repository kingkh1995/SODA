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
 * 继承 {@link EntityGateway} 提供基础 CRUD（save、findById、findAllById），
 * 扩展按业务字段查询的 {@code findByXxx} 抽象方法；{@code existsByXxx}
 * 为基于 findBy 的 default 组合。
 * <p>
 * 实现类位于基础设施层（{@code soda-user-infrastructure}）。
 * <p>
 * 终态持久化：注销（{@link User#deregister()}）后状态 R 由 {@code save} 持久化（ADR-0017），
 * 表示（状态列 / 软删 / 删行）由基础设施层决定，领域不感知擦除。
 *
 * @see EntityGateway
 * @see User
 */
public interface UserGateway extends EntityGateway<User, UserId> {

    Optional<User> findByUsername(Username username);

    Optional<User> findByMobile(Mobile mobile);

    Optional<User> findByEmail(Email email);

    /**
     * 用户名唯一性预检（与 DB 唯一索引双重保证 Username 全局唯一）。
     */
    default boolean existsByUsername(Username username) {
        return findByUsername(username).isPresent();
    }

    /**
     * 手机号唯一性预检（UCC 换绑的目标全局唯一前置检查）。
     */
    default boolean existsByMobile(Mobile mobile) {
        return findByMobile(mobile).isPresent();
    }

    /**
     * 邮箱唯一性预检（UCC 换绑的目标全局唯一前置检查）。
     */
    default boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }
}
