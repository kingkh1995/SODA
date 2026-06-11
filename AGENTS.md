# 仓库指南

## 项目概述

这是一个 **DDD 脚手架项目**，旨在将 [yudao](https://gitee.com/zhijiantianya/yudao-cloud)（芋道）微服务平台改造为领域驱动设计架构。项目结合了三个参考来源：

| 来源 | 角色 | 状态 |
|------|------|------|
| **yudao-cloud/** | 业务功能参考 — 14 个模块（system、infra、member、bpm、pay、mall、crm、erp、iot、mes、wms、im、mp、report） | 只读，已加入 gitignore |
| **COLA/** | 架构框架参考 — 阿里巴巴 DDD 脚手架（v5） | 只读，已加入 gitignore |
| **kk-ddd/** | 之前的 DDD 实现尝试 — 经验教训 | 只读，已加入 gitignore |

**目标技术栈**：Java 25、Spring Boot 4、Gradle（Groovy DSL）

**groupId**：`com.soda`

---

## 架构与数据流

### 核心理念：读写分离

每个业务模块拆分为 7 个子模块：`api`（共享 DTO/Feign）、`start`（写侧启动入口）、`adapter` + `app` + `domain` + `infrastructure`（写侧 DDD 4 层）、`query-server`（读服务简单混装）。读服务保持简单追求性能，写服务使用 DDD 追求质量。

## Agent skills

### Issue tracker

Issues and PRDs live as local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles use their default label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — `CONTEXT.md` at repo root + `docs/adr/`. See `docs/agents/domain.md`.