package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 禁用用户命令 —— E→D 发 UserStateChangedEvent；已是 D 则 no-op；
 * R 拒绝（吸收态，见 ADR-0017）。
 */
public record DisableUserCommand(
        @JsonProperty("userId") Long userId
) implements Command {
}
