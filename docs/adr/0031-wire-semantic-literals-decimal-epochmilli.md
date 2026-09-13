---
type: Decision Record
title: wire≠semantic 字面量：小数与绝对时间点
description: 设计小数或绝对时间点字面量 DP 时读：wire≠semantic 时 value() 返线上类型、语义值作派生访问器，小数经 DecimalLiteralType、时间点经 EpochMilli，两者不共享基类。
tags: [dp, jackson, literal-type]
status: stable
---

# 0031 — wire≠semantic 字面量：小数与绝对时间点

线上标量类型 ≠ 语义值时（小数：`BigDecimal` 语义、`String` 线上防精度丢失；绝对时间点：`Instant` 语义、`long` 毫秒线上），字面量
DP 归其线上类型所在家族——`value()` 返回线上类型（承载继承的 `@JsonValue`），语义值作第二访问器 + 派生字段，不参与序列化/
`equals`/`hashCode`。落两个形态：小数 `DecimalLiteralType` 抽象基类（直接实现 `StringLiteralType`，集中「规范值 String + 派生
`decimalValue()`」缓存不变量与 `validate` 钩子，`WanYuan`/`Percentage` 继承）；绝对时间点 `EpochMilli`（record 直挂
`LongLiteralType`：`long` 毫秒规范值 + 派生 `toInstant()`，毫秒精度互逆、亚毫秒截断；单实现者不建时间子契约）；两者不共享基类——唯一共性「两访问器」太薄。
`EpochMilli` 的 `@JsonValue long` 覆盖 Jackson 3 裸 `Instant` 的 ISO-8601 字符串默认且不受全局 feature 影响。

## Consequences

- 现有裸 `Instant` 领域字段暂不迁移（wire-breaking；`EpochMilli` 当前无生产消费方）；审计列属基础设施，保持裸 `Instant` 不入
  DP
- 序列化行为表见 [dp-conventions](../dp-conventions.md)
