package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 验证短信验证码并变更手机号命令 — 用于手机号变更的两步验证第二步。
 *
 * @see com.soda.user.api.UserAuthService#changeMobile
 */
public record ChangeMobileCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("code") String code
) implements Command {
}
