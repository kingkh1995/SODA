---
type: Convention
title: Framework Conventions
description: 查 soda-component 框架层有什么、模块怎么治理、类型怎么约定时读——分层总览 · Module structure 与 Modulith 治理 · 单源指针。类型与契约正文见 conventions/framework-type-contracts.md，编排 / 异常 / Logging / 数据库 / Code Style 跨切面见 conventions/framework-crosscutting.md。
tags: [ convention, framework ]
status: stable
---

# Framework Conventions

领域框架层（`soda-component-domain` / `soda-component-domain-types`）的约定 **索引**：类型定义即代码 javadoc（单源），本文件承载分层总览与
Modulith 治理规则；类型契约、编排 / 异常 / Logging / 数据库 / Code Style 等 **约定正文**见下方单源指针。业务词汇见根
`CONTEXT.md`。

## 1. 分层总览

写侧 COLA 分层（api / domain / application / adapter 家族 / infrastructure / start）＋读侧 query-server
混装，每个业务模块七子模块；分层承载与依赖方向见 §2 的 starter 链。

## 2. Module structure (Spring Modulith)

模块清单与依赖白名单是构建产物，事实源在 `settings.gradle` / `build.gradle`；本节不重述。

分层 starter 按 DDD 架构层（api → domain → app → adapter 家族）+ 基础设施 + 读服务 + 启动入口切割。每个 starter 通过
`allowedDependencies` 强制依赖方向——下层模块不允许反向引用上层。业务模块只需按需引入对应层。

adapter starter 家族（web/job/consumer）的包名不含 "adapter"——"adapter" 仅是构建级家族标签，Modulith 模块名即通道名（ADR-0019）。家族依赖
`{api}`，与基础设施（`{domain}`）并列、不隶属，按需引入。使用约定：不把 adapter 家族当基础设施层（依赖方向不同），也不与业务模块写侧
adapter 子模块（如 `soda-user-adapter`）混为一谈。

## 3. Modulith 治理规则

### 白名单原则

模块依赖通过 `@ApplicationModule(allowedDependencies = …)` 严格白名单控制。 按 `type` 分两类：

| 模块角色           | `type`   | 校验行为                                                                         | `allowedDependencies` |
|--------------------|----------|----------------------------------------------------------------------------------|-----------------------|
| 根模块（无依赖）   | `OPEN`   | 允许被任何模块引用，自身依赖不校验                                               | `{}`（默认）          |
| 有依赖的业务层模块 | `CLOSED` | 依赖方向被 ModulithTest 强制校验，import 超出 `allowedDependencies` 的模块即失败 | 显式声明白名单        |

未声明的跨模块引用在编译时不会被阻止，但会被 `ModulithTest.verify()` 在测试阶段捕获并拒绝（针对 `CLOSED` 模块）。
`soda-supports:soda-support-modulith` 的 `ModulithTestSupport.assertNoUndeclaredDependenciesInClosedModules`
提供反向校验（实际使用未声明依赖），与 `verify()` 互补。

### ModulithTest 强制

每个 Gradle 子项目（settings.gradle include 的每个 soda-* 模块） **必须**有一个 Modulith 一致性验证测试。模板见各项目
`ModulithTest.java`；典型包括 `verify()`、`forEach` 打印模块结构、CLOSED 模块反向校验三步。

### 新增模块步骤

1. 在根包添加 `package-info.java`，标注 `@ApplicationModule(allowedDependencies = {…})`
    - 无依赖的根模块 → `type = OPEN`, `allowedDependencies = {}`
    - 有依赖的业务层模块 → `type = CLOSED`, `allowedDependencies` 中声明所需模块的完整逻辑名（如 `domain.util`，非 `util`）
2. 在所属项目的 `ModulithTest` 注释表格中新增一行（文档用途，测试自动扫描）
3. 运行 `ModulithTest.verifyModuleStructure()` 确认无违反

## 4. 单源指针

| 需要                                                                                                                                                              | 去哪                                                                                 |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 类型契约（DP / Entity / Aggregate / Identifiable / Identifier / 字面量家族 / EnumType / StateEnumType / 持久化基类 / Convertor / Gateway / 领域事件 / DP 清单表） | [conventions/framework-type-contracts.md](conventions/framework-type-contracts.md)   |
| 编排 / 异常 / JSpecify / Logging / 数据库开发约定 / Code Style（import 规范）                                                                                     | [conventions/framework-crosscutting.md](conventions/framework-crosscutting.md)       |
| DP 怎么写（不可变 / 自校验 / 工厂命名 / JSON / 缓存 / 敏感值 / Masked 族）                                                                                        | [dp-conventions.md](dp-conventions.md)                                               |
| DP 怎么测                                                                                                                                                         | [conventions/dp-test-conventions.md](conventions/dp-test-conventions.md)             |
| WebAssembler / MapStruct 契约语义与命名                                                                                                                           | [conventions/adapter.md](conventions/adapter.md)                                     |
| DP 序列化契约族（字面量 / record / SensitiveValue / 自描述 ID）                                                                                                   | [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md)             |
| DP 校验与归一化（ValidateUtils / ParseUtils 职责）                                                                                                                | [conventions/dp-validation-conventions.md](conventions/dp-validation-conventions.md) |
| 测试分层与通用写法                                                                                                                                                | [test-conventions.md](test-conventions.md)                                           |
| 业务词汇（术语 + `_Avoid_`）                                                                                                                                      | 根目录 `CONTEXT.md`                                                                  |
| 架构决策（为什么这样设计）                                                                                                                                        | [adr/](adr/)                                                                         |
| 代码风格与注释规范                                                                                                                                                | [../STYLEGUIDE.md](../STYLEGUIDE.md)                                                 |
| 文档体系标准                                                                                                                                                      | [doc-conventions.md](doc-conventions.md)                                             |
