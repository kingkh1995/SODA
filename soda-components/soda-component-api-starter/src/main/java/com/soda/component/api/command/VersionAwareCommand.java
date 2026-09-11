package com.soda.component.api.command;

import org.jspecify.annotations.Nullable;

/**
 * 版本感知命令契约 — 声明携带乐观锁期望版本的写命令（见 ADR-0037）。
 * <p>
 * 实现 record 在组件上声明 {@code expectedVersion}（可空 {@code Integer}，承载 HTTP
 * {@code If-Match} 归一化后的期望版本，null = 未携带）；应用层经聚合侧 {@code Versioned} 判定
 * 统一比对（{@code AbstractAppService.requireIfMatch}），未实现本接口的命令自然放行（显式 opt-in）。
 * <p>
 * {@code null} = 未携带条件请求头 → 放行（宽松策略，见 ADR-0039）；缺席判定归应用侧守卫，勿引入 API 侧默认值。
 * <p>
 * 承载 {@code Integer} 而非 DP：api 层与领域类型解耦（JSON 反序列化直通），
 * 值比较语义与版本值相等一致。
 */
public interface VersionAwareCommand extends Command {

    /**
     * 客户端期望的当前版本号；null 表示未携带条件请求头，放行。
     *
     * @return 期望版本，可能为 null
     */
    @Nullable Integer expectedVersion();
}
