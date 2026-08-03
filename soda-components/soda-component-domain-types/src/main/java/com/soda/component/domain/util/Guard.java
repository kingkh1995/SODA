package com.soda.component.domain.util;

/**
 * 防御性编程守卫工具类 — 防御状态检查，失败抛出 {@link IllegalStateException}（无消息）。
 * <p>
 * 与 {@link ValidateUtils}（参数校验 → {@link IllegalArgumentException} 带消息）互补：
 * <ul>
 *   <li>参数/调用方校验（错误码语义，客户端可读）→ {@link ValidateUtils} / {@code Assert.isTrue(cond, msg)}</li>
 *   <li>防御状态守卫（不变量破坏，映射 500，不承诺客户端反馈）→ {@link #state(boolean)}</li>
 *   <li>null 守卫 → {@link java.util.Objects#requireNonNull}（JEP 358 自动帮助消息）</li>
 * </ul>
 * 见 ADR-0015 异常类使用约定。
 */
public final class Guard {

    private Guard() {
        throw new UnsupportedOperationException();
    }

    /**
     * 防御状态守卫：条件不成立抛 {@link IllegalStateException}（无消息）。
     * <p>
     * ISE 映射 HTTP 500，消息不承诺给客户端，因此不要求调用方提供消息。
     * 需要开发定位消息时，调用方自行使用 {@code Assert.state(condition, message)}。
     *
     * @param condition 必须成立的前置状态
     * @throws IllegalStateException condition 为 false
     */
    public static void state(boolean condition) {
        if (!condition) {
            throw new IllegalStateException();
        }
    }
}
