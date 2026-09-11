package com.soda.user.web.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.web.HttpValidatorSource;
import org.jspecify.annotations.Nullable;

/**
 * 用户资料响应体 — 用户资料的协议形状（裸字符串值）。
 * <p>
 * 字段与 {@link com.soda.user.api.dto.UserDTO} 一一对应；可空语义同 UserDTO。
 * 实现 {@link HttpValidatorSource} 供条件请求强验证器原料（AIP-154；见 ADR-0039）。
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
) implements HttpValidatorSource {
}
