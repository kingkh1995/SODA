package com.soda.user.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.api.command.Command;

/**
 * 注销用户命令 —— 终态迁移，前置必须禁用态 D；R 为吸收态，此后无任何操作（见 ADR-0017）。
 * 键释放（三键置空 + 归档审计）由基础设施表示决策负责，领域不感知（见 ADR-0023）。
 */
public record DeregisterUserCommand(
        @JsonProperty("userId") Long userId
) implements Command {
}
