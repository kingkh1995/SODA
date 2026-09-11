package com.soda.component.web.conditional;

import com.soda.component.web.IfMatch;

import org.springframework.core.MethodParameter;
import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@link IfMatch} 注解解析器 —— 从请求头提取 If-Match 并归一化为期望版本（见 ADR-0039）。
 * <p>
 * 缺失 / 空 / 通配（{@code *}，含归一化后的 {@code W/*}）→ null（放行与否归应用层，本解析器不裁决）；
 * 取逗号列表首值（客户端只发单值，多值取首值）；弱标签（{@code W/}）永不强匹配 → 412；
 * 非数字 → 400。版本比对不在此，由应用层同一事务内完成。
 * <p>
 * 经 {@code IfMatchAutoConfiguration} 注册：组件 adapter-starter-web 位于运行时类路径即生效。
 */
public class IfMatchResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(IfMatch.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!parameter.getParameterType().equals(Integer.class)) {
            throw new IllegalStateException("@IfMatch 仅支持 Integer 参数，实际：" + parameter.getParameterType());
        }
        var values = webRequest.getHeaderValues(HttpHeaders.IF_MATCH);
        var headers = new HttpHeaders();
        if (values != null) {
            for (var value : values) {
                headers.add(HttpHeaders.IF_MATCH, value);
            }
        }
        var tags = headers.getIfMatch();
        if (tags.isEmpty()) {
            return null;
        }
        if (tags.contains("*")) {
            return null;
        }
        var first = ETag.create(tags.getFirst());
        if (first.weak()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "If-Match weak validator requires strong comparison: " + tags);
        }
        try {
            return Integer.valueOf(first.tag());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid If-Match value: " + tags);
        }
    }
}
