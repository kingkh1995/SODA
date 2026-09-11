---
type: Decision Record
title: 聚合删除语义：终态迁移，领域无擦除
description: 设计聚合删除或生命周期终态时读——删除建模为领域终态迁移（UserState 增 R 吸收态），领域无 remove 词汇，gateway 仅 save 承载终态持久化。
tags: [aggregate, lifecycle, user]
status: stable
---

# 0017 — 聚合删除语义：终态迁移，领域无擦除

删除建模为领域终态迁移、擦除不属于领域：UserState 为 {E, D, R} 单维度状态机，R（注销）是吸收态终态，经 deregister () 从 D
严格迁移（业务不变量：仅禁用可注销）并注册 UserDeregisteredEvent；领域无 remove 词汇与删除 flag，mustEnable 单守卫同时覆盖 D
与 R。gateway 仅保留 save 承载终态持久化——R 的表示（状态列 / 软删 / 删行 /
归档）是基础设施的私有决定，领域不知晓；当前实现为状态列保留行。此后新增状态聚合沿用同一形态：生命周期 = 单一状态枚举 +
聚合根命令迁移方法。

## Consequences

- disable / enable 为 set-state 幂等（已处目标态 no-op 不发事件）；R 态下行为方法抛带消息 IAE（mustEnable 覆盖）
- 注销后的键释放与归档审计同为基础设施表示决策，见 [0023](0023-terminal-key-release-and-archive.md)
- 软删除（AIP-164）是基础设施持久化策略（标记删除、可 `:undelete`、无业务含义），注销是领域行为（`deregister()` D→R 吸收态迁移，经
  `POST /{resource}/{id}:deregister` 承载，不用标准 Delete）——『资源是否支持软删除』是 gateway 的 save
  表示决策，『注销后是否可恢复』取决于领域终态是否吸收（R 是吸收态、不可逆）。
