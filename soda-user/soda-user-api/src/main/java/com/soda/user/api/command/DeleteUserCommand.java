package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeleteUserCommand(
        @JsonProperty("userId") Long userId
) {
}
