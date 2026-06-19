## Parent

`.scratch/soda-user/PRD.md`

## What to build

`soda-user-domain` 模块中的所有领域原语（Domain Primitive）类。每个 DP 实现 `Type` 接口，不可变、自校验、可序列化、可比较。

**DP 清单：**
- `UserId` (LongId 包装，服务端生成)
- `Username` — 4-30 位字母数字，可变
- `Nickname` — 最多 30 字符
- `Mobile` — 手机号格式校验 + 归一化
- `Sex` — 枚举 MALE / FEMALE / UNKNOWN
- `Avatar` — URL 格式校验
- `UserStatus` — 枚举 ENABLED / DISABLED
- `AccountType` — 枚举 PASSWORD / SMS / EMAIL / SOCIAL
- `SocialType` — 枚举 GITEE / DINGTALK / WECHAT_ENTERPRISE / WECHAT_MP / WECHAT_OPEN / WECHAT_MINI_PROGRAM / ALIPAY_MINI_PROGRAM
- `PasswordAccountId` — 基于 userId 派生 `Identifier<Long>`
- `SmsAccountId` — 基于 mobile 派生 `Identifier<String>`
- `EmailAuthAccountId` — 基于 email 派生 `Identifier<String>`
- `SocialAccountId` — 基于 socialType + openId 组合派生 `Identifier<String>`
- `VerificationCodePolicy` — codeLength + expiry Duration，带 ServiceLoader SPI 机制
- `VerificationCode` — code + expireAt + used，含 verify() / use() / isExpired()

每个 DP 需配套单元测试（构造成功/失败、相等性、Jackson 序列化、compareTo）。

## Acceptance criteria

- [ ] 所有 DP 编译通过
- [ ] 每个 DP 有单元测试覆盖：合法构造、非法构造（抛异常）、相等性、序列化反序列化
- [ ] 测试不含 Spring 上下文，纯 JUnit 5
- [ ] 测试风格对齐 `soda-component-support` 的 `UUIdTest` / `EmailTest`

## Blocked by

`01-foundation-scaffolding.md`
