---
type: Repo Overview
title: SODA — DDD 脚手架
description: 人类了解项目 / 跑起来 / 看模块地图时读——DDD 脚手架概览；agent 工作入口见 AGENTS.md。
tags: [ repo, ddd, scaffold ]
status: stable
---

# SODA — DDD 脚手架

基于 yudao-cloud 业务功能改造的 DDD 脚手架项目：写侧 COLA 风格分层（api → adapter → application → domain →
infrastructure），读侧 yudao 混装风格（query-server）。写侧追求领域模型质量，读侧追求查询性能。

## 技术栈

Java 25 · Spring Boot 4 · Spring Modulith · Gradle（Groovy DSL）· groupId `com.soda`

## 快速开始

```bash
./gradlew :soda-user:soda-user-start:bootRun
```

启动 `SodaUserApplication`，`Started SodaUserApplication` 即成功，监听 8080。开发库为 H2 内存库（MySQL 兼容模式），Flyway 单
V1 脚本建表（ADR-0022）。

## 模块地图

| 模块               | 角色                                                                                                                                                            |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `soda-components/` | 分层 starter 基类族：api / domain / application / adapter（web·job·consumer）/ infrastructure / query-server / start ＋ domain-types（Domain Primitive 类型库） |
| `soda-user/`       | 首个业务模块：写侧 + 读服务（模块结构与依赖白名单见 `docs/framework-conventions.md` §1-3）                                                                      |

## 文档导航

| 文档            | 读者       | 内容                                        |
|-----------------|------------|---------------------------------------------|
| `AGENTS.md`     | agent      | 仓库工作入口（指针层）                      |
| `CONTEXT.md`    | 人 + agent | 领域词汇表（术语 + `_Avoid_`）              |
| `STYLEGUIDE.md` | 人 + agent | 编码规范 + 注释规范                         |
| `docs/`         | 人 + agent | ADR、约定、研究笔记（`docs/index.md` 索引） |

## 参考项目（只读）

| 来源           | 角色               |
|----------------|--------------------|
| `yudao-cloud/` | 业务功能参考       |
| `COLA/`        | 架构框架参考（v5） |
| `kk-ddd/`      | 前序 DDD 尝试      |

三个目录只读，禁止修改。

## License

MIT（见 `LICENSE`）。
