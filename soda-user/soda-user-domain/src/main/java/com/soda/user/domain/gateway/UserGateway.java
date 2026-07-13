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
 * 扩展按业务字段查询的方法。
 * <p>
 * 实现类位于基础设施层（{@code soda-user-infrastructure}）。
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
     * 用户名是否存在（唯一性校验）。
     */
    boolean existsByUsername(Username username);

    /**
     * 手机号是否存在。
     */
    boolean existsByMobile(Mobile mobile);

    /**
     * 邮箱是否存在。
     */
    boolean existsByEmail(Email email);

    /**
     * 按邮箱查找用户。
     */
    Optional<User> findByEmail(Email email);
}
