---
type: Decision Record
title: 活跃验证唯一性：active_key 数据库硬保证
description: 设计验证唯一性或占槽机制时读：同一 source 至多一条活跃验证由 uk_active_key 唯一索引硬保证，占槽与过期释放收敛进 gateway save。
tags: [verification, uniqueness, persistence]
status: stable
---

# 0025 — 活跃验证唯一性：active_key 数据库硬保证

同一 source 至多一条活跃验证由 DB 硬保证（应用层预检只承担友好快速失败）：`active_key` 列 =
`VerificationSource.compositeKey()`（scene:subject），`uk_active_key` 唯一索引仲裁并发——I/P 且未过期占槽，U 终态迁移清
NULL（终态不参与唯一），过期 I/P 行惰性 DELETE 收敛进 gateway save（INSERT 前同事务腾槽）。不引入 E 态——过期保持派生判断、不物化状态；预检
`existsBySource` 快速失败在前，并发撞索引 `DataIntegrityViolationException` 原样上抛、不做翻译。

## Consequences

- 槽位语义 per-source： (UCC, userId) 防同用户对不同 recipient 刷码、 (URG, phone) 防同端点重复；跨 source
  并存无害——输家收不到码即无法消费
- 生成列方案不可行（时间不可入键、过期 P 行永占槽）；existsBy* 命名规范单源在
  conventions/framework-type-contracts.md「查询契约命名」
