package com.soda.user.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * soda-user 写侧启动入口。
 * <p>
 * {@code @SpringBootApplication} 位于 {@code com.soda.user} 父包，
 * 自动扫描所有子模块（api, domain, application, infrastructure, adapter, queryserver）。
 * JPA 实体与 Repository 位于 {@code infrastructure.persistence} 子包，
 * 需显式 {@code @EntityScan}/{@code @EnableJpaRepositories} 指定根包
 * （Spring Boot 默认扫描主类包 {@code com.soda.user.start}，扫不到）。
 */
@SpringBootApplication(scanBasePackages = "com.soda.user")
@EntityScan(basePackages = "com.soda.user")
@EnableJpaRepositories(basePackages = "com.soda.user")
public class SodaUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodaUserApplication.class, args);
    }
}
