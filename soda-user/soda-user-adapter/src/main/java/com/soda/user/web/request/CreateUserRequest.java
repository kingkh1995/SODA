package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateUserRequest(
        @NotBlank @Size(min = 4, max = 30)
        @JsonProperty("username") String username,

        @NotBlank @Size(min = 6, max = 32)
        @JsonProperty("password") String password,

        @NotBlank @Size(max = 30)
        @JsonProperty("nickname") String nickname,

        @Nullable @JsonProperty("mobile") String mobile,
        @Nullable @JsonProperty("email") String email,
        @Nullable @JsonProperty("sex") String sex,
        @Nullable @JsonProperty("avatar") String avatar
) {
}
