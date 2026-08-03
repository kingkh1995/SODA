## Parent

`.scratch/soda-user/PRD.md`

## What to build

社交账号绑定的 AppService 和对应 Command。

**API 模块：**
- `BindSocialCommand` — userId, socialType, openId
- `UnbindSocialCommand` — userId, socialType
- `UserDTO` 补充 socialAccounts 查询方法（可选）

**App 模块：**
- `SocialBindAppService` — 加载 User → `user.addSocialAccount(socialType, openId)` → save → publishAll
- `SocialUnbindAppService` — 加载 User → `user.removeAccount(socialAccountId)` → save → publishAll

> 修订（2026-08-03）：`AccountBoundEvent` / `AccountUnboundEvent` 已删除（见 PRD 事件表注记），社交绑定需要的事件在实现时重新定义。`user.addSocialAccount` 尚不存在，需在 domain 层新增公开绑定入口。

不允许重复绑定同类型社交账号（User 内部校验：同 socialType 的 SocialAccount 已存在时抛异常或忽略）。

> **去重注意（2026-08-03 评审决定）**：当前 `User.addAccount` 的去重守卫 `typeEquals` 只比较 4 值 `AuthAccountType`（所有社交账号同为 O），**实现本 issue 时必须把去重改为按 socialType 判定**，否则第二个社交平台会被拒绝。

## Acceptance criteria

- [ ] Command 编译通过
- [ ] AppService 单元测试（Mock Gateway）
- [ ] 测试：绑定成功后 Account 出现在 User.accounts 中
- [ ] 测试：重复绑定同一社交类型抛异常

## Blocked by

`04-user-account-entities.md` + `05-domain-events.md`
