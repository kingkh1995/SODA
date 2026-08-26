package com.soda.user.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * 用户聚合出站 DTO —— 用户资料的协议形状（裸字符串值）。
 * <p>
 * username 恒非空（R 行恢复占位值 {@code Username.REMOVED}，见 ADR-0023）；
 * mobile/email/sex/avatar 可空（可选资料，R 态键释放后为 null）；
 * state 为 {@code UserState} 助记码；version 为乐观锁版本令牌。
 */
public record UserDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("nickname") String nickname,
        @JsonProperty("mobile") @Nullable String mobile,
        @JsonProperty("email") @Nullable String email,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar,
        @JsonProperty("state") String state,
        @JsonProperty("version") int version
) {
}
