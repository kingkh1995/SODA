package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 请求发送换绑邮箱验证码命令 —— 邮箱变更两步验证第一步。
 * <p>
 * scene=UCC、channel=E 由方法语义隐式（客户端请求体零主体/场景概念，不可伪造，见 ADR-0026）；
 * {@code userId} 由 adapter 从路径参数 {@code {id}} 注入（资源级端点必填）；
 * {@code newEmail} 格式校验由 AppService 构造 {@code Email} 时进行（领域规则留在 DP 构造器）。
 *
 * @see com.soda.user.api.UserAuthService#requestChangeEmailCode
 */
public record RequestChangeEmailCodeCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newEmail") String newEmail
) implements Command {
}
