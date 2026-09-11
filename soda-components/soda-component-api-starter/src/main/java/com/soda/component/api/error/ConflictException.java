package com.soda.component.api.error;

/**
 * 与资源当前状态冲突 —— <b>409</b>（RFC 9110 §15.5.10）；客户端动作：换值后重试（见 aip-api-conventions §8.2）。
 */
public final class ConflictException extends ProblemDetailException {

    private ConflictException(String message) {
        super(409, message);
    }

    /**
     * 唯一键已被占用的失败信号。
     *
     * @param field 冲突字段名（如 {@code Username}），非 null
     * @param value 冲突值，非 null
     * @return 409 失败信号，非 null
     */
    public static ConflictException alreadyExists(String field, Object value) {
        return new ConflictException(field + " already exists: " + value);
    }
}
