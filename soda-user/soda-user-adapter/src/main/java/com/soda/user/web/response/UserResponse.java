package com.soda.user.web.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * 用户资料响应体 —— {@code Result} 信封 {@code data} 段的形状（不含信封本身）。
 * <p>
 * 与 {@link com.soda.user.api.dto.UserDTO} 字段一一对应；可空语义同 UserDTO。
 */
public record UserResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("nickname") String nickname,
        @JsonProperty("mobile") @Nullable String mobile,
        @JsonProperty("email") @Nullable String email,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar,
        @JsonProperty("state") String state,
        @JsonProperty("version") int version
) {
}
