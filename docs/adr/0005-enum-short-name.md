---
type: Decision Record
title: 枚举短名标识设计
description: 定义业务枚举时读——以 name() 短名（1-4 字符）作 DB 列与 JSON 判别值，desc 为英文 i18n key，DTO/VO 只传 String，状态机命名 XxxState。
tags: [enum, persistence]
status: stable
---

# 0005 — 枚举短名标识设计

业务枚举实现 `EnumType`（同时是 Domain Primitive），以 `name()` 短名（1-4 字符）作 DB 列与 JSON 的唯一判别值——比 int code
在日志/DB 中直观、比长名紧凑，无需 `fromCode()` 映射；每常量带 `desc`（英文 i18n key）经 `desc()` 读取，反序列化走各枚举
`@JsonCreator of(String)`（`valueOf(String)` 仅内部用）。DTO/VO 一律 `String` 传递、不直接引用枚举类型——枚举无需跨层共享，仅当
web 层跨模块引用才下沉组件层（`Sex` 先例），其余留在所在模块 domain。状态机枚举命名 `XxxState`（如 `UserState`），具体状态值用
`XxxStatus`（如 `HttpStatus`）；实现形态与模板见 [dp-conventions](../dp-conventions.md)。

## Consequences

- 枚举短名即对外合同（DB 列 + JSON 值 + ID 前缀），常量改名需 migration
- 跨领域共享值域以领域前缀助记码隔离（`SocialType`、`UserVerificationScene` 先例）
