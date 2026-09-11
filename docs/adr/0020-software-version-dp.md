---
type: Decision Record
title: SoftwareVersion 三段式软件版本号 DP
description: 定义软件版本号 DP 或需版本比较/排序时读：SoftwareVersion 三段式独立 DP，base-1000 打包可单调比较，不复用乐观锁 Version。
tags: [dp, version]
status: stable
---

# 0020 — SoftwareVersion 三段式软件版本号 DP

软件版本号是独立通用 DP `SoftwareVersion`（soda-components）：严格三段纯数字（major.minor.patch，每段 `[0, 999]`）、小写 `v`
前缀必选、前导 0 归一化、逐段数值比较——与乐观锁 `ConcurrencyVersion`（单 int 计数器）语义完全不同不能复用。base-1000 打包
int（`v2.1.3` ↔
`2001003`）无损且序与版本序单调一致，可直接用于 DB int 列存储/索引/范围查询；`nextPatch`/`nextMinor`/`nextMajor` 步进到段顶抛
IAE 不进位。格式严格：段数不足/超出、缺前缀一律拒绝，不做宽松解析（见 [dp-conventions](../dp-conventions.md) 不修理输入）。

## Consequences

- 单段上限 999 使 `v1000.0.0` 不可表示；放宽属 breaking change（规范值格式与打包编码同时受影响）。
