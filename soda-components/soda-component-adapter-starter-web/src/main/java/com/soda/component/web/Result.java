package com.soda.component.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * null 省略策略：{@code data} / {@code error} 为 null 时对应键整体省略（{@code @JsonInclude(NON_NULL)}），
 * 成功响应（{@link #success()}）与错误响应（{@link #error}）因此可能只含 {@code code} + {@code msg}。
 * 参照 Yudao {@code CommonResult} 设计。
 *
 * @param <T>     data 段类型
 * @param code    业务码（0 为成功）
 * @param msg     消息
 * @param data    数据段，可为 null（省略）
 * @param error   错误详情（AIP-193），成功时为 null（省略）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(
        @JsonProperty("code") int code,
        @JsonProperty("msg") String msg,
        @JsonProperty("data") @Nullable T data,
        @JsonProperty("error") @Nullable ErrorInfo error
) implements Serializable {

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MSG = "success";

    // ========== Factory ==========

    /**
     * 成功响应，含数据体。
     */
    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data, null);
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
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    /**
     * 错误响应，含 ErrorInfo（遵循 AIP-193）。
     */
    public static <T> Result<T> error(int code, String msg, ErrorInfo errorInfo) {
        return new Result<>(code, msg, null, errorInfo);
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
