package com.soda.user.queryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * soda-user 读侧启动入口 (query-server)。
 *
 * <p>只做两件事：组件扫描（{@code @SpringBootApplication} 隐含，扫描 {@code com.soda.user}）与启动 Web 容器； 作为独立部署单元复用
 * infrastructure 层的持久化实现。JPA 实体与 Repository 的扫描根由持久化模块自持（{@code
 * com.soda.user.infrastructure.JpaPersistenceConfiguration}），与写侧入口约定一致。
 */
@SpringBootApplication(scanBasePackages = "com.soda.user")
public class SodaUserQueryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodaUserQueryServerApplication.class, args);
    }
}
