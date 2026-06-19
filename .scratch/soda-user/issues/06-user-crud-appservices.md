## Parent

`.scratch/soda-user/PRD.md`

## What to build

`soda-user-api` 中的 Command 和 DTO，以及 `soda-user-app` 中的 ApplicationService。所有 AppService 编排领域层调用，唯一性校验通过 `UserGateway.existsBy*()` 完成，持久化和事件发送委托 Gateway。

**API 模块：**
- `CreateUserCommand` — username, password, nickname, mobile?, email?, sex?, avatar?
- `UpdateUserCommand` — userId, nickname?, mobile?, email?, sex?, avatar?
- `UpdatePasswordCommand` — userId, newPassword
- `UpdateUserStatusCommand` — userId, status
- `UpdateProfileCommand` — userId, nickname?, mobile?, email?, sex?, avatar?（仅限用户个人信息修改）
- `ChangeUsernameCommand` — userId, newUsername
- `DeleteUserCommand` — userId
- `UserDTO` — 所有 User 字段的 DTO

**App 模块：**
- `UserCreateAppService` — 校验唯一性 → 构造 User（含 PasswordAccount） → `userGateway.save()` → `domainEventBus.fireAll(user.flushEvents())`
- `UserUpdateAppService` — 加载 → 更新字段 → save → fireAll
- `UserDeleteAppService` — 加载 → `userGateway.remove()` → fireAll（UserRemovedEvent publish 留 TODO）
- `UserStatusAppService` — 加载 → `user.changeStatus()` → save → fireAll
- `UserProfileAppService` — 加载 → `user.setMobile()/setEmail()` 等 → save → fireAll
- `UsernameChangeAppService` — 唯一性校验 → `user.changeUsername()` → save → fireAll
- `UserPasswordAppService` — 加载 → 更新 PasswordAccount → save → fireAll（PasswordChangedEvent publish 留 TODO）

所有 AppService 依赖 `UserGateway` + `DomainEventBus` + `PasswordEncoder`，通过构造器注入。

## Acceptance criteria

- [ ] 所有 Command 和 DTO 编译通过
- [ ] 所有 AppService 编译通过
- [ ] 单元测试使用 Mock Gateway 验证编排逻辑
- [ ] `createUser` 测试：成功创建 + 用户名重复抛异常
- [ ] `updateUserStatus` 测试：禁用后 UserStatusChangedEvent 在 flushEvents 中

## Blocked by

`04-user-account-entities.md` + `05-domain-events.md`
