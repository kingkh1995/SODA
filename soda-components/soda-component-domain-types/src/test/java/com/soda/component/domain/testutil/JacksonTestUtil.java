package com.soda.component.domain.testutil;

import tools.jackson.databind.ObjectMapper;

/**
 * 组件模块 DP 测试共享 Mapper —— 序列化/反序列化断言统一经 {@link #MAPPER} 进行，
 * 各测试文件不得自建 {@code ObjectMapper}。
 */
public final class JacksonTestUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JacksonTestUtil() {
    }
}
