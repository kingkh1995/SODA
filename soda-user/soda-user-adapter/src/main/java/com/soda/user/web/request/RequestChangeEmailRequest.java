package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 换绑邮箱发码请求体（两步验证第一步）—— 仅携带新邮箱；userId 由路径参数注入。
 */
public record RequestChangeEmailRequest(
        @NotBlank @Email
        @JsonProperty("newEmail") String newEmail
) {
}
