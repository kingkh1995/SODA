# 仓库指南

## 项目概述

DDD 脚手架项目，将 [yudao](https://gitee.com/zhijiantianya/yudao-cloud) 改造为领域驱动架构。结合三个只读参考项目：

| 来源 | 目录 | 角色 |
|------|------|------|
| **yudao-cloud** | `yudao-cloud/` | 业务功能参考（14 模块） |
| **COLA** | `COLA/` | 架构框架参考（v5） |
| **kk-ddd** | `kk-ddd/` | 前序 DDD 尝试 |

> **使用守则**：三个参考项目均为只读，严禁修改。代码搜索时主动忽略。仅在用户要求时才探索代码。

**目标技术栈**：Java 25、Spring Boot 4、Gradle（Groovy DSL）

**groupId**：`com.soda`

## 架构

**读写分离** — 每个业务模块 7 个子模块：

```
soda-xxx/
├── api/          共享 DTO / Feign 接口（读写共用）
├── start/        写侧启动入口
├── adapter/      写 Controller（COLA）
├── application/  写 ApplicationService（COLA）
├── domain/       领域层（COLA）
├── infrastructure/  写 Repository 实现（COLA）
└── query-server/    读服务（yudao 混装风格）
```

写侧 DDD 分层追求质量，读侧简单混装追求性能。

## Agent skills

### Issue tracker

Issue 与 spec 存放于 `.scratch/<feature>/` 本地 markdown（GitHub 不用于追踪）。见 `docs/agents/issue-tracker.md`。

### Triage labels

五个标准 triage 角色使用默认标签串（needs-triage / needs-info / ready-for-agent / ready-for-human / wontfix）。见 `docs/agents/triage-labels.md`。

### Domain docs

Single-context：根目录 `CONTEXT.md` + `docs/adr/`。见 `docs/agents/domain.md`。