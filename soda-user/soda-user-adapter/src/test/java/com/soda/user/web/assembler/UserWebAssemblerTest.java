package com.soda.user.web.assembler;

import com.soda.user.api.dto.UserDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserWebAssembler 转换器")
class UserWebAssemblerTest {

    private UserWebAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = Mappers.getMapper(UserWebAssembler.class);
    }

    @Nested
    @DisplayName("Request → Command")
    class RequestToCommand {

        @Test
        @DisplayName("CreateUserRequest 映射到 CreateUserCommand")
        void should_mapCreateUserRequest_when_toCreateCommand() {
            var request = new CreateUserRequest("testuser", "test1234", "测试用户", "13800138000", "test@example.com", "1", null);

            var cmd = assembler.toCreateCommand(request);

            assertThat(cmd.username()).isEqualTo("testuser");
            assertThat(cmd.password()).isEqualTo("test1234");
            assertThat(cmd.nickname()).isEqualTo("测试用户");
            assertThat(cmd.mobile()).isEqualTo("13800138000");
            assertThat(cmd.email()).isEqualTo("test@example.com");
            assertThat(cmd.sex()).isEqualTo("1");
            assertThat(cmd.avatar()).isNull();
        }

        @Test
        @DisplayName("CreateUserRequest 可空字段为 null 时正常映射")
        void should_mapCreateUserRequest_when_nullableFieldsAreNull() {
            var request = new CreateUserRequest("testuser", "test1234", "测试用户", null, null, null, null);

            var cmd = assembler.toCreateCommand(request);

            assertThat(cmd.username()).isEqualTo("testuser");
            assertThat(cmd.password()).isEqualTo("test1234");
            assertThat(cmd.nickname()).isEqualTo("测试用户");
            assertThat(cmd.mobile()).isNull();
            assertThat(cmd.email()).isNull();
            assertThat(cmd.sex()).isNull();
            assertThat(cmd.avatar()).isNull();
        }

        @Test
        @DisplayName("UpdateUserRequest 映射到 UpdateUserCommand（userId 来自额外参数）")
        void should_mapUpdateUserRequest_when_toUpdateCommand() {
            var request = new UpdateUserRequest("新昵称", "2", null);

            var cmd = assembler.toUpdateCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.nickname()).isEqualTo("新昵称");
            assertThat(cmd.sex()).isEqualTo("2");
            assertThat(cmd.avatar()).isNull();
        }

        @Test
        @DisplayName("ChangePasswordRequest 映射到 ChangePasswordCommand（userId 来自额外参数）")
        void should_mapChangePasswordRequest_when_toChangePasswordCommand() {
            var request = new ChangePasswordRequest("oldPass123", "newPass123");

            var cmd = assembler.toChangePasswordCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.oldPassword()).isEqualTo("oldPass123");
            assertThat(cmd.newPassword()).isEqualTo("newPass123");
        }

        @Test
        @DisplayName("DisableUserCommand 由用户 ID 构建")
        void should_mapDisableUser_when_toDisableCommand() {
            var cmd = assembler.toDisableCommand(42L);

            assertThat(cmd.userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("EnableUserCommand 由用户 ID 构建")
        void should_mapEnableUser_when_toEnableCommand() {
            var cmd = assembler.toEnableCommand(42L);

            assertThat(cmd.userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("ChangeUsernameRequest 映射到 ChangeUsernameCommand（userId 来自额外参数）")
        void should_mapChangeUsernameRequest_when_toChangeUsernameCommand() {
            var request = new ChangeUsernameRequest("newAdmin");

            var cmd = assembler.toChangeUsernameCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.newUsername()).isEqualTo("newAdmin");
        }

        @Test
        @DisplayName("DeregisterUserCommand 由用户 ID 构建")
        void should_mapDeregisterUser_when_toDeregisterCommand() {
            var cmd = assembler.toDeregisterCommand(42L);

            assertThat(cmd.userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("RequestChangeMobileCodeRequest 映射到 RequestChangeMobileCodeCommand（userId 来自路径）")
        void should_mapRequestChangeMobileCodeRequest_when_toRequestChangeMobileCodeCommand() {
            var request = new RequestChangeMobileCodeRequest("13900139000");
            var cmd = assembler.toRequestChangeMobileCodeCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.newMobile()).isEqualTo("13900139000");
        }

        @Test
        @DisplayName("ChangeMobileRequest 映射到 ChangeMobileCommand（userId 来自额外参数）")
        void should_mapChangeMobileRequest_when_toChangeMobileCommand() {
            var request = new ChangeMobileRequest("123456");
            var cmd = assembler.toChangeMobileCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.code()).isEqualTo("123456");
        }

        @Test
        @DisplayName("RequestChangeEmailCodeRequest 映射到 RequestChangeEmailCodeCommand（userId 来自路径）")
        void should_mapRequestChangeEmailCodeRequest_when_toRequestChangeEmailCodeCommand() {
            var request = new RequestChangeEmailCodeRequest("new@test.com");
            var cmd = assembler.toRequestChangeEmailCodeCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.newEmail()).isEqualTo("new@test.com");
        }

        @Test
        @DisplayName("ChangeEmailRequest 映射到 ChangeEmailCommand（userId 来自额外参数）")
        void should_mapChangeEmailRequest_when_toChangeEmailCommand() {
            var request = new ChangeEmailRequest("123456");
            var cmd = assembler.toChangeEmailCommand(1L, request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.code()).isEqualTo("123456");
        }
    }

    @Nested
    @DisplayName("DTO → Response")
    class DtoToResponse {

        @Test
        @DisplayName("UserDTO 映射到 UserResponse")
        void should_mapUserDto_when_toResponse() {
            var dto = new UserDTO(1L, "testuser", "测试用户", "13800138000", "test@example.com", "1", null, "E", 3);

            var response = assembler.toResponse(dto);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.nickname()).isEqualTo("测试用户");
            assertThat(response.mobile()).isEqualTo("13800138000");
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.sex()).isEqualTo("1");
            assertThat(response.avatar()).isNull();
            assertThat(response.state()).isEqualTo("E");
            assertThat(response.version()).isEqualTo(3);
        }

        @Test
        @DisplayName("UserDTO 列表映射到 UserResponse 列表")
        void should_mapUserDtoList_when_toResponseList() {
            var dtos = List.of(
                    new UserDTO(1L, "user1", "用户1", "13800138000", "u1@test.com", "1", null, "E", 1),
                    new UserDTO(2L, "user2", "用户2", "13900139000", "u2@test.com", "2", "avatar.png", "D", 2)
            );

            var resps = assembler.toResponseList(dtos);

            assertThat(resps).hasSize(2);
            assertThat(resps.get(0).id()).isEqualTo(1L);
            assertThat(resps.get(0).username()).isEqualTo("user1");
            assertThat(resps.get(0).version()).isEqualTo(1);
            assertThat(resps.get(1).id()).isEqualTo(2L);
            assertThat(resps.get(1).username()).isEqualTo("user2");
            assertThat(resps.get(1).version()).isEqualTo(2);
        }
    }
}
