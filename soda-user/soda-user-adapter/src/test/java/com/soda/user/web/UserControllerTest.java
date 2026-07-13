package com.soda.user.web;

import com.soda.user.api.UserService;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.web.assembler.UserWebAssembler;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.DeleteUserRequest;
import com.soda.user.web.request.UpdatePasswordRequest;
import com.soda.user.web.request.UpdateUserRequest;
import com.soda.user.web.request.UpdateUserStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserController REST 接口")
class UserControllerTest {

    private UserService userService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserController(userService, Mappers.getMapper(UserWebAssembler.class));
    }

    @Nested
    @DisplayName("POST /create")
    class CreateUser {

        @Test
        @DisplayName("创建成功返回 userId")
        void should_returnUserId_when_createUser() {
            var request = new CreateUserRequest("admin", "123456", "管理员", null, null, null, null);
            when(userService.createUser(any(CreateUserCommand.class))).thenReturn(42L);

            var response = controller.createUser(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isEqualTo(42L);
            assertThat(response.message()).isEqualTo("success");
        }

        @Test
        @DisplayName("委托 UserService.createUser 执行")
        void should_delegateToUserService_when_createUser() {
            var request = new CreateUserRequest("admin", "123456", "管理员", null, null, null, null);

            controller.createUser(request);

            verify(userService).createUser(any(CreateUserCommand.class));
        }
    }

    @Nested
    @DisplayName("PUT /update")
    class UpdateUser {

        @Test
        @DisplayName("更新成功返回空 data")
        void should_returnSuccess_when_updateUser() {
            var request = new UpdateUserRequest(1L, "新昵称", null, null, null, null);

            var response = controller.updateUser(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("DELETE /delete")
    class DeleteUser {

        @Test
        @DisplayName("删除成功返回空 data")
        void should_returnSuccess_when_deleteUser() {
            var request = new DeleteUserRequest(1L);

            var response = controller.deleteUser(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("PUT /update-status")
    class UpdateStatus {

        @Test
        @DisplayName("更新状态成功返回空 data")
        void should_returnSuccess_when_updateStatus() {
            var request = new UpdateUserStatusRequest(1L, "D");

            var response = controller.updateStatus(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("PUT /update-password")
    class ChangePassword {

        @Test
        @DisplayName("修改密码成功返回空 data")
        void should_returnSuccess_when_changePassword() {
            var request = new UpdatePasswordRequest(1L, "newPass123");

            var response = controller.changePassword(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("PUT /change-username")
    class ChangeUsername {

        @Test
        @DisplayName("修改用户名成功返回空 data")
        void should_returnSuccess_when_changeUsername() {
            var request = new ChangeUsernameRequest(1L, "newAdmin");

            var response = controller.changeUsername(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserService.changeUsername 执行")
        void should_delegateToUserService_when_changeUsername() {
            var request = new ChangeUsernameRequest(42L, "newAdmin");

            controller.changeUsername(request);

            verify(userService).changeUsername(any());
        }
    }
}
