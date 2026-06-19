## Parent

`.scratch/soda-user/PRD.md`

## What to build

社交账号绑定的 AppService 和对应 Command。

**API 模块：**
- `BindSocialCommand` — userId, socialType, openId
- `UnbindSocialCommand` — userId, socialType
- `UserDTO` 补充 socialAccounts 查询方法（可选）

**App 模块：**
- `SocialBindAppService` — 加载 User → `user.addSocialAccount(socialType, openId)` → save → fireAll（AccountBoundEvent publish 留 TODO）
- `SocialUnbindAppService` — 加载 User → `user.removeAccount(socialAccountId)` → save → fireAll（AccountUnboundEvent publish 留 TODO）

不允许重复绑定同类型社交账号（User 内部校验：同 socialType 的 SocialAccount 已存在时抛异常或忽略）。

## Acceptance criteria

- [ ] Command 编译通过
- [ ] AppService 单元测试（Mock Gateway）
- [ ] 测试：绑定成功后 Account 出现在 User.accounts 中
- [ ] 测试：重复绑定同一社交类型抛异常

## Blocked by

`04-user-account-entities.md` + `05-domain-events.md`
