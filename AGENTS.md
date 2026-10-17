# SODA Agent 入口

本文件只负责把任务分支路由到正确的知识或工作流。

## 工作前

1. 识别任务分支，按「任务路由」命中行加载其唯一目标或目标组。
2. 命中 agent 工作流时，加载对应的 `docs/agents/*` 或 skill（见「Agent 工作流」）。
3. 按已加载目标中的步骤、约束与完成判据执行。

## 任务路由

| 触发分支 | 唯一目标或目标组 |
|---|---|
| 项目概览 / 跑起来 / 模块地图 | [README.md](README.md) |
| 领域术语 / 概念命名 | [CONTEXT.md](CONTEXT.md) |
| 架构决策 / 领域行为设计 | [docs/agents/domain.md](docs/agents/domain.md) ＋ [docs/adr/_index.md](docs/adr/_index.md) |
| 代码风格 / 注释（Java、SQL、YAML、Gradle） / 依赖管理 | [STYLEGUIDE.md](STYLEGUIDE.md) |
| 写 / 改 DP（设计、校验、JSON、敏感值、DP 测试） | [docs/dp-conventions.md](docs/dp-conventions.md) |
| 写 / 改测试 | [docs/test-conventions.md](docs/test-conventions.md) |
| Entity / 聚合 / 网关 / 持久化 / 字段清单 / 状态机 / 加密盲索引 | [docs/conventions/framework-type-contracts.md](docs/conventions/framework-type-contracts.md) |
| 领域事件 / 事件载荷 / 投递时机 | [docs/conventions/framework-type-contracts.md](docs/conventions/framework-type-contracts.md) ＋ [docs/conventions/framework-crosscutting.md](docs/conventions/framework-crosscutting.md) |
| 条件请求 / If-Match / 乐观锁版本 | [ADR-0037](docs/adr/0037-optimistic-lock-version-guard.md) ＋ [framework-type-contracts.md](docs/conventions/framework-type-contracts.md) |
| 应用编排 / DomainService / AppService / 异常类 / Logging | [docs/conventions/framework-crosscutting.md](docs/conventions/framework-crosscutting.md) |
| 终态守卫 / 聚合删除语义 | [framework-crosscutting.md](docs/conventions/framework-crosscutting.md) ＋ [ADR-0017](docs/adr/0017-aggregate-lifecycle-removal-semantics.md) ＋ [ADR-0023](docs/adr/0023-terminal-key-release-and-archive.md) |
| 新增模块 / 模块声明 / 模块边界 / allowedDependencies / Modulith | [docs/conventions/modulith.md](docs/conventions/modulith.md) |
| Spring 用法 / 事务 / 启动类 / 部署单元 | [docs/conventions/spring.md](docs/conventions/spring.md) |
| 持久化扫描根 / 实体扫描范围 | [ADR-0043](docs/adr/0043-persistence-scan-root-ownership.md) |
| 建表 / Flyway / 数据库开发 | [ADR-0022](docs/adr/0022-dev-database-management.md) ＋ [framework-crosscutting.md](docs/conventions/framework-crosscutting.md) |
| 设计 / 评审 Controller 端点与查询端点 / WebAssembler | [docs/conventions/aip-api-conventions.md](docs/conventions/aip-api-conventions.md) ＋ [docs/conventions/adapter.md](docs/conventions/adapter.md) |
| 错误结构 / 错误码 | [ADR-0013](docs/adr/0013-error-response-structure.md) ＋ [aip-api-conventions.md](docs/conventions/aip-api-conventions.md) |
| MapStruct 映射 | [docs/conventions/mapstruct.md](docs/conventions/mapstruct.md) |
| 写 / 改文档 / ADR | [docs/doc-conventions.md](docs/doc-conventions.md) |
| 写 / 检视 Jackson | [docs/conventions/jackson.md](docs/conventions/jackson.md) |
| 写 / 检视 JSpecify | [docs/conventions/jspecify.md](docs/conventions/jspecify.md) |
| 写 / 检视 Lombok | [docs/conventions/lombok.md](docs/conventions/lombok.md) |

## Agent 工作流

- Issue tracker / 写 spec → [docs/agents/issue-tracker.md](docs/agents/issue-tracker.md)
- Domain docs / ADR 消费 → [docs/agents/domain.md](docs/agents/domain.md)
- Triage / 分诊标签 → [docs/agents/triage-labels.md](docs/agents/triage-labels.md)
- 写 / 改 skills、`AGENTS.md` / `CLAUDE.md`、agent 入口或被入口指针加载的 reference → `skill://writing-for-agents`
