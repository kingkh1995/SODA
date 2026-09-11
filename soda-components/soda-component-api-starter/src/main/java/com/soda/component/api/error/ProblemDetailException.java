package com.soda.component.api.error;

/**
 * api 契约层的失败信号 —— 自带 HTTP 状态码，由 starter-web 的 {@code ProblemDetailAdvice} 翻译为
 * RFC 9457 ProblemDetail（见 ADR-0013 / ADR-0015）。
 * <p>
 * 落点在 api 契约层：类型通用（不绑业务模块），状态码钉在类型上而非翻译层；只承载 {@code int}，
 * 不依赖 spring-web。子类一律私有构造 + 场景静态工厂，调用点读作「抛哪个场景」。
 */
public abstract class ProblemDetailException extends RuntimeException {

    /**
     * HTTP 状态码（4xx / 5xx）。
     */
    private final int status;

    /**
     * 子类构造入口 —— 状态码与客户端可见消息在此钉死。
     *
     * @param status  HTTP 状态码，限 4xx / 5xx
     * @param message 客户端可读的英文消息，成为 ProblemDetail 的 {@code detail}
     * @throws IllegalStateException status 不在 4xx / 5xx 时（子类写错状态码即编程错误）
     */
    protected ProblemDetailException(int status, String message) {
        super(message);
        if (status < 400 || status > 599) {
            throw new IllegalStateException("HTTP error status must be 4xx/5xx, but was " + status);
        }
        this.status = status;
    }

    /**
     * 返回该失败信号对应的 HTTP 状态码。
     *
     * @return 状态码（4xx / 5xx）
     */
    public final int status() {
        return status;
    }
}
