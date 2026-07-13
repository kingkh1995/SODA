package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record DeleteUserRequest(
        @NotNull @JsonProperty("userId") Long userId
) {
}
