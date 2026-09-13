package com.soda.component.domain.testutil;

import tools.jackson.databind.ObjectMapper;

/**
 * 组件模块 DP 测试共享 Mapper —— 序列化/反序列化断言统一经 {@link #MAPPER} 进行，
 * 各测试文件不得自建 {@code ObjectMapper}。
 * <p>
 * 位于 test fixtures 源集：本模块测试与下游模块（soda-user-domain）的契约测试共用同一实例。
 */
public final class JacksonTestUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JacksonTestUtil() {
    }
}
