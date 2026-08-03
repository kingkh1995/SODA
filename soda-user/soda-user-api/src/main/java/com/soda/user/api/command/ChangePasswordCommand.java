package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

public record ChangePasswordCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newPassword") String newPassword
) implements Command {
}
