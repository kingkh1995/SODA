package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.VersionAwareCommand;

import org.jspecify.annotations.Nullable;

/**
 * 更新用户资料命令 —— nickname/sex/avatar 全部可选；{@code updateMask} 控制实际生效字段 （AIP-134 §3.5 / AIP-161：省略 = 只写非
 * null 字段，命中的 nullable 字段传 null = 清空，{@code "*"} = 全量替换）。
 *
 * <p>用户名/手机号/邮箱变更走各自的专用命令，不在此列。 {@code expectedVersion} 承载 HTTP {@code If-Match} 归一化后的期望版本（null =
 * 未携带 → 放行，见 ADR-0039； 比对在应用层同一事务内完成，见 ADR-0037）。
 */
public record UpdateUserCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("updateMask") @Nullable String updateMask,
        @JsonProperty("nickname") @Nullable String nickname,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar,
        @JsonProperty("expectedVersion") @Nullable Integer expectedVersion)
        implements VersionAwareCommand {}
