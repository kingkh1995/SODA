package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record CreateUserCommand(
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("nickname") String nickname,
        @JsonProperty("mobile") @Nullable String mobile,
        @JsonProperty("email") @Nullable String email,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar
) {
}
