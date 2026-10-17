---
type: Decision Record
title: 本地领域事件同事务处理与聚合引用载荷
description: 设计 User/Verification 领域事件载荷与 Spring 监听事务时读：本地事件同步消费并共享发布方事务，聚合引用是明确例外。
tags: [ event, transaction, verification ]
status: stable
---

# 0044 — 本地领域事件同事务处理与聚合引用载荷

`UserCreatedEvent` 与 `VerificationCreatedEvent` 是本地进程内领域事件，均携带对应聚合引用，由普通同步 `@EventListener`
直接消费；事件不承诺 outbox、Registry 或可序列化。Verification
发送方异常继续传播并触发发布方事务回滚，但外部发送与数据库提交不具备跨资源原子性，项目接受“外部已发送、数据库提交失败”的双写窗口。

## Consequences

- 发布方事务覆盖外部发送：事务时长受 sender 超时约束、连接在发送期间被占用。当前量级接受该代价；若发送变慢或并发上升成为问题，把发送移到事务之后再评估——不为此预置开关或独立事务。