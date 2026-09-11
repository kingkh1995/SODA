package com.soda.component.api.error;

/**
 * 条件请求前置未满足 —— <b>412</b>（RFC 9110 §15.5.13「请求头字段中的条件求值为假」）。
 * <p>
 * 见 ADR-0037 / ADR-0039：版本失配由应用层守卫在同一事务内先判，持久层 {@code @Version} 竞态兜底译 409。
 */
public final class PreconditionFailedException extends ProblemDetailException {

    private PreconditionFailedException(String message) {
        super(412, message);
    }

    /**
     * {@code If-Match} 与当前版本不符的失败信号。
     *
     * @param expected 客户端期望版本
     * @param current  当前版本
     * @return 412 失败信号，非 null
     */
    public static PreconditionFailedException versionMismatch(int expected, int current) {
        return new PreconditionFailedException("If-Match " + expected + " does not match current " + current);
    }
}
