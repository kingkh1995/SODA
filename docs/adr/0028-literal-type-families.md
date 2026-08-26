---
type: Decision Record
title: 字面量类型家族
description: 设计单属性字面量 DP 或序列化时读：字面量按线上基本类型归五家族接口（互不关联、无共享根），序列化随家族契约继承。
tags: [dp, jackson, literal-type]
status: stable
---

# 0028 — 字面量类型家族

单属性字面量 DP 按线上基本类型归入五家族接口 `StringLiteralType`/`LongLiteralType`/`IntLiteralType`/
`BooleanLiteralType`/`DoubleLiteralType`——互不关联（IntSupplier 式，无共享根）、各 `extends Type`、`value()` 返回原语免装箱/拆箱；
`EnumType` 与五家族平行（枚举是封闭常量集、常量自身即值，不包装字面量）。单属性 DP 全量收编五家族，序列化入口随家族契约继承——实现类零/近零
Jackson 代码（机制与实现模板见 [dp-conventions](../dp-conventions.md)）；多属性 DP 与故意不可序列化类型（`SecretValue`）不参与。

## Consequences

- `VerificationRecipient<T extends StringLiteralType>` 泛型边界随家族契约收敛——基础设施经泛型捕获直接 `target().value()`
  落裸串，免密封 switch 判别
- `DoubleLiteralType` 契约就位、暂无实现；wire≠semantic
  扩展（小数/时间点）见 [ADR-0031](0031-wire-semantic-literals-decimal-epochmilli.md)
