package com.soda.component.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 错误详情结构，遵循 AIP-193。
 * <p>
 * {@code metadata} 为 null 时随 {@link Result} 的 null 省略策略一并省略。
 *
 * @param reason   UPPER_SNAKE_CASE 语义码（如 ALREADY_EXISTS、INVALID_ARGUMENT）
 * @param domain   服务域（如 soda-user.example.com）
 * @param metadata 上下文键值对（如 username=admin）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorInfo(
        String reason,
        String domain,
        @Nullable Map<String, String> metadata
) {
}
