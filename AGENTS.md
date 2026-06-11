# 仓库指南

## 项目概述

DDD 脚手架项目，将 [yudao](https://gitee.com/zhijiantianya/yudao-cloud) 改造为领域驱动架构。结合三个只读参考项目：

| 来源 | 目录 | 角色 |
|------|------|------|
| **yudao-cloud** | `yudao-cloud/` | 业务功能参考（14 模块） |
| **COLA** | `COLA/` | 架构框架参考（v5） |
| **kk-ddd** | `kk-ddd/` | 前序 DDD 尝试 |

> **使用守则**：三个参考项目均为只读，严禁修改。代码搜索时主动忽略。仅在确定方案时查阅。

**目标技术栈**：Java 25、Spring Boot 4、Gradle（Groovy DSL）

**groupId**：`com.soda`

## 架构

**读写分离** — 每个业务模块 7 个子模块：

```
soda-xxx/
├── api/          共享 DTO / Feign 接口（读写共用）
├── start/        写侧启动入口
├── adapter/      写 Controller（COLA）
├── app/          写 ApplicationService（COLA）
├── domain/       领域层（COLA）
├── infrastructure/  写 Repository 实现（COLA）
└── query-server/    读服务（yudao 混装风格）
```

写侧 DDD 分层追求质量，读侧简单混装追求性能。

## Agent skills

- **Issue tracker**：`.scratch/` 下 markdown 文件。详见 `docs/agents/issue-tracker.md`
- **Triage**：五个角色使用默认标签。详见 `docs/agents/triage-labels.md`
- **Domain docs**：单上下文 — `CONTEXT.md` + `docs/adr/`。详见 `docs/agents/domain.md`
