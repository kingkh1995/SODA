/**
 * 条件请求内部实现 —— 出站 {@code ETag} 头派生 + 入站 If-Match 参数解析（见 ADR-0039）。
 * <p>
 * 单入口 {@code IfMatchAutoConfiguration}（注册 + 导入）；由 Spring 自动发现，业务模块不直接引用；
 * 公开入口见根包 {@code HttpValidatorSource} 与 {@code IfMatch}。
 */
@NullMarked
package com.soda.component.web.conditional;

import org.jspecify.annotations.NullMarked;
