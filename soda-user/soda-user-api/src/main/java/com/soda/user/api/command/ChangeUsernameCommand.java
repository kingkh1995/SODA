package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

public record ChangeUsernameCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newUsername") String newUsername
) implements Command {
}
