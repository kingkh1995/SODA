package com.soda.user.web;

import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.api.command.RequestChangeEmailCommand;
import com.soda.user.api.command.RequestChangeMobileCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.dto.UserDTO;
import com.soda.user.web.assembler.UserWebAssembler;
import com.soda.user.web.request.ChangeEmailRequest;
import com.soda.user.web.request.ChangeMobileRequest;
import com.soda.user.web.request.ChangePasswordRequest;
import com.soda.user.web.request.ChangeUsernameRequest;
import com.soda.user.web.request.CreateUserRequest;
import com.soda.user.web.request.RequestChangeEmailRequest;
import com.soda.user.web.request.RequestChangeMobileRequest;
import com.soda.user.web.request.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

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

    private static UserDTO dto() {
        return new UserDTO(1L, "testuser", "Test", "13900139000", "test@test.com", null, null, "E", 0);
    }

    private static UserDTO removedDto() {
        return new UserDTO(1L, "testuser", "Test", "13900139000", "test@test.com", null, null, "R", 1);
    }

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userAuthService = mock(UserAuthService.class);
        UserWebAssembler assembler = Mappers.getMapper(UserWebAssembler.class);
        controller = new UserController(assembler, userService, userAuthService);
    }

    @Nested
    @DisplayName("POST /")
    class CreateUser {
        @Test
        @DisplayName("应直接返回新建用户资料")
        void should_returnCreatedUser_when_validRequest() {
            var request = new CreateUserRequest("testuser", "password123", "Test",
                    "13900139000", "test@test.com", null, null);
            when(userService.createUser(any())).thenReturn(dto());

            var response = controller.createUser(request);

            assertThat(response.username()).isEqualTo("testuser");
            verify(userService).createUser(any());
        }
    }

    @Nested
    @DisplayName("PATCH /{id}")
    class UpdateUser {
        @Test
        @DisplayName("应直接返回更新后完整资源（省略 updateMask → 忽略 null 字段）")
        void should_returnFullResource_when_validRequest() {
            var request = new UpdateUserRequest(null, "新昵称", null, null);
            when(userService.updateUser(any())).thenReturn(dto());

            var response = controller.updateUser(42L, request, null);

            assertThat(response.id()).isEqualTo(1L);
            verify(userService).updateUser(any());
        }

        @Test
        @DisplayName("省略 updateMask → 忽略 null 字段（请求体透传 updateMask=null）")
        void should_ignoreNullFields_when_updateMaskOmitted() {
            var request = new UpdateUserRequest(null, "新昵称", null, null);
            when(userService.updateUser(any())).thenReturn(dto());

            controller.updateUser(42L, request, null);

            ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
            verify(userService).updateUser(captor.capture());
            assertThat(captor.getValue().updateMask()).isNull();
            assertThat(captor.getValue().expectedVersion()).isNull();
        }

        @Test
        @DisplayName("updateMask=\"\" → 视同省略（请求体透传空串）")
        void should_treatEmptyUpdateMask_asOmitted() {
            var request = new UpdateUserRequest("", "新昵称", null, null);
            when(userService.updateUser(any())).thenReturn(dto());

            controller.updateUser(42L, request, null);

            ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
            verify(userService).updateUser(captor.capture());
            assertThat(captor.getValue().updateMask()).isEqualTo("");
        }

        @Test
        @DisplayName("updateMask 命中 nullable 字段 + 请求 null → 由 service 负责清空，adapter 仅透传")
        void should_passThrough_when_maskHitsNullableFieldWithNull() {
            var request = new UpdateUserRequest("sex", null, null, null);
            when(userService.updateUser(any())).thenReturn(dto());

            var response = controller.updateUser(42L, request, null);

            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("expectedVersion 透传 service（由 @IfMatch 注入）")
        void should_passExpectedVersion_when_versionPresent() {
            var request = new UpdateUserRequest(null, "新昵称", null, null);
            when(userService.updateUser(any())).thenReturn(dto());

            controller.updateUser(42L, request, 3);

            ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
            verify(userService).updateUser(captor.capture());
            assertThat(captor.getValue().expectedVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("expectedVersion 缺失 → null 放行")
        void should_passNullExpectedVersion_when_versionMissing() {
            var request = new UpdateUserRequest(null, "新昵称", null, null);
            when(userService.updateUser(any())).thenReturn(dto());

            controller.updateUser(42L, request, null);

            ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
            verify(userService).updateUser(captor.capture());
            assertThat(captor.getValue().expectedVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("POST /{id}:deregister")
    class DeregisterUser {
        @Test
        @DisplayName("应委托注销用例并返回 R 态完整资源")
        void should_delegateDeregister_when_called() {
            when(userService.deregisterUser(any())).thenReturn(removedDto());

            var response = controller.deregisterUser(42L);

            assertThat(response.state()).isEqualTo("R");
            verify(userService).deregisterUser(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:disable")
    class DisableUser {
        @Test
        @DisplayName("应委托禁用用例")
        void should_delegateDisable_when_called() {
            controller.disableUser(42L);

            verify(userService).disableUser(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:enable")
    class EnableUser {
        @Test
        @DisplayName("应委托启用用例")
        void should_delegateEnable_when_called() {
            controller.enableUser(42L);

            verify(userService).enableUser(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:changePassword")
    class ChangePassword {
        @Test
        @DisplayName("应委托修改密码用例")
        void should_delegateChangePassword_when_called() {
            var request = new ChangePasswordRequest("oldpass123", "newpass123");

            controller.changePassword(42L, request);

            verify(userAuthService).changePassword(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:changeUsername")
    class ChangeUsername {
        @Test
        @DisplayName("应委托修改账号用例")
        void should_delegateChangeUsername_when_called() {
            var request = new ChangeUsernameRequest("newusername");

            controller.changeUsername(42L, request);

            verify(userService).changeUsername(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:requestChangeMobile")
    class RequestChangeMobile {
        @Test
        @DisplayName("应委托发码用例并携带目标手机号")
        void should_delegateRequestCode_when_called() {
            var request = new RequestChangeMobileRequest("13900139000");

            controller.requestChangeMobile(42L, request);

            verify(userAuthService).requestChangeMobile(new RequestChangeMobileCommand(42L, "13900139000"));
        }
    }

    @Nested
    @DisplayName("POST /{id}:changeMobile")
    class ChangeMobile {
        @Test
        @DisplayName("应委托换绑手机号用例")
        void should_delegateChangeMobile_when_called() {
            var request = new ChangeMobileRequest("123456");

            controller.changeMobile(42L, request);

            verify(userAuthService).changeMobile(any());
        }
    }

    @Nested
    @DisplayName("POST /{id}:requestChangeEmail")
    class RequestChangeEmail {
        @Test
        @DisplayName("应委托发码用例并携带目标邮箱")
        void should_delegateRequestCode_when_called() {
            var request = new RequestChangeEmailRequest("new@test.com");

            controller.requestChangeEmail(42L, request);

            verify(userAuthService).requestChangeEmail(new RequestChangeEmailCommand(42L, "new@test.com"));
        }
    }

    @Nested
    @DisplayName("POST /{id}:changeEmail")
    class ChangeEmail {
        @Test
        @DisplayName("应委托换绑邮箱用例")
        void should_delegateChangeEmail_when_called() {
            var request = new ChangeEmailRequest("12345678");

            controller.changeEmail(42L, request);

            verify(userAuthService).changeEmail(any());
        }
    }
}
