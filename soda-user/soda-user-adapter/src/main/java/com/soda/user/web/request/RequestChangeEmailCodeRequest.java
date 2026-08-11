package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestChangeEmailCodeRequest(
        @NotBlank @Email
        @JsonProperty("newEmail") String newEmail
) {
}
