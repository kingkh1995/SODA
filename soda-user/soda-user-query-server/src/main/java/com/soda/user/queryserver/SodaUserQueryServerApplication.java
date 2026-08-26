package com.soda.user.queryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * soda-user 读侧启动入口 (query-server)。
 * <p>
 * {@code @SpringBootApplication} 扫描 {@code com.soda.user} 父包，
 * 复用 infrastructure 层的 Mapper，同时作为独立部署单元。
 * JPA 实体与 Repository 位于 {@code infrastructure.persistence} 子包，
 * 需显式 {@code @EntityScan}/{@code @EnableJpaRepositories} 指定根包
 * （主类包 {@code com.soda.user.queryserver} 扫不到，同写侧入口约定）。
 */
@SpringBootApplication(scanBasePackages = "com.soda.user")
@EntityScan(basePackages = "com.soda.user")
@EnableJpaRepositories(basePackages = "com.soda.user")
public class SodaUserQueryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodaUserQueryServerApplication.class, args);
    }
}
