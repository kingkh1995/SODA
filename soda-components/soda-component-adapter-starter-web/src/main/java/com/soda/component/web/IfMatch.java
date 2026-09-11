package com.soda.component.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If-Match 请求头注入 —— 归一化为乐观锁期望版本（AIP-154 / RFC 7232；见 ADR-0039）。
 * <p>
 * 标注 {@code Integer} 入参；缺失 / 空 / 通配（{@code *}）→ null（是否放行由应用层决定，见 {@code UserServiceImpl}）；
 * 取逗号列表首值；弱标签（{@code W/}）→ 412；非数字 → 400。版本比对不在此，由应用层同一事务内完成。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IfMatch {
}
