## Parent

`.scratch/soda-user/PRD.md`

## Status

**部分完成** — 2/6 事件已实现（位于 `soda-user-domain`，非 PRD 规划的 `soda-user-api`），4 个事件未定义。

## What was built

`soda-user-domain` 中的领域事件 record。全部实现 `DomainEvent` 基接口（不含 `Serializable`），泛型正确绑定对应的 Entity ID 类型。

**事件清单：**

| 事件 | EntityId 类型 | 组件 | 状态 |
|------|---------------|------|------|
| `UserCreatedEvent` | `UserId` | `userId, occurredAt` | **已实现 publish** |
| `UserStatusChangedEvent` | `UserId` | `userId, oldStatus, newStatus, occurredAt` | **已实现 publish** |
| `PasswordChangedEvent` | `UserId` | `userId, occurredAt` | ❌ 未定义 |
| `AccountBoundEvent` | `UserId` | `userId, accountType, accountId, occurredAt` | ❌ 未定义 |
| `AccountUnboundEvent` | `UserId` | `userId, accountType, accountId, occurredAt` | ❌ 未定义 |
| `UserRemovedEvent` | `UserId` | `userId, occurredAt` | ❌ 未定义 |

## Acceptance criteria

- [x] 2 个 event record 编译通过（剩余 4 个未创建）
- [x] `UserCreatedEvent` 在 `User` 构造时通过 `registerEvent()` 注册
- [x] `UserStatusChangedEvent` 在 `User.changeStatus()` 时注册
- [ ] 其余 4 个事件记录定义存在
- [ ] 其余 4 个事件的 publish 点有 `// TODO` 注释
