---
type: Decision Record
title: 双根包建模 — 两个应用根 + 单向跨根校验
description: 决定或评审应用根包拓扑（单根 vs 双根）、跨根依赖校验、或新增业务模块的模块边界时读——保留 com.soda.component / com.soda.user 两个独立应用根，合并 FQN 实例为权威 verify() 入口；跨根方向由两条不变式（框架层不反依赖业务层、业务层不互引）经 assertNoCrossRootCoupling() 机检。
tags: [ modulith, packaging, governance, cross-root ]
status: stable
---

# 0042 — 双根包建模：两个应用根 + 单向跨根校验

保留 `com.soda.component`（框架层）与 `com.soda.user`（业务层）作为 **两个独立应用根**，通过
`ModulithTestSupport.CrossRootModules`（`@Modulithic(additionalPackages = …)` + `useFullyQualifiedModuleNames = true`）合并为
**单一校验实例**，作为 `verify()` 的唯一权威入口。跨根方向由 `ModulithTestSupport.assertNoCrossRootCoupling()` 以
**同一实例枚举**补足，只断言两条不变式： **框架层不得反向依赖业务层**、 **业务层跨根依赖只允许指向框架层**。
**不做统一单根**。

## 为什么

白名单在原理上无法区分两根。`allowedDependencies` 的项相对传入实例的模块集解析；同一份 `package-info.java` 会被任何包含它的实例解析，故
**不能同时服务两种拓扑**：双根下写逻辑名 → `of("com.soda.user")` 解析失败；统一单根 → 同名子包歧义。 **FQN
是唯一能表达并强制跨根依赖的命名制**，而 FQN 只能在合并实例上解析，故权威 `verify()` 只能在 `CrossRootModules` 上跑。

统一单根实测可行，但须 repackage 至 `com.soda` 单根 + `explicitly-annotated` + 三处嵌套模块去嵌套 + 25
处白名单前缀化，代价是嵌套模块语义永久消失（`SUB_MODULE` 规则是硬墙，实测 132 条违规无法消弭）。跨根依赖现状严格单向（
`user.* → component.*` 173 条，反向 0 条），收益与代价不匹配。故校验强度按风险面配比：`CrossRootModules.verify()` 守白名单 +
无环 + named interface，`assertNoCrossRootCoupling()` 守方向不变式。

机制细节与偏离登记见 [conventions/modulith.md](../conventions/modulith.md) §3.1 与 §4 E1。

## Consequences

- `CrossRootModules.verify()` 是唯一权威校验入口；19 处 `package-info.java` 的白名单全部用 FQN 书写（同根短名实例无法解析
  FQN）。
- `assertNoCrossRootCoupling()` 补跨根方向，二者职责互补，不可互相替代。
- 新增业务模块的跨根依赖只允许指向 `com.soda.component.*`，引用另一业务模块内部即被拒绝。
- 双根定为架构选择而非待偿还技术债；`ADR-0008` 分层 starter 与双根绑定关系固化。
