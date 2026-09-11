package com.soda.component.api.error;

/**
 * 资源不存在 —— <b>404</b>（RFC 9110 §15.5.5）；客户端动作：不重试（见 aip-api-conventions §8.2）。
 */
public final class NotFoundException extends ProblemDetailException {

    private NotFoundException(String message) {
        super(404, message);
    }

    /**
     * 实体未找到的失败信号。
     *
     * @param resource 资源 / 实体名（如 {@code User}），非 null
     * @param id       标识符裸值，非 null
     * @return 404 失败信号，非 null
     */
    public static NotFoundException entityNotFound(String resource, Object id) {
        return new NotFoundException(resource + " not found: " + id);
    }
}
