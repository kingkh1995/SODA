package com.soda.user.infrastructure.repository;

import com.soda.user.infrastructure.persistence.UserArchivePO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserArchiveRepository} 真库行为（H2 MODE=MySQL + Flyway V1，ADR-0022）——
 * {@code user_archive} 归档快照列映射、{@code archive_time} 审计填充、主键 1:1 路由
 * （id 恒有 → 恒 merge，ADR-0024）。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserArchiveRepository 真库行为")
class UserArchiveRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserArchiveRepository userArchiveRepository;

    /**
     * 快照行内联 fixture（可空键由用例覆写）。
     */
    private static UserArchivePO archive(Long id) {
        var po = new UserArchivePO();
        po.setId(id);
        po.setUsername("alice");
        return po;
    }

    @Test
    @DisplayName("原键快照落列一致；archive_time 由 auditing 填充")
    void should_persistSnapshot_withAuditedArchiveTime() {
        var po = archive(42L);
        po.setMobile("13800138000");
        po.setEmail("alice@example.com");

        var saved = userArchiveRepository.saveAndFlush(po);
        em.clear();

        var reloaded = userArchiveRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUsername()).isEqualTo("alice");
        assertThat(reloaded.getMobile()).isEqualTo("13800138000");
        assertThat(reloaded.getEmail()).isEqualTo("alice@example.com");
        assertThat(reloaded.getArchiveTime()).isNotNull();
    }

    @Test
    @DisplayName("可空键允许 NULL——注销前未绑定的登录方式")
    void should_allowNullOptionalKeys() {
        userArchiveRepository.saveAndFlush(archive(43L));
        em.clear();

        var reloaded = userArchiveRepository.findById(43L).orElseThrow();
        assertThat(reloaded.getMobile()).isNull();
        assertThat(reloaded.getEmail()).isNull();
        assertThat(reloaded.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("同 id 重复归档不产生第二行——主键 1:1，merge 路由 update")
    void should_keepSingleRow_when_archivedTwice() {
        userArchiveRepository.saveAndFlush(archive(44L));
        userArchiveRepository.saveAndFlush(archive(44L));
        em.clear();

        assertThat(userArchiveRepository.count()).isEqualTo(1);
    }
}
