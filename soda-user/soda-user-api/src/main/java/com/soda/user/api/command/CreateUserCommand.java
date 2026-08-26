package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;
import org.jspecify.annotations.Nullable;

/**
 * 创建用户命令 —— username/password/nickname 必填，mobile/email/sex/avatar 可选。
 * <p>
 * Command 不携带校验注解；格式校验由 AppService 构造领域 DP 时进行
 * （领域规则留在 DP 构造器）。
 */
public record CreateUserCommand(
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("nickname") String nickname,
        @JsonProperty("mobile") @Nullable String mobile,
        @JsonProperty("email") @Nullable String email,
        @JsonProperty("sex") @Nullable String sex,
        @JsonProperty("avatar") @Nullable String avatar
) implements Command {
}
