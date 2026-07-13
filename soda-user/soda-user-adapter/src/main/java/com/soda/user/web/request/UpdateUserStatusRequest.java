package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull @JsonProperty("userId") Long userId,
        @NotBlank @JsonProperty("status") String status
) {
}
