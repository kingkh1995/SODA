package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
        @NotNull @JsonProperty("userId") Long userId,
        @NotBlank @Size(min = 4, max = 30)
        @JsonProperty("newUsername") String newUsername
) {
}
