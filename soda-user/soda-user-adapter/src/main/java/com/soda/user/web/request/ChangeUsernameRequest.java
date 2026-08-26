package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 修改账号请求体 —— 新用户名（4-30 位字母数字，与 Username DP 同形）。
 */
public record ChangeUsernameRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$")
        @JsonProperty("newUsername") String newUsername
) {
}
