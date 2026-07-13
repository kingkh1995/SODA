package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdatePasswordCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newPassword") String newPassword
) {
}
