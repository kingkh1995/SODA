package com.soda.user.web.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record UserResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("nickname") String nickname,
        @JsonProperty("mobile") @Nullable String mobile,
        @JsonProperty("email") @Nullable String email,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar,
        @JsonProperty("status") String status
) {
}
