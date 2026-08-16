package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 请求发送换绑邮箱验证码命令 — 邮箱变更两步验证第一步（2026-08-16，见 ADR-0026）。
 * <p>
 * per-use-case 命令（{@code RequestCodeCommand} 已删除——api 按用例拆分，scene/channel 由
 * 方法语义隐式，客户端不可伪造）；{@code userId} 由 adapter 从认证会话解析注入（<b>客户端
 * 请求体零主体概念</b>，UCC 恒认证态必填）；{@code newEmail} 格式校验由 AppService 构造
 * {@code Email} 时进行（领域规则留在 DP 构造器）。
 *
 * @see com.soda.user.api.UserAuthService#requestChangeEmailCode
 */
public record RequestChangeEmailCodeCommand(
        @JsonProperty("userId") Long userId,
        @JsonProperty("newEmail") String newEmail
) implements Command {
}
