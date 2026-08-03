package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 验证邮箱验证码并变更邮箱命令 — 用于邮箱变更的两步验证第二步。
 *
 * @see com.soda.user.api.UserAuthService#changeEmail
 */
public record ChangeEmailCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("code") String code
) implements Command {
}
