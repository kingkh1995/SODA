package com.soda.user.web;

import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.dto.UserDTO;
import com.soda.user.web.assembler.UserWebAssembler;
import com.soda.user.web.request.ChangeEmailRequest;
import com.soda.user.web.request.ChangeMobileRequest;
import com.soda.user.web.request.ChangePasswordRequest;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.RequestChangeEmailCodeRequest;
import com.soda.user.web.request.RequestChangeMobileCodeRequest;
import com.soda.user.web.request.UpdateUserRequest;
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
    private UserAuthService userAuthService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userAuthService = mock(UserAuthService.class);
        var assembler = Mappers.getMapper(UserWebAssembler.class);
        controller = new UserController(assembler, userService, userAuthService);
    }

    @Nested
    @DisplayName("POST /")
    class CreateUser {

        @Test
        @DisplayName("创建用户成功返回用户对象")
        void should_returnUser_when_createUser() {
            var request = new CreateUserRequest("testuser", "test1234", "测试用户", "13800138000", "test@example.com", "1", null);
            var dto = new UserDTO(1L, "testuser", "测试用户", "13800138000", "test@example.com", "M", null, "E", 5);
            when(userService.createUser(any(CreateUserCommand.class))).thenReturn(dto);

            var response = controller.createUser(request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().id()).isEqualTo(1L);
            assertThat(response.data().username()).isEqualTo("testuser");
            assertThat(response.data().nickname()).isEqualTo("测试用户");
            assertThat(response.data().mobile()).isEqualTo("13800138000");
            assertThat(response.data().email()).isEqualTo("test@example.com");
            assertThat(response.data().sex()).isEqualTo("M");
            assertThat(response.data().state()).isEqualTo("E");
            assertThat(response.data().version()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("PATCH /{id}")
    class UpdateUser {

        @Test
        @DisplayName("更新用户成功返回空 data")
        void should_returnSuccess_when_updateUser() {
            var request = new UpdateUserRequest("新昵称", "2", null);

            var response = controller.updateUser(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("DELETE /{id}")
    class DeleteUser {

        @Test
        @DisplayName("删除成功返回空 data")
        void should_returnSuccess_when_deleteUser() {
            var response = controller.deleteUser(1L);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("POST /{id}:disable")
    class DisableUser {

        @Test
        @DisplayName("禁用用户成功返回空 data")
        void should_returnSuccess_when_disableUser() {
            var response = controller.disableUser(1L);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserService.disableUser 执行")
        void should_delegateToUserService_when_disableUser() {
            controller.disableUser(42L);

            verify(userService).disableUser(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:enable")
    class EnableUser {

        @Test
        @DisplayName("启用用户成功返回空 data")
        void should_returnSuccess_when_enableUser() {
            var response = controller.enableUser(1L);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserService.enableUser 执行")
        void should_delegateToUserService_when_enableUser() {
            controller.enableUser(42L);

            verify(userService).enableUser(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:changePassword")
    class ChangePassword {

        @Test
        @DisplayName("修改密码成功返回空 data")
        void should_returnSuccess_when_changePassword() {
            var request = new ChangePasswordRequest("newPass123");
            var response = controller.changePassword(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserAuthService.changePassword 执行")
        void should_delegateToUserAuthService_when_changePassword() {
            var request = new ChangePasswordRequest("newPass123");

            controller.changePassword(42L, request);

            verify(userAuthService).changePassword(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:changeUsername")
    class ChangeUsername {

        @Test
        @DisplayName("修改用户名成功返回空 data")
        void should_returnSuccess_when_changeUsername() {
            var request = new ChangeUsernameRequest("newAdmin");

            var response = controller.changeUsername(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserService.changeUsername 执行")
        void should_delegateToUserService_when_changeUsername() {
            var request = new ChangeUsernameRequest("newAdmin");

            controller.changeUsername(42L, request);

            verify(userService).changeUsername(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:requestChangeMobileCode")
    class RequestChangeMobileCode {

        @Test
        @DisplayName("发送验证码成功返回空 data")
        void should_returnSuccess_when_requestChangeMobileCode() {
            var request = new RequestChangeMobileCodeRequest("13900139000");
            var response = controller.requestChangeMobileCode(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserAuthService.requestChangeMobileCode 执行（userId 来自路径）")
        void should_delegateToUserAuthService_when_requestChangeMobileCode() {
            var request = new RequestChangeMobileCodeRequest("13900139000");

            controller.requestChangeMobileCode(42L, request);

            verify(userAuthService).requestChangeMobileCode(
                    new com.soda.user.api.command.RequestChangeMobileCodeCommand(42L, "13900139000"));
        }
    }

    @Nested
    @DisplayName("POST /{id}:changeMobile")
    class ChangeMobile {

        @Test
        @DisplayName("变更手机号成功返回空 data")
        void should_returnSuccess_when_changeMobile() {
            var request = new ChangeMobileRequest("123456");

            var response = controller.changeMobile(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserAuthService.changeMobile 执行")
        void should_delegateToUserAuthService_when_changeMobile() {
            var request = new ChangeMobileRequest("123456");

            controller.changeMobile(42L, request);

            verify(userAuthService).changeMobile(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:requestChangeEmailCode")
    class RequestChangeEmailCode {

        @Test
        @DisplayName("发送验证码成功返回空 data")
        void should_returnSuccess_when_requestChangeEmailCode() {
            var request = new RequestChangeEmailCodeRequest("new@test.com");
            var response = controller.requestChangeEmailCode(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserAuthService.requestChangeEmailCode 执行（userId 来自路径）")
        void should_delegateToUserAuthService_when_requestChangeEmailCode() {
            var request = new RequestChangeEmailCodeRequest("new@test.com");

            controller.requestChangeEmailCode(42L, request);

            verify(userAuthService).requestChangeEmailCode(
                    new com.soda.user.api.command.RequestChangeEmailCodeCommand(42L, "new@test.com"));
        }
    }

    @Nested
    @DisplayName("POST /{id}:changeEmail")
    class ChangeEmail {

        @Test
        @DisplayName("变更邮箱成功返回空 data")
        void should_returnSuccess_when_changeEmail() {
            var request = new ChangeEmailRequest("123456");

            var response = controller.changeEmail(1L, request);

            assertThat(response.code()).isZero();
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("委托 UserAuthService.changeEmail 执行")
        void should_delegateToUserAuthService_when_changeEmail() {
            var request = new ChangeEmailRequest("123456");

            controller.changeEmail(42L, request);

            verify(userAuthService).changeEmail(any());
        }
    }
}
