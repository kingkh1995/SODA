package com.soda.component.web.error;

import com.soda.component.api.error.ProblemDetailException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * 项目异常 → RFC 9457 ProblemDetail 的翻译单源（见 ADR-0013 / ADR-0015）。语义完全由 HTTP 状态码承载，
 * 不设 {@code type} 自定义词表。
 * <p>
 * 继承 {@link ResponseEntityExceptionHandler}：框架异常（请求体校验、类型转换、媒体类型、方法不支持等）
 * 由继承的 handler 一并翻译；该继承同时抑制 Boot 的 {@code ProblemDetailsExceptionHandler}
 * （其 {@code @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)}），全链路只此一个 advice。
 * <p>
 * 项目自己的映射：{@link ProblemDetailException} 取类型自带的状态码（404 / 409 / 412）、
 * {@link IllegalArgumentException}（格式 / 值不可表示通道，见 ADR-0015）与
 * {@link ConstraintViolationException}（类级 {@code @Validated} 的 AOP 方法校验，框架 handler 清单不含它）
 * 译 400、{@link OptimisticLockingFailureException}（持久层 {@code @Version} 竞态）译 409 ——
 * 均以异常消息为 {@code detail}（客户端反馈通道）。其余未捕获异常译 500，{@code detail} 用固定文案
 * 不泄露内部信息，仅记日志。
 * <p>
 * 经 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册：
 * 组件 adapter-starter-web 位于运行时类路径即生效。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ControllerAdvice(basePackages = "com.soda")
@Slf4j
public class ProblemDetailAdvice extends ResponseEntityExceptionHandler {

    /**
     * 项目失败信号 -> 状态码由类型自带。
     */
    @ExceptionHandler(ProblemDetailException.class)
    public ProblemDetail handleProblemDetailException(ProblemDetailException ex, WebRequest request) {
        log.warn("业务拒绝: {} {}", ex.status(), ex.getMessage());
        return problem(ex.status(), ex.getMessage(), request);
    }

    /**
     * 参数非法 / 方法校验失败 -> 400。
     */
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex, WebRequest request) {
        log.warn("非法请求: {}", ex.getMessage());
        return problem(400, ex.getMessage(), request);
    }

    /**
     * 持久层乐观锁竞态 -> 409（客户端可重读后重试）。
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("乐观锁冲突: {}", ex.getMessage());
        return problem(409, "Concurrent modification", request);
    }

    /**
     * 未捕获异常（含防御编程守卫的裸 ISE） -> 500。
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAnyException(Exception ex, WebRequest request) {
        log.error("未捕获异常: ", ex);
        return problem(500, "Internal server error", request);
    }

    /**
     * ProblemDetail 组装单源：{@code title} 由 {@code forStatus} 按状态码派生（ReasonPhrase，不显式设置），
     * {@code detail} 由调用方给出（英文，不本地化），{@code instance} 取请求 URI。
     */
    private static ProblemDetail problem(int status, String detail, WebRequest request) {
        var problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return problemDetail;
    }
}
