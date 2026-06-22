## Parent

`.scratch/soda-user/PRD.md`

## Status

**已完成** — 代码合并至 `main`（未推送）。

## What was built

`soda-user-domain` 模块中的所有领域原语（Domain Primitive）类。每个 DP 实现 `Type`/`EnumType` 接口，不可变、自校验、可序列化、可比较。

**DP 清单：**
- `UserId` (LongId 包装，服务端生成)
- `Username` — 4-30 位字母数字，可变
- `Nickname` — 最多 30 字符
- `Mobile` — 手机号格式校验 + 归一化
- `Sex` — 枚举 DP，实现 `EnumType extends Type`，常量 `M`(Male) / `F`(Female)
- `Avatar` — URL 格式校验
- `UserStatus` — 枚举 DP，常量 `E`(Enabled) / `D`(Disabled)
- `AuthAccountType` — 枚举 DP，常量 `P`(Password) / `S`(Sms) / `E`(Email) / `O`(OAuth)
- `SocialType` — 枚举 DP，常量 `GE`(Gitee) / `DT`(DingTalk) / `WENT`(WechatWork) / `WMP`(WechatMp) / `WOPN`(WechatOpen) / `WMIN`(WechatMini) / `ALIP`(AlipayMini)
- `PasswordAccountId` — 基于 userId 派生 `Identifier<Long>`
- `SmsAccountId` — 基于 mobile 派生 `Identifier<String>`
- `EmailAuthAccountId` — 基于 email 派生 `Identifier<String>`
- `SocialAuthAccountId` — 基于 socialType + openId 组合派生 `Identifier<String>`
- `VerificationCodePolicy` — codeLength + expiry Duration，带 ServiceLoader SPI 机制
- `VerificationCode` — code + expireAt + used，含 verify() / use() / isExpired()

**枚举 DP 特殊约定：**
- 实现 `EnumType`（继承 `Type`），使用 Java `enum` + Lombok，不转为 class/record
- 序列化使用 Jackson 默认 `name()`（短名字符串）
- 反序列化通过 `@JsonCreator of(String)` 委托 `ParseUtils.parseEnum()`
- 编译器生成的 `valueOf(String)` 可直接作为不可靠输入入口（抛 IAE）

## Acceptance criteria

- [x] 所有 DP 编译通过
- [x] 每个 DP 有单元测试覆盖：合法构造、非法构造（抛异常）、相等性、序列化反序列化
- [x] 测试不含 Spring 上下文，纯 JUnit 5
- [x] 测试风格对齐 `soda-component-support` 的 `UUIdTest` / `EmailTest`
