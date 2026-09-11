package com.soda.component.web.conditional;

import com.soda.component.web.HttpValidatorSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * HTTP 验证器响应头统一处理 —— 为 {@link HttpValidatorSource} 实现类补 {@code ETag} 头
 * （AIP-154；见 ADR-0039）。
 * <p>
 * 只写头、不改 body、不求值：Controller 只返回裸资源（ADR-0009），设头逻辑收敛于此，端点零样板；
 * 入站 {@code If-Match} 由 {@code IfMatchResolver} 归一化为 {@code @IfMatch} 参数 + 应用层同事务比对
 * （失配 → 412 {@code Precondition Failed}，由应用层守卫直抛
 * {@link com.soda.component.api.error.PreconditionFailedException#versionMismatch}，见 ADR-0037 / ADR-0039）。
 * 无 GET 端点故无 {@code If-None-Match}/304。
 * <p>
 * 经 {@code IfMatchAutoConfiguration} 导入：组件 adapter-starter-web 位于运行时类路径即生效。
 * webmvc 切片下 {@code ResponseBodyAdvice} 类型不可见时不注册（见 framework-conventions §3.2）。
 */
@ControllerAdvice(basePackages = "com.soda")
public class HttpValidatorHeadersAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return HttpValidatorSource.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof HttpValidatorSource validators) {
            response.getHeaders().setETag(Integer.toString(validators.version()));
        }
        return body;
    }
}
