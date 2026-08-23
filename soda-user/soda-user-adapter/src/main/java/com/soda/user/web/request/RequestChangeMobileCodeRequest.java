package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.web.validation.MobileNumber;
import jakarta.validation.constraints.NotBlank;

public record RequestChangeMobileCodeRequest(
        @NotBlank @MobileNumber
        @JsonProperty("newMobile") String newMobile
) {
}
