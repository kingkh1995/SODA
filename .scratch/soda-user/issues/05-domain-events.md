## Parent

`.scratch/soda-user/PRD.md`

## Status

**已完成（2026-08-03 修订）** — 4 个事件记录定义存在；PasswordChangedEvent、UserRemovedEvent、UserStateChangedEvent、UserCreatedEvent 已实现 publish。AccountBoundEvent/AccountUnboundEvent 已删除（社交绑定未实现，事件随之下架，见 PRD 修订注记）。

## What was built

`soda-user-domain` 中的领域事件 record。全部实现 `DomainEvent` 基接口，泛型正确绑定对应的 Entity ID 类型。

**事件清单：**

| 事件 | EntityId 类型 | 组件 | 状态 |
|------|---------------|------|------|
| `UserCreatedEvent` | `UserId` | `user, occurredAt` | **已实现 publish** (User.create 注册) |
| `PasswordChangedEvent` | `UserId` | `userId, occurredAt` | **已实现 publish** (UserAuthServiceImpl) |
| `UserStateChangedEvent` | `UserId` | `userId, oldState, newState, occurredAt` | **已实现 publish** (User.disable/enable) |
| `UserRemovedEvent` | `UserId` | `userId, occurredAt` | **已实现 publish** (UserServiceImpl.deleteUser) |

> 已删除：`AccountBoundEvent` / `AccountUnboundEvent`（2026-08-03，社交绑定功能未实现，事件定义下架；issue-07 社交绑定设计时重新定义）。

## Acceptance criteria

- [x] 全部 4 个 event record 编译通过
- [x] `UserCreatedEvent` 在 `User` 构造时通过 `registerEvent()` 注册
- [x] `PasswordChangedEvent` 在 `UserAuthServiceImpl` 中通过 `domainEventBus.publish()` 发布
- [x] `UserRemovedEvent` 在 `UserServiceImpl` 中通过 `domainEventBus.publish()` 发布
- [x] `UserStateChangedEvent` 在 `User.disable()/enable()` 中注册
