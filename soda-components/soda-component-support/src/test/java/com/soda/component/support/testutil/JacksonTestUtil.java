package com.soda.component.support.testutil;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试工具 — Jackson 序列化/反序列化 round-trip 校验。
 * <p>
 * 消除各 DP 测试中重复的 {@code new ObjectMapper()} + round-trip 样板代码。
 */
public final class JacksonTestUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JacksonTestUtil() {
    }

    /**
     * 返回共享的 ObjectMapper 实例，供各测试文件在需要直接读写 JSON 时使用。
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 对 {@code original} 序列化后反序列化，断言双向一致。
     */
    public static <T> void assertRoundTrip(T original, Class<T> type) throws Exception {
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, type);
        assertThat(restored).isEqualTo(original);
    }
}
