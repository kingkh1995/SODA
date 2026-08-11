package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.web.validation.Mobile;
import jakarta.validation.constraints.NotBlank;

public record RequestChangeMobileCodeRequest(
        @NotBlank @Mobile
        @JsonProperty("newMobile") String newMobile
) {
}
