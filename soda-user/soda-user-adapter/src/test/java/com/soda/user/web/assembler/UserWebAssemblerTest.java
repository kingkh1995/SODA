package com.soda.user.web.assembler;

import com.soda.user.api.dto.UserDTO;
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
        void should_mapCreateUserRequest_when_toCommand() {
            var request = new com.soda.user.web.request.CreateUserRequest(
                    "admin", "123456", "管理员", "13800138000", "admin@test.com", "M", "http://avatar");
            var cmd = assembler.toCommand(request);

            assertThat(cmd.username()).isEqualTo("admin");
            assertThat(cmd.password()).isEqualTo("123456");
            assertThat(cmd.nickname()).isEqualTo("管理员");
            assertThat(cmd.mobile()).isEqualTo("13800138000");
            assertThat(cmd.email()).isEqualTo("admin@test.com");
            assertThat(cmd.sex()).isEqualTo("M");
            assertThat(cmd.avatar()).isEqualTo("http://avatar");
        }

        @Test
        @DisplayName("CreateUserRequest 可空字段为 null 时正常映射")
        void should_mapCreateUserRequest_when_nullableFieldsAreNull() {
            var request = new com.soda.user.web.request.CreateUserRequest(
                    "guest", "123456", "访客", null, null, null, null);
            var cmd = assembler.toCommand(request);

            assertThat(cmd.username()).isEqualTo("guest");
            assertThat(cmd.mobile()).isNull();
            assertThat(cmd.email()).isNull();
            assertThat(cmd.sex()).isNull();
            assertThat(cmd.avatar()).isNull();
        }

        @Test
        @DisplayName("UpdateUserRequest 映射到 UpdateUserCommand")
        void should_mapUpdateUserRequest_when_toCommand() {
            var request = new com.soda.user.web.request.UpdateUserRequest(
                    1L, "新昵称", "13900139000", null, null, null);
            var cmd = assembler.toCommand(request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.nickname()).isEqualTo("新昵称");
            assertThat(cmd.mobile()).isEqualTo("13900139000");
            assertThat(cmd.email()).isNull();
        }

        @Test
        @DisplayName("UpdatePasswordRequest 映射到 UpdatePasswordCommand")
        void should_mapUpdatePasswordRequest_when_toCommand() {
            var request = new com.soda.user.web.request.UpdatePasswordRequest(1L, "newPass123");
            var cmd = assembler.toCommand(request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.newPassword()).isEqualTo("newPass123");
        }

        @Test
        @DisplayName("UpdateUserStatusRequest 映射到 UpdateUserStatusCommand")
        void should_mapUpdateUserStatusRequest_when_toCommand() {
            var request = new com.soda.user.web.request.UpdateUserStatusRequest(1L, "D");
            var cmd = assembler.toCommand(request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.status()).isEqualTo("D");
        }

        @Test
        @DisplayName("ChangeUsernameRequest 映射到 ChangeUsernameCommand")
        void should_mapChangeUsernameRequest_when_toCommand() {
            var request = new com.soda.user.web.request.ChangeUsernameRequest(1L, "newAdmin");
            var cmd = assembler.toCommand(request);

            assertThat(cmd.userId()).isEqualTo(1L);
            assertThat(cmd.newUsername()).isEqualTo("newAdmin");
        }

        @Test
        @DisplayName("DeleteUserRequest 映射到 DeleteUserCommand")
        void should_mapDeleteUserRequest_when_toCommand() {
            var request = new com.soda.user.web.request.DeleteUserRequest(42L);
            var cmd = assembler.toCommand(request);

            assertThat(cmd.userId()).isEqualTo(42L);
        }
    }

    // ==================== DTO → Response ====================

    @Nested
    @DisplayName("DTO → Response")
    class DtoToResponse {

        @Test
        @DisplayName("UserDTO 映射到 UserResponse（全字段）")
        void should_mapUserDto_when_toResponse() {
            var dto = new UserDTO(1L, "admin", "管理员", "13800138000",
                    "admin@test.com", "M", "http://avatar", "E");
            var resp = assembler.toResponse(dto);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.username()).isEqualTo("admin");
            assertThat(resp.nickname()).isEqualTo("管理员");
            assertThat(resp.mobile()).isEqualTo("13800138000");
            assertThat(resp.email()).isEqualTo("admin@test.com");
            assertThat(resp.sex()).isEqualTo("M");
            assertThat(resp.avatar()).isEqualTo("http://avatar");
            assertThat(resp.status()).isEqualTo("E");
        }

        @Test
        @DisplayName("UserDTO 映射到 UserResponse（可空字段为 null）")
        void should_mapUserDto_when_nullableFieldsAreNull() {
            var dto = new UserDTO(1L, "guest", "访客", null, null, null, null, "E");
            var resp = assembler.toResponse(dto);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.mobile()).isNull();
            assertThat(resp.email()).isNull();
            assertThat(resp.sex()).isNull();
            assertThat(resp.avatar()).isNull();
        }

        @Test
        @DisplayName("UserDTO 列表映射到 UserResponse 列表")
        void should_mapUserDtoList_when_toResponseList() {
            var dtos = List.of(
                    new UserDTO(1L, "admin", "管理员", null, null, null, null, "E"),
                    new UserDTO(2L, "guest", "访客", null, null, null, null, "E")
            );
            var resps = assembler.toResponse(dtos);

            assertThat(resps).hasSize(2);
            assertThat(resps.get(0).id()).isEqualTo(1L);
            assertThat(resps.get(1).id()).isEqualTo(2L);
        }
    }
}
