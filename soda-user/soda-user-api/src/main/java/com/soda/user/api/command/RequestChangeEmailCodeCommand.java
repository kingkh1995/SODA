package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 发送邮箱换绑验证码命令 — 用于邮箱变更的两步验证第一步。
 *
 * @see com.soda.user.api.UserAuthService#requestChangeEmailCode
 */
public record RequestChangeEmailCodeCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newEmail") String newEmail
) implements Command {
}
