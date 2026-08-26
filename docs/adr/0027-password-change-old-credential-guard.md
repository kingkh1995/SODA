---
type: Decision Record
title: 自助改密原密码守卫
description: 实现自助改密或原密码守卫时读：聚合根先 mustEnable 再 verify 旧凭证，不匹配抛 IAE 零变更零事件，admin 重置走独立命令。
tags: [user, password, guard]
status: stable
---

# 0027 — 自助改密原密码守卫

自助改密的凭证归属防线是原密码比对域守卫：`User.changePassword` 先经 `mustEnable`（R/D 态用户得到状态语义异常而非密码语义），再经
`PasswordAuthAccount.verify` 比对旧凭证与当前哈希，不匹配抛 IAE、零变更零事件——不变量单一来源在聚合根，会话授权不等于凭证持有。
`oldPassword` 字段经链路透传：校验注解落 adapter 请求对象，Command 纯透传，应用层构造 `SecretValue` 不做业务校验。

## Consequences

- 未来引入 admin 重置（无旧密码语义）时走独立命令绕过本守卫，不放宽自助路径的强制校验
