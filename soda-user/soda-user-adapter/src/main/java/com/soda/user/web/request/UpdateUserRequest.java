package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.types.Sex;
import com.soda.component.web.validation.EnumName;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

public record UpdateUserRequest(
        @Nullable @JsonProperty("nickname") String nickname,
        @Nullable @EnumName(Sex.class) @JsonProperty("sex") String sex,
        @Nullable @URL @JsonProperty("avatar") String avatar
) {
}
