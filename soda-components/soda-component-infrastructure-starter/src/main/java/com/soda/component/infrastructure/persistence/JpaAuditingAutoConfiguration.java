package com.soda.component.infrastructure.persistence;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA 审计自动配置 — {@code @CreatedDate}/{@code @LastModifiedDate} + {@code
 * AuditingEntityListener}（官方审计机制，见 {@link AbstractAuditable}）。
 *
 * <p>时间类型为 {@link Instant}（绝对时间点，仓库约定，见 VerificationPO.expireAt）—— 默认 {@code DateTimeProvider} 返回
 * {@code LocalDateTime.now()}（服务器本地时区）， 且 {@code Instant.from(LocalDateTime)} 无时区信息会抛异常，故显式提供返回
 * {@code Instant.now()} 的 provider。 未接入操作人身份，故不配置 {@code @CreatedBy}/{@code @LastModifiedBy} 与
 * auditorAwareRef。
 *
 * <p>仅在 JPA 审计能力在类路径、且容器无 {@link DateTimeProvider} bean 时生效——消费方可提供自定义 provider（如固定时钟）覆盖。
 */
@AutoConfiguration
@ConditionalOnClass(EnableJpaAuditing.class)
@EnableJpaAuditing
public class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DateTimeProvider.class)
    DateTimeProvider auditableDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
