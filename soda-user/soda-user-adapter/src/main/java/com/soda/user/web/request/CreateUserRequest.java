package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.types.Sex;
import com.soda.component.web.validation.EnumName;
import com.soda.component.web.validation.MobileNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

/**
 * 创建用户请求体 —— 必填 username/password/nickname，可选 mobile/email/sex/avatar。
 * <p>
 * 协议格式校验（JSR 380）在本层完成；领域规则同形保留在 DP 构造器（与 Command 校验职责分离）。
 */
public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$")
        @JsonProperty("username") String username,

        @NotBlank @Size(min = 6, max = 32)
        @JsonProperty("password") String password,

        @NotBlank @Size(max = 30)
        @JsonProperty("nickname") String nickname,

        @Nullable @MobileNumber @JsonProperty("mobile") String mobile,
        @Nullable @Email @JsonProperty("email") String email,
        @Nullable @EnumName(Sex.class) @JsonProperty("sex") String sex,
        @Nullable @URL @JsonProperty("avatar") String avatar
) {
}
