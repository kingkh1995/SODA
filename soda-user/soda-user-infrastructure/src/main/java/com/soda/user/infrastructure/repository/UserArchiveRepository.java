package com.soda.user.infrastructure.repository;

import com.soda.user.infrastructure.persistence.UserArchivePO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code user_archive} 表 Spring Data JPA Repository — 基础设施内部接口（仅写入，ADR-0023）。
 */
public interface UserArchiveRepository extends JpaRepository<UserArchivePO, Long> {
}
