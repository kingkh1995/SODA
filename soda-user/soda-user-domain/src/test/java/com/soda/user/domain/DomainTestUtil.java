package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 测试工具 — 共享的 Jackson {@link ObjectMapper} 实例。
 * <p>
 * 消除各 DP/Entity 测试中重复的 {@code new ObjectMapper().registerModule(new JavaTimeModule())} 样板代码。
 * <br>
 * 需要自定义配置的测试（如 {@link VerificationCodeTest}）请用 {@link #MAPPER}{@code .copy()} 后派生。
 */
public final class DomainTestUtil {

    /**
     * 预配置了 JSR310 模块的共享 ObjectMapper，可处理 {@code Instant}、{@code Duration} 等类型。
     */
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private DomainTestUtil() {
    }
}
