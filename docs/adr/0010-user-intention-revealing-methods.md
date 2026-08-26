---
type: Decision Record
title: User 意图揭示方法
description: 设计 User 聚合方法时读——属性修改统一 changeXxx（参数 Domain Primitive），状态跃迁用 disable/enable 表达具体意图，不暴露贫血 setter 或泛化 changeStatus。
tags: [user, naming, api]
status: stable
---

> 沿革注记：本 ADR 在旧版端点表中曾记录"自助改密不实现旧密码校验——admin
> 重置场景无旧密码语义"决策项；该决策已被 [ADR-0027 自助改密原密码守卫](0027-password-change-old-credential-guard.md) 撤销（仅限
> `POST /users/{id}:changePassword` 自助路径，admin 重置仍走独立命令）。本 ADR 当前 stable 形态不再展开端点表与原密码策略细节。

# 0010 — User 意图揭示方法

User 聚合根的方法按业务意图命名：属性修改统一 `changeXxx`（参数一律 Domain Primitive，由应用层从 Command
转换，实体不接触应用层类型），状态跃迁用 `disable` / `enable` 表达具体意图，不暴露贫血 setter 或泛化 `changeStatus`——setter
无法附加业务规则（如换手机号需拦截验证），泛化状态方法不限制合法跃迁路径。注销由聚合根 `deregister`
表达吸收态终态迁移（见 [ADR-0017](0017-aggregate-lifecycle-removal-semantics.md)）。

REST 端点形态（PATCH 字段级 / `:action` 自定义方法 / mobile·email
走独立验证流程端点）见 [ADR-0012](0012-url-naming-convention.md)，本 ADR 不重述。
