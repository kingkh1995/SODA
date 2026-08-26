---
type: Decision Record
title: 异常类使用约定：构造器校验与方法零守卫
description: 写领域模型校验与异常时读——构造器 DP 式校验（ValidateUtils→IAE）、方法零守卫（jspecify 契约）、业务参数校验带消息 IAE，防御守卫不携消息。
tags: [exception, validation, domain]
status: stable
---

# 0015 — 异常类使用约定：构造器校验与方法零守卫

写侧异常用法收敛为三分： **构造器校验（DP 式）**——属性合法的保证点在构造器，非空参数用 `ValidateUtils.notNull`（IAE 固定标准消息，与
DP 构造器完全一致），创建与恢复路径统一拦截； **方法零守卫**——方法参数 null 契约由 jspecify `@NullMarked` 声明 + 调用方遵守（真实
NPE 由 JEP 358 提供帮助消息，未来 NullAway 编译期 enforce），方法路径无运行时守卫； **业务参数校验**
——客户端可预期触发的一切（错码、过期、重复用户名、状态机前置）归带消息 IAE（`Assert.isTrue` / `Assert.notNull`
），消息是接线实现前客户端唯一的反馈通道。聚合内部结构不变量类型化（构造器必填字段 + 恢复路径 JSON
`@JsonProperty(required = true)`，不可表示的状态无需检查）；可空查找返回 `Optional`，requireXXX 模式仅在 appservice。

## Consequences

- 操作语义：set-state（`disable`/`enable`）幂等 no-op、不发事件；transition（`verify`/`use`）严格前置失败抛带消息 IAE；同值换绑抛
  IAE 是产品决策（昂贵验证码流程中同值＝操作失误）
- 防御编程守卫（网关终态拒写等兜底）不携消息——异常类型 + 栈帧即语义（裸 ISE / NPE / NSE），非客户端反馈通道
