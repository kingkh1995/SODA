package com.soda.user.infrastructure;

import com.soda.component.infrastructure.persistence.JpaAuditingAutoConfiguration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 持久化切片测试引导配置 —— 本模块测试切片（{@code @DataJpaTest}）向上搜索 {@code @SpringBootConfiguration} 的锚点。
 *
 * <p>切片测试禁用主自动配置（{@code OverrideAutoConfiguration(enabled=false)}），数据源、 Flyway、JPA
 * 等由切片注解按需装配；本类只补两件事：
 *
 * <ul>
 *   <li>{@code @Import(JpaPersistenceConfiguration)}：实体与 Repository 的扫描根 —— 与生产装配同源单一定义， 本类不重复声明
 *       {@code @EntityScan}/{@code @EnableJpaRepositories}
 *   <li>{@code @Import(JpaAuditingAutoConfiguration)}：审计列填充——自定义自动配置 不在切片装配清单内，须显式引入
 * </ul>
 *
 * 属共享测试基础设施（配置锚点，非数据 fixture/基类，见 test-conventions §3）。
 */
@SpringBootConfiguration
@Import({JpaPersistenceConfiguration.class, JpaAuditingAutoConfiguration.class})
class InfrastructureTestApp {}
