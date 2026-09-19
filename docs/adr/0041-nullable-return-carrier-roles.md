---
type: Decision Record
title: 可空返回载体：按角色二分 @Nullable T 与 Optional
description: 设计或评审可空返回值（数据载体属性、领域/网关查询未命中）的载体选择时读——数据载体（Command / Request / Response / DP）用 JSpecify `@Nullable T` 且禁 `Optional`，领域层与网关的「查询无结果」用 `Optional<T>`。
tags: [ jspecify, nullable, optional, dp, nullness ]
status: stable
---

# 0041 — 可空返回载体：按角色二分 `@Nullable T` 与 `Optional`

「无值」在本仓有两种 **不同语义**： **属性未携带**（数据载体的可空属性）与 **查询无命中**（领域/网关检索未找到结果）。前者用
JSpecify `@Nullable T` 标注， **禁止** `Optional` 包装；后者返回 `Optional<T>`。判据是 **角色**，不是个人偏好。

## 为什么

`Optional` 是 JDK API Note 警示的数据载体误用——其设计用途是「明确表示无结果」的返回值。把 `Optional` 套在记录型属性上会
**破坏 Jackson / MapStruct 的直通链路**，并把「未携带」与「显式空」合并成同一态。领域/网关的查询未命中则是显式的「没有这个结果」，正是
`Optional` 被设计的场景。

规则、写法与序列化链路见 [conventions/jspecify.md](../conventions/jspecify.md) §3.2
与 [conventions/dp-json-conventions.md](../conventions/dp-json-conventions.md)；MapStruct 版本差距与 null
行为缺口登记见 [conventions/mapstruct.md](../conventions/mapstruct.md) §5。

## Consequences

- 载体与角色绑定：数据载体（Command / Request / Response / DP 及其属性）→ `@Nullable T`；领域/网关「查询无结果」→
  `Optional<T>`。硬约束：不返回 `Optional<@Nullable T>`；`Optional` 变量自身不得为 `null`。
