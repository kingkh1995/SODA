package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求体 —— 原密码 + 新密码（原密码比对为域守卫，见 ADR-0027）。
 */
public record ChangePasswordRequest(
        @NotBlank @Size(min = 6, max = 32)
        @JsonProperty("oldPassword") String oldPassword,
        @NotBlank @Size(min = 6, max = 32)
        @JsonProperty("newPassword") String newPassword
) {
}
