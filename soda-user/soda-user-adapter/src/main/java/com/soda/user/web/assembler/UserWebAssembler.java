package com.soda.user.web.assembler;

import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeleteUserCommand;
import com.soda.user.api.command.UpdatePasswordCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.command.UpdateUserStatusCommand;
import com.soda.user.api.dto.UserDTO;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.DeleteUserRequest;
import com.soda.user.web.request.UpdatePasswordRequest;
import com.soda.user.web.request.UpdateUserRequest;
import com.soda.user.web.request.UpdateUserStatusRequest;
import com.soda.user.web.response.UserResponse;
import org.mapstruct.Mapper;

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
@Mapper(componentModel = "spring")
public interface UserWebAssembler {

    // ========== Request → Command ==========

    CreateUserCommand toCommand(CreateUserRequest request);

    UpdateUserCommand toCommand(UpdateUserRequest request);

    UpdatePasswordCommand toCommand(UpdatePasswordRequest request);

    UpdateUserStatusCommand toCommand(UpdateUserStatusRequest request);

    ChangeUsernameCommand toCommand(ChangeUsernameRequest request);

    DeleteUserCommand toCommand(DeleteUserRequest request);

    // ========== DTO → Response ==========

    UserResponse toResponse(UserDTO dto);

    List<UserResponse> toResponse(List<UserDTO> dtos);
}
