---
type: Decision Record
title: 领域能力边界：缓存／锁／追踪不进 Entity 与 Aggregate
description: 在 Entity / Aggregate 加缓存、分布式锁或变更追踪时读——三类能力不进领域层，缓存与锁归应用层、变更追踪归基础设施，领域层对 Redis/锁 SDK/持久化零感知。
tags: [ddd, entity, layering]
status: stable
---

# 0002 — 领域能力边界：缓存／锁／追踪不进 Entity 与 Aggregate

缓存、分布式锁、变更追踪三类能力都不进入领域层——Entity 的唯一职责是业务身份与业务行为，缓存与锁是应用层关注点（区域名、key、锁资源等策略声明在
ApplicationService 上），变更追踪是基础设施持久化优化（收敛在 gateway 实现内），领域层对 Redis、锁 SDK、持久化机制零感知。判据：若把这类能力做成
Entity 接口或基类方法（如 `cacheKey()`），「可选」语义即退化为全员必备，与业务无关的 key/ttl 方法会污染实体接口。
