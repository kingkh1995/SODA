package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 启用用户命令 —— D→E 发 UserStateChangedEvent；已是 E 则 no-op；
 * R 拒绝（吸收态，见 ADR-0017）。
 */
public record EnableUserCommand(
        @JsonProperty("userId") Long userId
) implements Command {
}
