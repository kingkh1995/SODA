## Parent

`.scratch/soda-user/PRD.md`

## Status

**已完成** — 全部 Command/DTO + User 变更方法 + AppServices + 单元测试。

## What was built

### API 模块（8 个 record）

- `CreateUserCommand` — username, password, nickname, mobile?, email?, sex?, avatar?
- `UpdateUserCommand` — userId, nickname?, mobile?, email?, sex?, avatar?
- `UpdatePasswordCommand` — userId, newPassword
- `UpdateUserStatusCommand` — userId, status
- ~~`UpdateProfileCommand`~~ → 已合并到 `UpdateUserCommand`（重复）
- `ChangeUsernameCommand` — userId, newUsername
- `DeleteUserCommand` — userId
- `UserDTO` — 所有 User 字段的 DTO

### User.java 变更方法

- `addAccount(AuthAccount<?>)` — 添加认证账户到聚合
- `changeUsername(Username)` — 修改用户名
- `changeStatus(UserStatus)` — 修改状态，注册 `UserStatusChangedEvent`
- `setNickname(Nickname)` / `setMobile(Mobile)` / `setEmail(Email)` / `setSex(Sex)` / `setAvatar(Avatar)` — 字段设置

### App 模块（1 个 UserServiceImpl + 6 个方法）

- `UserService`（接口在 `soda-user-api`）→ `UserServiceImpl`（实现在 `soda-user-app`）

方法一览：
- `createUser` — 两阶段创建（先 save User 获得 UserId，再创建 PasswordAuthAccount 并 addAccount，最后 save + fireAll）
- `updateUser` — 加载 → 更新字段 → save → fireAll
- `deleteUser` — 加载 → remove → fire UserRemovedEvent
- `updateStatus` — 加载 → changeStatus → save → fireAll
- `changeUsername` — 唯一性校验 → changeUsername → save → fireAll
- `changePassword` — 加载 → find PasswordAccount → changePassword → save → fireAll（PasswordChangedEvent 通过 domainEventBus.fire() 发布）

**旧**：6 个 `*AppService`（已删除）

### build.gradle

- `soda-user-app/build.gradle` 补充 `soda-component-domain-starter` 和 `soda-component-support` 依赖
- `soda-user-app/package-info.java` 补充 `allowedDependencies = {"api", "domain"}`

## Acceptance criteria

- [x] 所有 7 个 Command/DTO 编译通过（原 8 个，删除重复的 UpdateProfileCommand）
- [x] `UserService` 接口 + `UserServiceImpl` 实现编译通过（替代原 6 个 AppService）
- [x] 单元测试验证编排逻辑
- [x] `createUser` 测试：成功创建 + 用户名重复抛异常
- [x] `updateUserStatus` 测试：禁用后调用 domainEventBus.fireAll
- [x] ModulithTest 通过（adapter → api 边界强制）

## Blocked by

`04-user-account-entities.md` + `05-domain-events.md`
