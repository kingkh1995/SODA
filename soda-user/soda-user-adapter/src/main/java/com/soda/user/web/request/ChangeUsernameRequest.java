package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeUsernameRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$")
        @JsonProperty("newUsername") String newUsername
) {
}
