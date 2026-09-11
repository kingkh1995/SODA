package com.soda.user.web.assembler;

import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeregisterUserCommand;
import com.soda.user.api.command.DisableUserCommand;
import com.soda.user.api.command.EnableUserCommand;
import com.soda.user.api.command.RequestChangeEmailCommand;
import com.soda.user.api.command.RequestChangeMobileCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.dto.UserDTO;
import com.soda.user.web.request.ChangeEmailRequest;
import com.soda.user.web.request.ChangeMobileRequest;
import com.soda.user.web.request.ChangePasswordRequest;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.RequestChangeEmailRequest;
import com.soda.user.web.request.RequestChangeMobileRequest;
import com.soda.user.web.request.UpdateUserRequest;
import com.soda.user.web.response.UserResponse;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * {@code web/} 层的转换器，负责：
 * <ul>
 *   <li>入站：{@link com.soda.user.web.request Request → Command}</li>
 *   <li>出站：{@link UserDTO DTO → Response}</li>
 * </ul>
 * <p>
 * 由 MapStruct 在编译期生成实现，{@code componentModel = "spring"} 使实现为 Spring Bean。
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserWebAssembler {

    // ─── Request → Command ───

    CreateUserCommand toCreateCommand(CreateUserRequest request);

    /**
     * 入站映射（Update）—— {@code expectedVersion} 由 {@code @IfMatch} 参数注入（starter-web 归一化），不来自 Request。
     */
    UpdateUserCommand toUpdateCommand(Long userId, UpdateUserRequest request,
                                      @Nullable Integer expectedVersion);

    DeregisterUserCommand toDeregisterCommand(Long userId);

    DisableUserCommand toDisableCommand(Long userId);

    EnableUserCommand toEnableCommand(Long userId);

    ChangeUsernameCommand toChangeUsernameCommand(Long userId, ChangeUsernameRequest request);

    ChangePasswordCommand toChangePasswordCommand(Long userId, ChangePasswordRequest request);

    /**
     * 换绑手机号发码：scene=UCC、channel=S 由方法语义决定，非 Request 数据；
     * userId 取路径用户（资源级端点）。
     */
    RequestChangeMobileCommand toRequestChangeMobileCommand(
            Long userId, RequestChangeMobileRequest request);

    ChangeMobileCommand toChangeMobileCommand(Long userId, ChangeMobileRequest request);

    /**
     * 换绑邮箱发码：scene=UCC、channel=E 由方法语义决定，非 Request 数据；
     * userId 取路径用户（资源级端点）。
     */
    RequestChangeEmailCommand toRequestChangeEmailCommand(
            Long userId, RequestChangeEmailRequest request);

    ChangeEmailCommand toChangeEmailCommand(Long userId, ChangeEmailRequest request);

    // ─── DTO → Response ───

    /**
     * 出站映射（Create / Update / Query / List）—— 返回完整资源本身（AIP-134：无独立 UpdateResponse）。
     */
    UserResponse toResponse(UserDTO dto);

    List<UserResponse> toResponseList(List<UserDTO> dtos);

}
