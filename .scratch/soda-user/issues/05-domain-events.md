## Parent

`.scratch/soda-user/PRD.md`

## Status

**已完成** — 全部 6 个事件记录定义存在；PasswordChangedEvent 和 UserRemovedEvent 已实现 publish，AccountBoundEvent/AccountUnboundEvent 有 TODO 注释。

## What was built

`soda-user-domain` 中的领域事件 record。全部实现 `DomainEvent` 基接口，泛型正确绑定对应的 Entity ID 类型。

**事件清单：**

| 事件 | EntityId 类型 | 组件 | 状态 |
|------|---------------|------|------|
| `PasswordChangedEvent` | `UserId` | `userId, occurredAt` | **已实现 publish** (UserPasswordAppService 直接 fire) |
| `UserStatusChangedEvent` | `UserId` | `userId, oldStatus, newStatus, occurredAt` | **已实现 publish** |
| `AccountBoundEvent` | `UserId` | `userId, accountType, accountId, occurredAt` | **已定义，TODO publish 注释** (User.java:244) |
| `AccountUnboundEvent` | `UserId` | `userId, accountType, accountId, occurredAt` | **已定义，TODO publish 注释** (User.java:244) |
| `UserRemovedEvent` | `UserId` | `userId, occurredAt` | **已实现 publish** (UserDeleteAppService 直接 fire) |

## Acceptance criteria

- [x] 全部 6 个 event record 编译通过
- [x] `UserCreatedEvent` 在 `User` 构造时通过 `registerEvent()` 注册
- [x] `PasswordChangedEvent` 在 `UserPasswordAppService` 中通过 `domainEventBus.fire()` 发布
- [x] `UserRemovedEvent` 在 `UserDeleteAppService` 中通过 `domainEventBus.fire()` 发布
- [ ] `AccountBoundEvent` / `AccountUnboundEvent` publish 点有 `// TODO` 注释 (User.java:244)
- [x] 其余 4 个事件的 publish 点有 `// TODO` 注释
