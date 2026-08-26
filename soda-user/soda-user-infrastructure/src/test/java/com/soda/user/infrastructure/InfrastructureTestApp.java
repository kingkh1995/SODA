package com.soda.user.infrastructure;

import com.soda.component.infrastructure.persistence.JpaAuditingAutoConfiguration;
import com.soda.user.infrastructure.persistence.UserPO;
import com.soda.user.infrastructure.repository.UserRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 持久化切片测试引导配置 —— 本模块测试切片（{@code @DataJpaTest}）向上搜索
 * {@code @SpringBootConfiguration} 的锚点。
 * <p>
 * 切片测试禁用主自动配置（{@code OverrideAutoConfiguration(enabled=false)}），数据源、
 * Flyway、JPA 等由切片注解按需装配；本类只补三件事：
 * <ul>
 *   <li>{@code @EntityScan}/{@code @EnableJpaRepositories}：显式圈定实体与仓储扫描包
 *       （无 {@code @SpringBootApplication} 时没有 AutoConfigurationPackages 基准包）</li>
 *   <li>{@code @Import(JpaAuditingAutoConfiguration)}：审计列填充——自定义自动配置
 *       不在切片装配清单内，须显式引入</li>
 * </ul>
 * 属共享测试基础设施（配置锚点，非数据 fixture/基类，见 test-conventions §3）。
 */
@SpringBootConfiguration
@EntityScan(basePackageClasses = UserPO.class)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
@Import(JpaAuditingAutoConfiguration.class)
class InfrastructureTestApp {
}
