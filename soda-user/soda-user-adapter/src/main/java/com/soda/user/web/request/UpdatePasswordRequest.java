package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotNull @JsonProperty("userId") Long userId,

        @NotBlank @Size(min = 6, max = 32)
        @JsonProperty("newPassword") String newPassword
) {
}
