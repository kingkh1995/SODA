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

模块清单与依赖白名单的事实源在 22 处 `package-info.java`（其中 1 处在 `soda-support-modulith` 的 `src/testFixtures`）；模块数与
Gradle 项目数为 N:1（`soda-component-domain-types` 出
3、`soda-user-adapter` 出 3），本节不重述。

分层 starter 按 DDD 架构层（api → domain → app → adapter 家族）+ 基础设施 + 读服务 + 启动入口切割。每个 starter 通过
`allowedDependencies` 强制依赖方向——下层模块不允许反向引用上层。业务模块只需按需引入对应层。

adapter starter 家族（web/job/consumer）的包名不含 "adapter"——"adapter" 仅是构建级家族标签，Modulith 模块名即通道名（ADR-0019）。家族依赖
`{api}`，与基础设施（`{domain}`）并列、不隶属，按需引入。使用约定：不把 adapter 家族当基础设施层（依赖方向不同），也不与业务模块写侧
adapter 子模块（如 `soda-user-adapter`）混为一谈。

## 3. Modulith 治理规则

模块依赖通过 `@ApplicationModule(allowedDependencies = …)` 严格白名单控制：`type = CLOSED` 的模块 **必须**显式声明白名单。判定语义单源在
[conventions/modulith.md](conventions/modulith.md) §2 / §3——`{}` 与留空的语义区别、模块声明与 `type` 判据、白名单命名与
named interface、`verify()` 与反向校验的机制与覆盖范围，本节只留治理句与操作步骤。

### 一致性验证测试

**每个应用根（`ApplicationModules.of(...)` 的每个根包）必须有且仅有一个** Modulith 一致性验证测试；`verify()`
仅覆盖被传入的单个根包，跨根依赖不在任何实例的视野内——该方向由 `ModulithTestSupport.assertNoCrossRootCoupling()`
以单实例枚举补足（登记在 modulith.md §4
E1）。测试体写法归 [test-conventions.md](test-conventions.md)，模板见各应用根下的 `ModulithTest.java`。

### 新增模块步骤

1. 在根包添加 `package-info.java`，标注 `@ApplicationModule(allowedDependencies = {…})`（`type` 判据与白名单写法见
   [conventions/modulith.md](conventions/modulith.md) §2）
2. 运行所属 `ModulithTest` 的结构打印用例，与刚写的 `package-info.java` 声明核对一致（模块清单的事实源是声明本身，本仓不维护第二份副本）
3. 运行所属 `ModulithTest` 的三个校验用例确认无违反：`verify()` / 反向校验 / 跨根绑定

## 4. 单源指针

| 需要                                                                                                                                                              | 去哪                                                                                 |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 类型契约（DP / Entity / Aggregate / Identifiable / Identifier / 字面量家族 / EnumType / StateEnumType / 持久化基类 / Convertor / Gateway / 领域事件 / DP 清单表） | [conventions/framework-type-contracts.md](conventions/framework-type-contracts.md)   |
| 编排 / 异常 / Logging / 数据库开发约定 / Code Style（import 规范）                                                                                                | [conventions/framework-crosscutting.md](conventions/framework-crosscutting.md)       |
| DP 怎么写（不可变 / 自校验 / 工厂命名 / JSON / 缓存 / 敏感值 / Masked 族）                                                                                        | [dp-conventions.md](dp-conventions.md)                                               |
| DP 怎么测                                                                                                                                                         | [conventions/dp-test-conventions.md](conventions/dp-test-conventions.md)             |
| WebAssembler 契约语义与命名                                                                                                                                       | [conventions/adapter.md](conventions/adapter.md)                                     |
| DP 序列化契约族（字面量 / record / SensitiveValue / 自描述 ID）                                                                                                   | [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md)             |
| DP 校验与归一化（ValidateUtils / ParseUtils 职责）                                                                                                                | [conventions/dp-validation-conventions.md](conventions/dp-validation-conventions.md) |
| 测试分层与通用写法                                                                                                                                                | [test-conventions.md](test-conventions.md)                                           |
| 业务词汇（术语 + `_Avoid_`）                                                                                                                                      | 根目录 `CONTEXT.md`                                                                  |
| 架构决策（为什么这样设计）                                                                                                                                        | [adr/](adr/)                                                                         |
| 代码风格与注释规范                                                                                                                                                | [../STYLEGUIDE.md](../STYLEGUIDE.md)                                                 |
| 文档体系标准                                                                                                                                                      | [doc-conventions.md](doc-conventions.md)                                             |
| Jackson 3 用法（接入与共享 / 注解落位 / 多态 / 3.0 行为基线）                                                                                                     | [conventions/jackson.md](conventions/jackson.md)                                     |
| 空性标注（`@NullMarked` 基线 / `@Nullable` 位置契约 / 泛型与覆写 / 外部库边界）                                                                                   | [conventions/jspecify.md](conventions/jspecify.md)                                   |
| Lombok 注解（准入与禁用集 / 访问器与 fluent / 构造器与 Builder / 相等与调试 / 日志与构建接线）                                                                    | [conventions/lombok.md](conventions/lombok.md)                                       |
| MapStruct 机制规则（注解形态 / 映射语义 / 构建接线 / Null 与 JSpecify 缺口）                                                                                      | [conventions/mapstruct.md](conventions/mapstruct.md)                                 |
| Spring Framework 用法（DI / 组件与自动配置 / 事务 / 事件 / Web MVC 扩展点 / 校验）                                                                                | [conventions/spring.md](conventions/spring.md)                                       |
| Spring Modulith 模块声明与校验（`@ApplicationModule` / `allowedDependencies` / verify 与反向校验）                                                                | [conventions/modulith.md](conventions/modulith.md)                                   |
