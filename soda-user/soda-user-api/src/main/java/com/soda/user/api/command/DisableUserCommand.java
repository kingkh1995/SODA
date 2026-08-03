package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

public record DisableUserCommand(
        @JsonProperty("userId") Long userId
) implements Command {
}
