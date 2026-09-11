---
type: Decision Record
title: Verification 聚合根认定与发码拓扑
description: 设计 Verification 聚合根归属或发码拓扑时读：Verification 为独立聚合根与协助方，发码用例按场景归用户侧应用服务，发送走 AFTER_COMMIT 事件。
tags: [verification, aggregate-root, topology]
status: stable
---

# 0021 — Verification 聚合根认定与发码拓扑

Verification 是独立聚合根——判据为能否脱离 user 独立存在（RG 发码先于用户创建）与生命周期是否一致（验证码分钟级 vs
用户永久），它是验证码生命周期的协助方聚合、发码主体是用户。发码用例按场景归属用户侧应用服务（如
`UserAuthService.requestChangeMobile`），无聚合级统一入口、api 按用例拆命令；发送走 AFTER_COMMIT 事件，消费经
`CredentialChangeDomainService` 编排、同事务双 save。

## Consequences

- 唯一性机制：`active_key` = source 复合键，U 终态清 NULL，过期行惰性 DELETE 收敛进 gateway save
- api 无 scene/channel 枚举——per-use-case 命令，方法即场景，客户端不可伪造
- 用户状态规则（启用态、target ≠ 当前值）在应用层编排
