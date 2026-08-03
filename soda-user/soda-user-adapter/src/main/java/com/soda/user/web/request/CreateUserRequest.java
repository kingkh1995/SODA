package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.types.Sex;
import com.soda.component.web.validation.EnumName;
import com.soda.component.web.validation.Mobile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$")
        @JsonProperty("username") String username,

        @NotBlank @Size(min = 6, max = 32)
        @JsonProperty("password") String password,

        @NotBlank @Size(max = 30)
        @JsonProperty("nickname") String nickname,

        @Nullable @Mobile @JsonProperty("mobile") String mobile,
        @Nullable @Email @JsonProperty("email") String email,
        @Nullable @EnumName(Sex.class) @JsonProperty("sex") String sex,
        @Nullable @URL @JsonProperty("avatar") String avatar
) {
}
