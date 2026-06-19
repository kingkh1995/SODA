## Parent

`.scratch/soda-user/PRD.md`

## What to build

写侧 Controller 端点，接受 HTTP 请求 → DTO ↔ VO 转换 → 调 AppService → 返回 VO。

**VO 定义**（在 `soda-user-adapter` 模块）：
- `CreateUserVO` / `UpdateUserVO` / `UserDetailVO` 等— Controller 专属展现模型
- `UserAssembler` — DTO ↔ VO 互转

**Controller 端点：**

| 端点 | 方法 | AppService |
|------|------|------------|
| `POST /api/system/user/create` | `UserController.createUser(CreateUserVO)` → `UserCreateAppService` | 返回 `UserId` |
| `PUT /api/system/user/update` | `UserController.updateUser(UpdateUserVO)` → `UserUpdateAppService` | |
| `DELETE /api/system/user/delete` | `UserController.deleteUser(id)` → `UserDeleteAppService` | |
| `PUT /api/system/user/update-status` | `UserController.updateStatus(UpdateStatusVO)` → `UserStatusAppService` | |
| `PUT /api/system/user/update-password` | `UserController.updatePassword(UpdatePasswordVO)` → `UserPasswordAppService` | |
| `PUT /api/system/user/update-profile` | `UserController.updateProfile(UpdateProfileVO)` → `UserProfileAppService` | |
| `PUT /api/system/user/change-username` | `UserController.changeUsername(ChangeUsernameVO)` → `UsernameChangeAppService` | |

Controller 层职责：
1. 接收 VO → Assembler 转 Command → 调 AppService
2. 将 DTO 返回结果 → Assembler 转 VO → 返回客户端
3. 不含业务逻辑

## Acceptance criteria

- [ ] 所有 Controller 端点编译通过
- [ ] Controller 集成测试覆盖 create → update → status → password → profile → delete 全流程

## Blocked by

`10-user-password-persistence.md`
