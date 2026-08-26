package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 修改密码命令 —— 单步流程：校验原密码后重哈希（原密码比对为域守卫，
 * 不匹配抛 IAE，见 ADR-0027）。
 */
public record ChangePasswordCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("oldPassword") String oldPassword,
        @JsonProperty("newPassword") String newPassword
) implements Command {
}
