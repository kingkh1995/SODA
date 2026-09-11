package com.soda.user.web;

import com.soda.component.web.IfMatch;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.web.assembler.UserWebAssembler;
import com.soda.user.web.request.ChangeEmailRequest;
import com.soda.user.web.request.ChangeMobileRequest;
import com.soda.user.web.request.ChangePasswordRequest;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.RequestChangeEmailRequest;
import com.soda.user.web.request.RequestChangeMobileRequest;
import com.soda.user.web.request.UpdateUserRequest;
import com.soda.user.web.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 Controller — 接受 HTTP 请求，委托 {@link UserService} / {@link UserAuthService} 执行。
 * <p>
 * 写操作用例路由：
 * 标准方法（create / update）+ 领域行为（disable / enable / deregister / changeUsername 等，AIP-136 :verb）→ {@link UserService}
 * 凭证变更（changePassword / changeMobile / changeEmail / 关联发码）→ {@link UserAuthService}（见 ADR-0026）
 * <p>
 * 条件请求（AIP-154 / RFC 7232；见 ADR-0039）：{@code updateUser}/{@code createUser} 返回体实现
 * {@code HttpValidatorSource}，{@code HttpValidatorHeadersAdvice} 统一补 {@code ETag} 头（弱验证器
 * {@code Last-Modified} 不提供——审计列不入出站模型，见 ADR-0031）；
 * 可选 {@code If-Match} 头由 starter-web {@code IfMatchResolver} 归一化为 {@code @IfMatch} 参数（→ {@code expectedVersion}），版本比对在
 * {@code UserServiceImpl.updateUser} 同一事务内完成（失配 → 412 {@code Precondition Failed}；缺失/`*` = 放行，见 ADR-0037 / ADR-0039）。
 * 无 GET 端点（AIP-121 偏离，读走查询服务），故无 If-None-Match、无 304。
 */
@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserWebAssembler assembler;
    private final UserService userService;
    private final UserAuthService userAuthService;

    /**
     * 创建用户 —— username/mobile/email 唯一性由应用层前置检查，返回新用户资料。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        log.info("createUser: request={}", request);
        var dto = userService.createUser(assembler.toCreateCommand(request));
        return assembler.toResponse(dto);
    }

    /**
     * 更新用户资料 —— {@code updateMask} 控制实际生效字段（AIP-134 §3.5 / AIP-161）：省略 = 只写非 null
     * 字段，命中 nullable 字段时请求体 null 视同清空，{@code "*"} = 全量替换。响应返回完整资源本身
     * （AIP-134：无独立 UpdateResponse）。
     */
    @PatchMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid UpdateUserRequest request,
                                   @IfMatch Integer expectedVersion) {
        log.info("updateUser: id={}, request={}, expectedVersion={}", id, request, expectedVersion);
        var command = assembler.toUpdateCommand(id, request, expectedVersion);
        var dto = userService.updateUser(command);
        return assembler.toResponse(dto);
    }

    /**
     * 注销用户 —— AIP-136 领域行为方法（:deregister），终态迁移到吸收态 R，前置必须禁用态；
     * 键释放由基础设施负责（见 ADR-0023）。响应返回 R 态完整资源（领域终态快照，
     * 快照语义见 {@code UserService#deregisterUser}）。
     */
    @PostMapping("/{id}:deregister")
    public UserResponse deregisterUser(@PathVariable("id") @Positive Long id) {
        log.info("deregisterUser: id={}", id);
        var dto = userService.deregisterUser(assembler.toDeregisterCommand(id));
        return assembler.toResponse(dto);
    }

    /**
     * 禁用用户 —— AIP-136 自定义方法；已是禁用态则 no-op（见 ADR-0017）。
     */
    @PostMapping("/{id}:disable")
    public void disableUser(@PathVariable("id") @Positive Long id) {
        log.info("disableUser: id={}", id);
        userService.disableUser(assembler.toDisableCommand(id));
    }

    /**
     * 启用用户 —— AIP-136 自定义方法；已是启用态则 no-op（见 ADR-0017）。
     */
    @PostMapping("/{id}:enable")
    public void enableUser(@PathVariable("id") @Positive Long id) {
        log.info("enableUser: id={}", id);
        userService.enableUser(assembler.toEnableCommand(id));
    }

    /**
     * 修改账号 —— 新用户名全局唯一。
     */
    @PostMapping("/{id}:changeUsername")
    public void changeUsername(@PathVariable("id") @Positive Long id,
                               @RequestBody @Valid ChangeUsernameRequest request) {
        log.info("changeUsername: id={}, request={}", id, request);
        userService.changeUsername(assembler.toChangeUsernameCommand(id, request));
    }

    /**
     * 修改密码 —— 单步流程，域守卫校验原密码（见 ADR-0027）。
     */
    @PostMapping("/{id}:changePassword")
    public void changePassword(@PathVariable("id") @Positive Long id,
                               @RequestBody @Valid ChangePasswordRequest request) {
        log.info("changePassword: id={}, request={}", id, request);
        userAuthService.changePassword(assembler.toChangePasswordCommand(id, request));
    }

    /**
     * 请求换绑手机号验证码 —— UCC 两步验证第一步；目标 ≠ 当前值且全局唯一。
     */
    @PostMapping("/{id}:requestChangeMobile")
    public void requestChangeMobile(@PathVariable("id") @Positive Long id,
                                    @RequestBody @Valid RequestChangeMobileRequest request) {
        log.info("requestChangeMobile: id={}, request={}", id, request);
        userAuthService.requestChangeMobile(assembler.toRequestChangeMobileCommand(id, request));
    }

    /**
     * 核验验证码并换绑手机号 —— UCC 两步验证第二步（verify → change → use 单事务）。
     */
    @PostMapping("/{id}:changeMobile")
    public void changeMobile(@PathVariable("id") @Positive Long id,
                             @RequestBody @Valid ChangeMobileRequest request) {
        log.info("changeMobile: id={}, request={}", id, request);
        userAuthService.changeMobile(assembler.toChangeMobileCommand(id, request));
    }

    /**
     * 请求换绑邮箱验证码 —— UCC 两步验证第一步；目标 ≠ 当前值且全局唯一。
     */
    @PostMapping("/{id}:requestChangeEmail")
    public void requestChangeEmail(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid RequestChangeEmailRequest request) {
        log.info("requestChangeEmail: id={}, request={}", id, request);
        userAuthService.requestChangeEmail(assembler.toRequestChangeEmailCommand(id, request));
    }

    /**
     * 核验验证码并换绑邮箱 —— UCC 两步验证第二步（verify → change → use 单事务）。
     */
    @PostMapping("/{id}:changeEmail")
    public void changeEmail(@PathVariable("id") @Positive Long id,
                            @RequestBody @Valid ChangeEmailRequest request) {
        log.info("changeEmail: id={}, request={}", id, request);
        userAuthService.changeEmail(assembler.toChangeEmailCommand(id, request));
    }
}
