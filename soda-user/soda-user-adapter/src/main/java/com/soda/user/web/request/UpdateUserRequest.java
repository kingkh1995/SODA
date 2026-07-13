package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record UpdateUserRequest(
        @NotNull @JsonProperty("userId") Long userId,
        @Nullable @JsonProperty("nickname") String nickname,
        @Nullable @JsonProperty("mobile") String mobile,
        @Nullable @JsonProperty("email") String email,
        @Nullable @JsonProperty("sex") String sex,
        @Nullable @JsonProperty("avatar") String avatar
) {
}
