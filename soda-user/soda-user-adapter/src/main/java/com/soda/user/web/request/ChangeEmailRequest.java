package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 换绑邮箱请求体（两步验证第二步）—— 仅携带邮箱验证码，目标邮箱隐含在验证记录中。
 */
public record ChangeEmailRequest(
        @NotBlank @Size(min = 8, max = 8)
        @JsonProperty("code") String code
) {
}
