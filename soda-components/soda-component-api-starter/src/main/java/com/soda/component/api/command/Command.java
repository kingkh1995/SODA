package com.soda.component.api.command;

/**
 * 命令接口标记（Marker）— 所有写操作入参的统一契约。
 * <p>
 * Record 实现此接口后，可被 {@code CommandExecutor}（未来模块）按类型统一派发，
 * 并支撑 AIP-163 的 validateOnly 语义（通过 {@link #validateOnly()} 声明，暂未有实现覆写，后续待实现）。
 *
 * <pre>{@code
 * public record CreateUserCommand(
 *     String username
 * ) implements Command {
 * }
 * }</pre>
 */
public interface Command {

    /**
     * 是否仅验证请求，不执行实际操作。
     *
     * @return {@code false}（默认），子类按需覆写
     */
    default boolean validateOnly() {
        return false;
    }
}
