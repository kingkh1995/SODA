## Parent

`.scratch/soda-user/PRD.md`

## What to build

`soda-user-api` 模块中的领域事件 record 定义。全部实现 `DomainEvent` 基接口，泛型正确绑定对应的 Entity ID 类型。

**事件清单：**

| 事件 | EntityId 类型 | 组件 | 状态 |
|------|---------------|------|------|
| `UserCreatedEvent` | `UserId` | `userId, occurredAt` | **实现 publish** |
| `UserStatusChangedEvent` | `UserId` | `userId, oldStatus, newStatus, occurredAt` | **实现 publish** |
| `PasswordChangedEvent` | `UserId` | `userId, occurredAt` | record 定义，publish 留 TODO |
| `AccountBoundEvent` | `UserId` | `userId, accountType, accountId, occurredAt` | record 定义，publish 留 TODO |
| `AccountUnboundEvent` | `UserId` | `userId, accountType, accountId, occurredAt` | record 定义，publish 留 TODO |
| `UserRemovedEvent` | `UserId` | `userId, occurredAt` | record 定义，publish 留 TODO |

`publish` 逻辑在对应 Entity 的业务方法中通过 `registerEvent()` 注册，ApplicationService 持久化后通过 `flushEvents()` + `DomainEventBus.fireAll()` 发送。

## Acceptance criteria

- [ ] 6 个 event record 编译通过
- [ ] `UserCreatedEvent` 在 `User` 构造时通过 `registerEvent()` 注册
- [ ] `UserStatusChangedEvent` 在 `User.changeStatus()` 时注册
- [ ] 其余 4 个事件定义存在，publish 点有 `// TODO` 注释

## Blocked by

`04-user-account-entities.md`
