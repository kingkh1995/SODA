package com.soda.user.queryserver;

import com.soda.user.domain.gateway.VerificationGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 读侧部署单元启动冒烟——跨越「能启动」seam 的唯一测试：完整上下文装配，
 * 扫描 {@code com.soda.user} 复用写侧模块装配（基础设施 gateway/repository、
 * Flyway V1 迁移；H2 内存库 MODE=MySQL 同写侧，ADR-0022）。
 */
@SpringBootTest(classes = SodaUserQueryServerApplication.class)
@DisplayName("SodaUserQueryServerApplication 读侧入口")
class SodaUserQueryServerApplicationTest {

    @Autowired
    private VerificationGateway verificationGateway;

    @Test
    @DisplayName("上下文可启动且复用写侧装配——基础设施 gateway 就位")
    void should_bootContextAndExposeGateways_when_deployableStarts() {
        assertThat(verificationGateway).isNotNull();
    }
}
