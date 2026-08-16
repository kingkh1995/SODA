package com.soda.user.infrastructure.repository;

import com.soda.user.infrastructure.persistence.UserPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@code user} 表 Spring Data JPA Repository — 基础设施内部接口。
 */
public interface UserRepository extends JpaRepository<UserPO, Long> {

    Optional<UserPO> findByUsername(String username);

    Optional<UserPO> findByMobile(String mobile);

    Optional<UserPO> findByEmail(String email);
}
