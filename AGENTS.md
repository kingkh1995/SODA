---
type: Agent Guide
title: Soda 仓库指南
description: agent 接手本仓库任务时读——先按下方「触发场景 → 文档」表定向到对应约定 / 词汇 / ADR，再动手。
tags: [ agent-guide, ddd ]
status: stable
---

# 仓库指南

agent 接手本仓库任何任务前的入口。项目概览读 README.md；语言与术语约定以 STYLEGUIDE §1 / CONTEXT.md 为单源，本文件不重述。

## 5 分钟入门

首次接手本仓库的 agent 按顺序读这三份即可（其他都是按需查）：

1. **根目录 `CONTEXT.md`** — 业务词汇单一来源（User / AuthAccount / Verification 等术语）
2. **根目录 `STYLEGUIDE.md`** — Java 代码风格与注释规范
3. **`docs/doc-conventions.md`** — 文档体系自身规范（frontmatter / bundle 布局 / 合规基准 / V1 阶段条款）

读完后跳到「工作前读什么」按触发场景定向；遇到架构决策先查 `docs/adr/_index.md` 主题聚类。

## 工作前读什么（按触发场景定向）

| 触发场景（词）                      | 文档                                                                                                                                                                                                                                        |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 领域术语 / 概念命名                 | 根目录 `CONTEXT.md`                                                                                                                                                                                                                         |
| 架构决策 / 领域行为设计             | `docs/adr/`（按编号）＋ [adr/_index.md](docs/adr/_index.md)（按主题聚类）                                                                                                                                                                   |
| 写 DP                               | `docs/dp-conventions.md`（设计规则）＋ `docs/conventions/dp-validation-conventions.md`（校验与归一化）                                                                                                                                      |
| DP JSON / 序列化                    | `docs/conventions/dp-json-conventions.md`（Jackson 模式总表与示例）                                                                                                                                                                         |
| 新增字段类型                        | `docs/conventions/framework-type-contracts.md`（DP 清单表）＋ `docs/dp-conventions.md`                                                                                                                                                      |
| Entity / 聚合                       | `docs/conventions/framework-type-contracts.md`（Entity / Aggregate 条目）                                                                                                                                                                   |
| 网关 / 持久化                       | `docs/conventions/framework-type-contracts.md`（Gateway / AbstractPersistable / Convertor 条目）                                                                                                                                            |
| 写测试                              | `docs/test-conventions.md`（分层映射 · 通用写法）                                                                                                                                                                                           |
| DP 必测分组                         | `docs/conventions/dp-test-conventions.md`                                                                                                                                                                                                   |
| 新增模块                            | `docs/framework-conventions.md`（模块结构）                                                                                                                                                                                                 |
| 依赖声明                            | `docs/framework-conventions.md` §3 声明规则                                                                                                                                                                                                 |
| 模块边界                            | `docs/conventions/framework-crosscutting.md` §5 ApplicationService 编排规范                                                                                                                                                                 |
| 建表 / Flyway / 数据库开发          | [ADR-0022 开发阶段数据库管理约定](docs/adr/0022-dev-database-management.md)                                                                                                                                                                 |
| 抛异常 / 异常类                     | `docs/conventions/framework-crosscutting.md`（异常约定）＋ [ADR-0015 异常类使用约定](docs/adr/0015-exception-class-convention.md)                                                                                                           |
| 错误结构                            | `docs/conventions/framework-type-contracts.md`（Result / ErrorInfo 条目）＋ [ADR-0013 错误响应结构](docs/adr/0013-error-response-structure.md)                                                                                              |
| 跨聚合用例 / DomainService          | `docs/conventions/framework-crosscutting.md`（编排规范）                                                                                                                                                                                    |
| AppService 编排前置                 | `docs/conventions/framework-crosscutting.md`（编排规范）                                                                                                                                                                                    |
| 打日志 / 入口日志 / 日志层级        | `docs/conventions/framework-crosscutting.md`（Logging）                                                                                                                                                                                     |
| 写转换器 / WebAssembler / MapStruct | `docs/conventions/adapter.md`（MapStruct 注解速查见 `STYLEGUIDE.md` §3.4）                                                                                                                                                                  |
| 领域事件 / 事件载荷 / 投递时机      | `docs/conventions/framework-type-contracts.md`（DomainEvent 族节）＋ `docs/conventions/framework-crosscutting.md`（编排规范副作用条）＋ [ADR-0011 跨模块验证领域](docs/adr/0011-verification-cross-module-domain.md)                        |
| 敏感值 / DP 设计                    | `docs/dp-conventions.md`（敏感值与 Masked 族）                                                                                                                                                                                              |
| 加密 / 哈希 / 盲索引                | `docs/conventions/framework-type-contracts.md`（Gateway 契约一览 · 盲索引模式）                                                                                                                                                             |
| 缓存 key / 分布式锁 / Lockable      | `docs/conventions/framework-type-contracts.md`（Cacheable / Lockable / KeyUtils 节）                                                                                                                                                        |
| 状态机                              | `docs/conventions/framework-type-contracts.md`（StateEnumType 节）                                                                                                                                                                          |
| 终态守卫                            | `docs/conventions/framework-crosscutting.md`（终态守卫段）＋ [ADR-0017 聚合生命周期与删除语义](docs/adr/0017-aggregate-lifecycle-removal-semantics.md) / [ADR-0023 注销终态键释放与归档](docs/adr/0023-terminal-key-release-and-archive.md) |
| 文档体系自身规范                    | `docs/doc-conventions.md`                                                                                                                                                                                                                   |

## 架构


读写分离：写侧 DDD 分层（COLA）、读侧简单混装（yudao 风格）；结构与依赖白名单见 `docs/framework-conventions.md` §1-3。

## 文档体系怎么读

四类知识各归其位（详见 `docs/doc-conventions.md §1`）：

- **词汇**（User 是什么、Verification 是什么）→ 根 `CONTEXT.md`
- **决策**（为什么这样设计）→ `docs/adr/`（按编号 + 主题聚类 `docs/adr/_index.md`）
- **契约**（类型不变式 / 前置 / 副作用）→ 与代码同处的 Javadoc
- **约定**（怎么写代码 / 怎么写文档）→ 根 `STYLEGUIDE.md` + `docs/framework-conventions.md` + `docs/conventions/*`

判断标准：一种内容出现在两类文档里就是 bug。 **改一处，别处不需跟着改**——这是文档体系的健康信号。

## ADR 怎么用

ADR 极简三句：背景 + 决策 + 为什么。V1 阶段正文不写被否方案、不写修订沿革，过时即删。

- **查决策**：`docs/adr/_index.md` 按主题聚类导航；按编号 grep
- **写新 ADR**：模板见 `docs/doc-conventions.md §4`；编号取当前最大值 + 1
- **改旧 ADR**：V1 阶段 **就地更新**为最终态，不另开 ADR
- **撤销决策**：直接删除 ADR 文本，git 历史承载；编号空洞合法

## Agent skills

### Issue tracker

追踪 Issue / 写 spec 时：存于 `.scratch/<feature>/` 本地 markdown（GitHub 不用于追踪）。见 `docs/agents/issue-tracker.md`。

### Triage labels

给 Issue 打 triage 标签时：五个标准角色用默认标签串（needs-triage / needs-info / ready-for-agent / ready-for-human /
wontfix）。见 `docs/agents/triage-labels.md`。

### Domain docs

查领域文档体系（single-context 结构）时：根目录 `CONTEXT.md` + `docs/adr/`。见 `docs/agents/domain.md`。
