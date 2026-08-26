package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.web.validation.MobileNumber;
import jakarta.validation.constraints.NotBlank;

/**
 * 换绑手机号发码请求体（两步验证第一步）—— 仅携带新手机号；userId 由路径参数注入。
 */
public record RequestChangeMobileCodeRequest(
        @NotBlank @MobileNumber
        @JsonProperty("newMobile") String newMobile
) {
}
