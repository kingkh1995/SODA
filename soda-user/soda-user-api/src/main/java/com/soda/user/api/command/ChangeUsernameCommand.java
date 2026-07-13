package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangeUsernameCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newUsername") String newUsername
) {
}
