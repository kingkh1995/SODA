---
type: Convention
title: Framework Conventions
description: 查 soda-component 框架层有什么、模块怎么治理、类型怎么约定时读——分层总览 · Module structure 与 Modulith 治理 · 单源指针。类型与契约正文见 conventions/framework-type-contracts.md，编排 / 异常 / Logging / 数据库 / Import 约定跨切面见 conventions/framework-crosscutting.md。
tags: [ convention, framework ]
status: stable
---

# Framework Conventions

领域框架层（`soda-component-domain` / `soda-component-domain-types`）的约定 **索引**：类型定义即代码 javadoc（单源），本文件承载分层总览与
Modulith 治理规则；类型契约、编排 / 异常 / Logging / 数据库 / Import 约定等 **约定正文**见下方单源指针。业务词汇见根
`CONTEXT.md`。

## 1. 分层总览

写侧 COLA 分层（api / domain / application / adapter 家族 / infrastructure / start）＋读侧 query-server
混装，每个业务模块七子模块；分层承载与依赖方向见 §2 的 starter 链。

## 2. Module structure (Spring Modulith)

模块清单与依赖白名单的事实源在各 module 的 `src/main` `package-info.java`；模块数与 Gradle 项目数为 N:1
（`soda-component-domain-types` 与 `soda-user-adapter` 各承载多个包），本节不重述。`user.domain` 是明确登记的共享领域 OPEN
module；测试支持夹具不计入应用模块图。

分层 starter 按 DDD 架构层（api → domain → app → adapter 家族）+ 基础设施 + 读服务 + 启动入口切割。每个 starter 通过
`allowedDependencies` 强制依赖方向——下层模块不允许反向引用上层。业务模块只需按需引入对应层。

adapter starter 家族（web/job/consumer）的包名不含 "adapter"——"adapter" 仅是构建级家族标签，Modulith 模块名即通道名（ADR-0019）。家族依赖
`{api}`，与基础设施（`{domain}`）并列、不隶属，按需引入。使用约定：不把 adapter 家族当基础设施层（依赖方向不同），也不与业务模块写侧
adapter 子模块（如 `soda-user-adapter`）混为一谈。

## 3. Modulith 治理规则

模块依赖通过 `@ApplicationModule(allowedDependencies = …)` 严格白名单控制：`type = CLOSED` 的模块 **必须**显式声明白名单。判定语义单源在
[conventions/modulith.md](conventions/modulith.md) §2 / §3——`{}` 与留空的语义区别、模块声明与 `type` 判据、白名单命名与
named interface、官方 `verify()` 与项目传递依赖门禁的机制和覆盖范围，本节只留治理句与操作步骤。

### 一致性验证测试

**唯一的全仓 Modulith 架构门禁**（`soda-supports/soda-support-modulith`，治理边界见
[ADR-0045](adr/0045-modulith-gate-minimal-backstop.md)）兜底官方 `verify()`、项目传递依赖门禁、跨根方向与应用根存在断言——防住白名单写错、
依赖方向写反、应用根丢失三类回归。门禁正文（测试类与四个断言、应用根登记步骤、运行与诊断命令）单源在
[test-conventions.md](test-conventions.md) §2.4，本节不重述。

### 新增模块步骤

1. 在根包 `package-info.java` 声明 `@ApplicationModule(allowedDependencies = {…})`，其余按
   [conventions/modulith.md](conventions/modulith.md) 写清单逐条执行（`type` 判据、白名单写法、named interface、可引用性）；
2. 声明改动完成后运行全仓架构门禁并确认通过——门禁机制、断言与运行 / 诊断入口见
   [test-conventions.md](test-conventions.md) §2.4。

## 4. 单源指针

| 需要                                                                                                                                                              | 去哪                                                                                 |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 类型契约（DP / Entity / Aggregate / Identifiable / Identifier / 字面量家族 / EnumType / StateEnumType / 持久化基类 / Convertor / Gateway / 领域事件 / DP 清单表） | [conventions/framework-type-contracts.md](conventions/framework-type-contracts.md)   |
| 编排 / 异常 / Logging / 数据库开发约定 / Import 约定                                                                                                | [conventions/framework-crosscutting.md](conventions/framework-crosscutting.md)       |
| DP 怎么写（不可变 / 自校验 / 工厂命名 / JSON / 缓存 / 敏感值 / Masked 族）                                                                                        | [dp-conventions.md](dp-conventions.md)                                               |
| DP 怎么测                                                                                                                                                         | [conventions/dp-test-conventions.md](conventions/dp-test-conventions.md)             |
| WebAssembler 契约语义与命名                                                                                                                                       | [conventions/adapter.md](conventions/adapter.md)                                     |
| DP 序列化契约族（字面量 / record / SensitiveValue / 自描述 ID）                                                                                                   | [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md)             |
| DP 校验与归一化（ValidateUtils / ParseUtils 职责）                                                                                                                | [conventions/dp-validation-conventions.md](conventions/dp-validation-conventions.md) |
| 测试分层与通用写法                                                                                                                                                | [test-conventions.md](test-conventions.md)                                           |
| 业务词汇（术语 + `_Avoid_`）                                                                                                                                      | 根目录 `CONTEXT.md`                                                                  |
| 架构决策（为什么这样设计）                                                                                                                                        | [adr/](adr/)                                                                         |
| 代码风格与注释规范                                                                                                                                                | [../STYLEGUIDE.md](../STYLEGUIDE.md)                                                 |
| 文档体系标准（写/改文档、ADR 或 frontmatter、执行迁移审计时读：权威层级 · bundle 布局 · 来源登记与 profile · 写·检视双清单 · 合规硬门） | [doc-conventions.md](doc-conventions.md)                                             |
| Jackson 3 用法（接入与共享 / 注解落位 / 多态 / 3.0 行为基线）                                                                                                     | [conventions/jackson.md](conventions/jackson.md)                                     |
| 空性标注（`@NullMarked` 基线 / `@Nullable` 位置契约 / 泛型与覆写 / 外部库边界）                                                                                   | [conventions/jspecify.md](conventions/jspecify.md)                                   |
| Lombok 注解（准入与禁用集 / 访问器与 fluent / 构造器与 Builder / 相等与调试 / 日志与构建接线）                                                                    | [conventions/lombok.md](conventions/lombok.md)                                       |
| MapStruct 机制规则（注解形态 / 映射语义 / 构建接线 / Null 与 JSpecify 缺口）                                                                                      | [conventions/mapstruct.md](conventions/mapstruct.md)                                 |
| Spring Framework 用法（DI / 组件与自动配置 / 事务 / 事件 / Web MVC 扩展点 / 校验）                                                                                | [conventions/spring.md](conventions/spring.md)                                       |
| Spring Modulith 模块声明与校验（`@ApplicationModule` / `allowedDependencies` / 官方 `verify()` 与传递依赖门禁）                                                   | [conventions/modulith.md](conventions/modulith.md)                                   |
