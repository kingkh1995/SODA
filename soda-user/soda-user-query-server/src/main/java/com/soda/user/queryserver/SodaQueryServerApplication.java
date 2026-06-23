package com.soda.user.queryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * soda-user 读侧启动入口 (query-server)。
 * <p>
 * {@code @SpringBootApplication} 扫描 {@code com.soda.user} 父包，
 * 复用 infrastructure 层的 Mapper，同时作为独立部署单元。
 */
@SpringBootApplication(scanBasePackages = "com.soda.user")
public class SodaQueryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodaQueryServerApplication.class, args);
    }
}
