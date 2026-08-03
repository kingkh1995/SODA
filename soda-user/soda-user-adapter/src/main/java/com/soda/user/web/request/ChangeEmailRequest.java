package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank
        @Size(min = 8, max = 8)
        @JsonProperty("code") String code
) {
}
