package com.soda.user.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * soda-user 写侧启动入口。
 *
 * <p>只做两件事：组件扫描（{@code @SpringBootApplication} 隐含，扫描 {@code com.soda.user} 全部子模块）与启动 Web 容器。JPA 实体与
 * Repository 的扫描根由持久化模块自持（{@code com.soda.user.infrastructure.JpaPersistenceConfiguration}），本类不再声明
 * {@code @EntityScan}/{@code @EnableJpaRepositories}。
 */
@SpringBootApplication(scanBasePackages = "com.soda.user")
public class SodaUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SodaUserApplication.class, args);
    }
}
