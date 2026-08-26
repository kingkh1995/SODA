package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;
import org.jspecify.annotations.Nullable;

/**
 * 更新用户资料命令 —— nickname/sex/avatar 全部可选，null 即不修改对应字段。
 * <p>
 * 用户名/手机号/邮箱变更走各自的专用命令，不在此列。
 */
public record UpdateUserCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("nickname") @Nullable String nickname,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar
) implements Command {
}
