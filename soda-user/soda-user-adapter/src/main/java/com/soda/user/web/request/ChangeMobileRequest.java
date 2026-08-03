package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMobileRequest(
        @NotBlank
        @Size(min = 6, max = 6)
        @JsonProperty("code") String code
) {
}
