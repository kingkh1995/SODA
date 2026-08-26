package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 修改账号命令 —— 新 Username 全局唯一前置检查（与 DB 唯一索引双重保证）。
 */
public record ChangeUsernameCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newUsername") String newUsername
) implements Command {
}
