## Parent

`.scratch/soda-user/PRD.md`

## What to build

写侧 Controller 端点，接受 HTTP 请求 → DTO ↔ VO 转换 → 调 AppService → 返回 VO。

**VO 定义**（在 `soda-user-adapter` 模块）：
- `CreateUserVO` / `UpdateUserVO` / `UserDetailVO` 等— Controller 专属展现模型
- `UserAssembler` — DTO ↔ VO 互转

**Controller 端点**（统一前缀 `/api/users`，命名遵循 ADR-0012 AIP camelCase）：

| 端点 | 方法 | Service |
|------|------|---------|
| `POST /api/users` | `UserController.createUser(CreateUserRequest)` → `UserService.createUser` | 返回 `UserId` |
| `PATCH /api/users/{id}` | `UserController.updateUser(UpdateUserRequest)` → `UserService.updateUser` | |
| `DELETE /api/users/{id}` | `UserController.deleteUser(id)` → `UserService.deleteUser` | |
| `POST /api/users/{id}:disable` | `UserController.disableUser(id)` → `UserService.disableUser` | |
| `POST /api/users/{id}:enable` | `UserController.enableUser(id)` → `UserService.enableUser` | |
| `POST /api/users/{id}:changePassword` | `UserController.changePassword(ChangePasswordRequest)` → `UserAuthService.changePassword` | |
| `POST /api/users/{id}:changeUsername` | `UserController.changeUsername(ChangeUsernameRequest)` → `UserService.changeUsername` | |
| `POST /api/users/{id}:verifyMobile` | `UserController.verifyMobile(VerifyMobileRequest)` → `UserAuthService.verifyMobile` | |
| `POST /api/users/{id}:changeMobile` | `UserController.changeMobile(ChangeMobileRequest)` → `UserAuthService.changeMobile` | |
| `POST /api/users/{id}:verifyEmail` | `UserController.verifyEmail(VerifyEmailRequest)` → `UserAuthService.verifyEmail` | |
| `POST /api/users/{id}:changeEmail` | `UserController.changeEmail(ChangeEmailRequest)` → `UserAuthService.changeEmail` | |

> 历史端点（`/api/system/user/create` 等）已被 AIP 风格替换（ADR-0012），不再实现。

Controller 层职责：
1. 接收 VO → Assembler 转 Command → 调 AppService
2. 将 DTO 返回结果 → Assembler 转 VO → 返回客户端
3. 不含业务逻辑

## Acceptance criteria

- [ ] 所有 Controller 端点编译通过
- [ ] Controller 集成测试覆盖 create → update → status → password → profile → delete 全流程

## Blocked by

`10-user-password-persistence.md`
