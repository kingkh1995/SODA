package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateUserStatusCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("status") String status
) {
}
