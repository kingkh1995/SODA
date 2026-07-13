package com.soda.user.web;

import com.soda.component.web.Result;
import com.soda.user.api.UserService;
import com.soda.user.web.assembler.UserWebAssembler;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.DeleteUserRequest;
import com.soda.user.web.request.UpdatePasswordRequest;
import com.soda.user.web.request.UpdateUserRequest;
import com.soda.user.web.request.UpdateUserStatusRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 Controller — 接受 HTTP 请求，委托 {@link UserService} 执行，返回统一信封。
 * <p>
 * 出入参使用 {@code web/request/} 和 {@code web/response/} 的专用类型，
 * 通过 {@link UserWebAssembler} 与 {@code api/} 模块的 Command / DTO 转换。
 */
@Slf4j
@RestController
@RequestMapping("/api/system/user")
public class UserController {

    private final UserService userService;
    private final UserWebAssembler assembler;

    public UserController(UserService userService, UserWebAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @PostMapping("/create")
    public Result<Long> createUser(@RequestBody @Valid CreateUserRequest request) {
        log.info("createUser: username={}", request.username());
        var cmd = assembler.toCommand(request);
        return Result.success(userService.createUser(cmd));
    }

    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody @Valid UpdateUserRequest request) {
        log.info("updateUser: {}", request);
        var cmd = assembler.toCommand(request);
        userService.updateUser(cmd);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<Void> deleteUser(@RequestBody @Valid DeleteUserRequest request) {
        log.info("deleteUser: {}", request);
        var cmd = assembler.toCommand(request);
        userService.deleteUser(cmd);
        return Result.success();
    }

    @PutMapping("/update-status")
    public Result<Void> updateStatus(@RequestBody @Valid UpdateUserStatusRequest request) {
        log.info("updateStatus: {}", request);
        var cmd = assembler.toCommand(request);
        userService.updateStatus(cmd);
        return Result.success();
    }

    @PutMapping("/update-password")
    public Result<Void> changePassword(@RequestBody @Valid UpdatePasswordRequest request) {
        log.info("changePassword: userId={}", request.userId());
        var cmd = assembler.toCommand(request);
        userService.changePassword(cmd);
        return Result.success();
    }

    @PutMapping("/change-username")
    public Result<Void> changeUsername(@RequestBody @Valid ChangeUsernameRequest request) {
        log.info("changeUsername: {}", request);
        var cmd = assembler.toCommand(request);
        userService.changeUsername(cmd);
        return Result.success();
    }
}
