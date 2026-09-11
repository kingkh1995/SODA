/**
 * REST Controller — 接受 HTTP 请求，委托 api 层接口执行。
 * <p>
 * 写操作用例路由：CRUD + 状态机 + 资料更新（create / update / disable / enable / deregister / changeUsername）→ {@link com.soda.user.api.UserService}<br>
 * 凭证变更（changePassword / changeMobile / changeEmail / 关联发码）→ {@link com.soda.user.api.UserAuthService}（见 ADR-0026）
 * <p>
 * 错误响应由 starter-web {@code ProblemDetailAdvice} 统一翻译为 RFC 9457 ProblemDetail
 * （状态码承载语义，见 ADR-0013）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
