package com.soda.user.web;

import com.soda.component.web.Result;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.web.assembler.UserWebAssembler;
import com.soda.user.web.request.ChangeEmailRequest;
import com.soda.user.web.request.ChangeMobileRequest;
import com.soda.user.web.request.ChangePasswordRequest;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.RequestChangeEmailCodeRequest;
import com.soda.user.web.request.RequestChangeMobileCodeRequest;
import com.soda.user.web.request.UpdateUserRequest;
import com.soda.user.web.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 Controller — 接受 HTTP 请求，委托 {@link UserService} / {@link UserAuthService} 执行，返回 {@code Result} 信封。
 * <p>
 * 写操作用例路由：<br>
 * CRUD + 状态机 + 资料更新（create / update / disable / enable / deregister / changeUsername）→ {@link UserService}<br>
 * 凭证变更（changePassword / changeMobile / changeEmail / 关联发码）→ {@link UserAuthService}（见 ADR-0026）
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
    public Result<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        log.info("createUser: request={}", request);
        return Result.success(assembler.toResponse(userService.createUser(assembler.toCreateCommand(request))));
    }

    /**
     * 更新用户资料 —— 可选字段（nickname/sex/avatar）缺省即不修改。
     */
    @PatchMapping("/{id}")
    public Result<Void> updateUser(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid UpdateUserRequest request) {
        log.info("updateUser: id={}, request={}", id, request);
        userService.updateUser(assembler.toUpdateCommand(id, request));
        return Result.success();
    }

    /**
     * 注销用户 —— 终态迁移，前置必须禁用态；键释放由基础设施负责（见 ADR-0023）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> deregisterUser(@PathVariable("id") @Positive Long id) {
        log.info("deregisterUser: id={}", id);
        userService.deregisterUser(assembler.toDeregisterCommand(id));
        return Result.success();
    }

    /**
     * 禁用用户 —— AIP-136 自定义方法；已是禁用态则 no-op（见 ADR-0017）。
     */
    @PostMapping("/{id}:disable")
    public Result<Void> disableUser(@PathVariable("id") @Positive Long id) {
        log.info("disableUser: id={}", id);
        userService.disableUser(assembler.toDisableCommand(id));
        return Result.success();
    }

    /**
     * 启用用户 —— AIP-136 自定义方法；已是启用态则 no-op（见 ADR-0017）。
     */
    @PostMapping("/{id}:enable")
    public Result<Void> enableUser(@PathVariable("id") @Positive Long id) {
        log.info("enableUser: id={}", id);
        userService.enableUser(assembler.toEnableCommand(id));
        return Result.success();
    }

    /**
     * 修改账号 —— 新用户名全局唯一。
     */
    @PostMapping("/{id}:changeUsername")
    public Result<Void> changeUsername(@PathVariable("id") @Positive Long id,
                                       @RequestBody @Valid ChangeUsernameRequest request) {
        log.info("changeUsername: id={}, request={}", id, request);
        userService.changeUsername(assembler.toChangeUsernameCommand(id, request));
        return Result.success();
    }

    /**
     * 修改密码 —— 单步流程，域守卫校验原密码（见 ADR-0027）。
     */
    @PostMapping("/{id}:changePassword")
    public Result<Void> changePassword(@PathVariable("id") @Positive Long id,
                                       @RequestBody @Valid ChangePasswordRequest request) {
        log.info("changePassword: id={}, request={}", id, request);
        userAuthService.changePassword(assembler.toChangePasswordCommand(id, request));
        return Result.success();
    }

    /**
     * 请求换绑手机号验证码 —— UCC 两步验证第一步；目标 ≠ 当前值且全局唯一。
     */
    @PostMapping("/{id}:requestChangeMobileCode")
    public Result<Void> requestChangeMobileCode(@PathVariable("id") @Positive Long id,
                                                @RequestBody @Valid RequestChangeMobileCodeRequest request) {
        log.info("requestChangeMobileCode: id={}, request={}", id, request);
        userAuthService.requestChangeMobileCode(assembler.toRequestChangeMobileCodeCommand(id, request));
        return Result.success();
    }

    /**
     * 核验验证码并换绑手机号 —— UCC 两步验证第二步（verify → change → use 单事务）。
     */
    @PostMapping("/{id}:changeMobile")
    public Result<Void> changeMobile(@PathVariable("id") @Positive Long id,
                                     @RequestBody @Valid ChangeMobileRequest request) {
        log.info("changeMobile: id={}, request={}", id, request);
        userAuthService.changeMobile(assembler.toChangeMobileCommand(id, request));
        return Result.success();
    }

    /**
     * 请求换绑邮箱验证码 —— UCC 两步验证第一步；目标 ≠ 当前值且全局唯一。
     */
    @PostMapping("/{id}:requestChangeEmailCode")
    public Result<Void> requestChangeEmailCode(@PathVariable("id") @Positive Long id,
                                               @RequestBody @Valid RequestChangeEmailCodeRequest request) {
        log.info("requestChangeEmailCode: id={}, request={}", id, request);
        userAuthService.requestChangeEmailCode(assembler.toRequestChangeEmailCodeCommand(id, request));
        return Result.success();
    }

    /**
     * 核验验证码并换绑邮箱 —— UCC 两步验证第二步（verify → change → use 单事务）。
     */
    @PostMapping("/{id}:changeEmail")
    public Result<Void> changeEmail(@PathVariable("id") @Positive Long id,
                                    @RequestBody @Valid ChangeEmailRequest request) {
        log.info("changeEmail: id={}, request={}", id, request);
        userAuthService.changeEmail(assembler.toChangeEmailCommand(id, request));
        return Result.success();
    }
}
