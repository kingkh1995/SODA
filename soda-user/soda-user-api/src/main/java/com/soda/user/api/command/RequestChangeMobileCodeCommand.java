package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 发送手机号换绑验证码命令 — 用于手机号变更的两步验证第一步。
 *
 * @see com.soda.user.api.UserAuthService#requestChangeMobileCode
 */
public record RequestChangeMobileCodeCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newMobile") String newMobile
) implements Command {
}
