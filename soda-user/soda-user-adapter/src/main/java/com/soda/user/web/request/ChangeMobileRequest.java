package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 换绑手机号请求体（两步验证第二步）—— 仅携带短信验证码，目标手机号隐含在验证记录中。
 */
public record ChangeMobileRequest(
        @NotBlank @Size(min = 6, max = 6)
        @JsonProperty("code") String code
) {
}
