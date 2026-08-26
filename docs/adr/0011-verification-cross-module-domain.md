---
type: Decision Record
title: 验证是独立于用户聚合的可复用领域概念
description: 设计验证码或跨聚合验证流程时读——验证码独立 Verification 聚合承载（分钟级临时状态），消费经外部聚合参数传入、领域服务跨聚合编排。
tags: [verification, aggregate, user]
status: stable
---

# 0011 — 验证是独立于用户聚合的可复用领域概念

验证码是分钟级临时状态，与永久凭证和用户聚合生命周期不一致，因此建模为独立的 `Verification` 聚合而非 AuthAccount/User
的内嵌字段——换绑、登录、找回、注册等多用例复用同一验证概念。消费采用外部聚合参数传入：`User.changeMobile(Verification)` 自带
source 匹配守卫、不反向修改验证聚合；跨聚合流程（verify → change → use）的编排与同事务双 save 见 ADR-0021。

## Consequences

- 同事务双 save 是「一个事务一个聚合」的显式例外——适用条件：同限界上下文、短事务、无外部 I/O、不变量真实共享
- 过期是派生判断、不落状态；verify 失败路径实体无变更、不落库
