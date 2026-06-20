package com.soda.user.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * soda-user 写侧启动入口。
 * <p>
 * {@code @SpringBootApplication} 位于 {@code com.soda.user} 父包，
 * 自动扫描所有子模块（api, domain, app, infrastructure, adapter, queryserver）。
 */
@SpringBootApplication(scanBasePackages = "com.soda.user")
public class SodaUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodaUserApplication.class, args);
    }
}
