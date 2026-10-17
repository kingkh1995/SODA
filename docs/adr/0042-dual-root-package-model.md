---
type: Decision Record
title: 双根包建模 — 两个应用根 + 单向跨根校验
description: 决定或评审应用根包拓扑（单根 vs 双根）、跨根依赖校验、传递依赖门禁或新增业务模块的模块边界时读——保留 com.soda.component / com.soda.user 两个独立应用根；SodaModulith 合并 FQN 扫描配置；SodaModulithArchitectureTest 是唯一全仓架构门禁。
tags: [ modulith, packaging, governance, cross-root ]
status: stable
---

# 0042 — 双根包建模：两个应用根 + 单向跨根校验

保留 `com.soda.component`（框架层）与 `com.soda.user`（业务层）作为 **两个独立应用根**。`soda-support-modulith` 的测试源通过
`SodaModulith`（`@Modulithic(additionalPackages = …)` + `useFullyQualifiedModuleNames = true`）合并两个根，
由 `SodaModulithArchitectureTest` 在显式包含全部模块的测试运行时中执行唯一权威的官方 `verify()` 与 CLOSED 模块传递依赖门禁。
跨根方向由同一测试类断言： **框架层不得反向依赖业务层**、 **业务层跨根依赖只允许指向框架层**。
`user.domain` 是登记的共享领域 OPEN 例外；全仓门禁集中在 `SodaModulithArchitectureTest` 单一测试类（最小兜底见
ADR-0045），测试支持包不再声明为应用模块。
**不做统一单根**。

## 为什么

白名单在原理上无法区分两根。`allowedDependencies` 的项相对传入实例的模块集解析；同一份 `package-info.java` 会被任何包含它的实例解析，故
**不能同时服务两种拓扑**：双根下写逻辑名 → 单根实例解析失败；统一单根 → 同名子包歧义。 **FQN
是唯一能表达并强制跨根依赖的命名制**，而 FQN 只能在合并实例上解析。

统一单根实测可行，但须 repackage 至 `com.soda` 单根 + `explicitly-annotated` + 三处嵌套模块去嵌套 + 25
处白名单前缀化，代价是嵌套模块语义永久消失（`SUB_MODULE` 规则是硬墙，实测 132 条违规无法消弍）。跨根依赖现状严格单向（
`user.* → component.*` 173 条，反向 0 条），收益与代价不匹配。故校验强度按风险面配比：官方 `verify()` 守白名单、无环与 named
interface，
传递依赖门禁补足 CLOSED 模块的实际依赖白名单，`SodaModulithArchitectureTest` 另守方向不变式；测试支持夹具不进入应用模块图。

机制细节与偏离登记见 [conventions/modulith.md](../conventions/modulith.md) §3.1 与 §4 E1。

## Consequences

- `SodaModulith` 合并两个应用根，使用 FQN 解析白名单；其测试运行时显式包含 18 个应用模块。
- `SodaModulithArchitectureTest` 是唯一权威校验入口，官方 `verify()`、传递依赖门禁与跨根方向断言在同一测试类中执行。
- 新增业务模块的跨根依赖只允许指向 `com.soda.component.*`，引用另一业务模块内部即被拒绝。
- `user.domain` 保持为登记的共享领域 OPEN module；其内部子包通过 unnamed interface 暴露，不使用 `@NamedInterface`。
- 双根定为架构选择而非待偿还技术债；`ADR-0008` 分层 starter 与双根绑定关系固化。
