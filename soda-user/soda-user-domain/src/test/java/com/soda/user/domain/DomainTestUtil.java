package com.soda.user.domain;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 测试工具 — 共享的 Jackson {@link ObjectMapper} 实例。
 * <p>
 * 消除各 DP/Entity 测试中重复的 {@code new ObjectMapper()} 样板代码。
 * <br>
 * 需要自定义配置的测试（如 {@link VerificationCodeTest}）请用 {@link #MAPPER}{@code .copy()} 后派生。
 * <p>
 * 注意：Jackson 3.x 原生支持 JSR310 类型 (Instant, Duration 等)，无需额外注册 JavaTimeModule。
 */
public final class DomainTestUtil {

    /**
     * 共享的 Jackson 3 默认配置 ObjectMapper。
     */
    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .build();

    private DomainTestUtil() {
    }
}
