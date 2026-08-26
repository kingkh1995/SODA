---
type: Decision Record
title: Gateway save 统一路由：全权委托 Spring Data JPA
description: 设计 gateway save 持久化路由或终态处理时读——save 单一入口全权委托 Spring Data JPA，三语义由 isNew+merge 内建路由，网关零判别逻辑。
tags: [gateway, persistence, jpa]
status: stable
---

# 0024 — Gateway save 统一路由

`EntityGateway.save` 不做判别逻辑——`isNew()`（`id == null`，收在 `AbstractPersistable`）+ Spring Data `merge` 按行存在性统一路由
insert/put/终态处理三语义，网关零判别。机制细节（`AbstractPersistable.isNew` 判据、`Convertor.toPersistence`
全量构造、审计列不搬运、终态守卫、insert-only
表等）见 [conventions/framework-type-contracts.md](../conventions/framework-type-contracts.md)
「EntityGateway」「AbstractPersistable / AbstractAuditable」「Convertor（基础设施）」节——本 ADR 只承载为什么这么定，不重述机制。

## Consequences

- 选 Spring Data 内建路由而非手写判别：消除多入口与判别漂移；网关代码只承担"输入领域聚合 → 输出 ID"的责任
- 客户端生成 id 的聚合 save 前置：必须已标识（网关 fail-fast NPE）
