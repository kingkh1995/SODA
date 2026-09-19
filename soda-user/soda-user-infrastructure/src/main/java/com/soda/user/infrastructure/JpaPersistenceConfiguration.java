package com.soda.user.infrastructure;

import com.soda.user.infrastructure.persistence.UserPO;
import com.soda.user.infrastructure.repository.UserRepository;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 扫描根声明 —— 实体与 Repository 的扫描范围由持久化实现所在的模块声明，不由入口模块代劳。
 *
 * <p>入口模块（{@code soda-user-start} / {@code soda-user-query-server}）只保留
 * {@code @SpringBootApplication}：既不引用持久化类型，也不承载扫描根这一知识。本类位于两入口 {@code scanBasePackages =
 * "com.soda.user"} 覆盖范围内，经组件扫描自动装载。
 *
 * <p>扫描根取 {@code basePackageClasses} 而非包名字符串：包重命名／搬迁由编译器拦截，不会静默失配。
 *
 * <p>本类只承载扫描声明、无 {@code @Bean} 方法，故 {@code proxyBeanMethods = false}（免去 CGLIB 子类化）。
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = UserPO.class)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
public class JpaPersistenceConfiguration {}
