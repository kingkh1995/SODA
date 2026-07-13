package com.soda.component.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * 统一 API 操作结果信封。
 * <p>
 * 所有 REST Controller 的返回值必须用此类包裹，格式：
 * <pre>
 * { "code": 0, "msg": "success", "data": {…} }
 * </pre>
 * <p>
 * 参照 Yudao {@code CommonResult} 设计。
 *
 * @param <T> data 段类型
 */
public record Result<T>(
        @JsonProperty("code") int code,
        @JsonProperty("msg") String message,
        @JsonProperty("data") @Nullable T data
) implements Serializable {

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MSG = "success";

    // ========== Factory ==========

    /**
     * 成功响应，含数据体。
     */
    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    /**
     * 成功响应，无数据体（删除、修改密码等操作）。
     */
    public static Result<Void> success() {
        return success(null);
    }

    /**
     * 错误响应。
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // ========== Query ==========

    @JsonIgnore
    public boolean isSuccess() {
        return code == SUCCESS_CODE;
    }

    @JsonIgnore
    public boolean isError() {
        return !isSuccess();
    }
}
