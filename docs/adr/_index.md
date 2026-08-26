---
type: Convention
title: ADR 主题聚类索引
description: 按主题分类浏览 ADR——写新 ADR / 跨 ADR 决策溯源时读。每个 ADR 的一句话范围 + 入口；不复制 ADR 正文。
tags: [adr, navigation, index]
status: stable
---

# ADR 主题聚类索引

按主题分组浏览 ADR；同一主题下 ADR 是「同一概念的不同面」——按入口先读粗体。完整决策与理由以各 ADR 正文为单源，本文件只承担导航。

> 命名约定：本文件以 `_` 前缀命名，进 `docs/adr/` 目录但不属于 ADR 本身；不参与 ADR
> 顺序编号，不进合规审计（详见 [doc-conventions §2](../doc-conventions.md)）。

## 架构与模块

- **[0001](0001-module-architecture.md)** — 读写分离业务模块分层（写侧 COLA / 读侧混装 / api 共用）
- **[0008](0008-layered-component-starters.md)** — 分层 starter 一架构层一组件模块
- **[0019](0019-adapter-starter-channel-split.md)** — Adapter starter 按入站通道拆分

## DDD 概念边界

- **[0002](0002-capabilities-scope.md)** — 缓存 / 锁 / 追踪不进 Entity 与 Aggregate
- **[0016](0016-type-class-mapping-ownership.md)** — 判别值映射归属（type↔class）
- **[0017](0017-aggregate-lifecycle-removal-semantics.md)** — 聚合删除语义：终态迁移，领域无擦除
- **[0023](0023-terminal-key-release-and-archive.md)** — 注销终态键释放与归档审计

## User / AuthAccount / 凭证

- **[0004](0004-account-polymorphism-and-persistence.md)** — AuthAccount 多态与单表持久化
- **[0006](0006-sealed-authaccount-hierarchy.md)** — AuthAccount sealed 层级
- **[0007](0007-authaccountid-centralized-jsoncreator.md)** — AuthAccountId 前缀编码与集中反序列化
- **[0010](0010-user-intention-revealing-methods.md)** — User 意图揭示方法（changeXxx / disable / enable）
- **[0027](0027-password-change-old-credential-guard.md)** — 自助改密原密码守卫

## Verification（验证码）

5 个 ADR 承载同一概念的 5 个面——按「拓扑 → 唯一性 → 双概念模型 → 模块位置」顺序读：

1. **[0011](0011-verification-cross-module-domain.md)** — 验证是独立于用户聚合的可复用领域概念
2. **[0021](0021-verification-aggregate-root-and-topology.md)** — Verification 聚合根认定与发码拓扑
3. **[0025](0025-verification-active-key-uniqueness.md)** — 活跃验证唯一性：active_key 数据库硬保证
4. **[0026](0026-verification-source-recipient.md)** — Verification 双概念模型：source 与 recipient
5. **[0029](0029-verification-domain-split.md)** — Verification 类型的模块位置

## DP 谱系

- **[0018](0018-random-string-alphabet.md)** — Alphabet DP（字符集升格）
- **[0020](0020-software-version-dp.md)** — SoftwareVersion 三段式版本号 DP
- **[0028](0028-literal-type-families.md)** — 字面量类型家族（五家族接口）
- **[0030](0030-data-protection-dp-categories.md)** — 数据保护 DP 四分类 + SensitiveValue 基类
- **[0031](0031-wire-semantic-literals-decimal-epochmilli.md)** — wire≠semantic 字面量（Decimal / EpochMilli）
- **[0032](0032-masked-value-dps-and-naming.md)** — Masked Value DP 与脱敏命名（三层词汇）
- **[0033](0033-crypto-hash-type-redesign.md)** — 加密族与哈希族类型设计（Ciphertext / PasswordHash / Digest）

## 持久化

- **[0022](0022-dev-database-management.md)** — 开发阶段数据库管理约定（H2-only / 单 V1 / 严禁外键与存储过程）
- **[0024](0024-gateway-save-unified-routing.md)** — Gateway save 统一路由（全权委托 Spring Data JPA）

## API 与错误

- **[0009](0009-adapter-web-req-resp-pattern.md)** — Adapter 层 Request/Response + WebAssembler 模式
- **[0012](0012-url-naming-convention.md)** — URL 与 HTTP 方法规范（Google AIP 风格）
- **[0013](0013-error-response-structure.md)** — 错误响应结构（ErrorInfo 语义详情）

## 异常与守卫

- **[0015](0015-exception-class-convention.md)** — 异常类使用约定（构造器校验 / 方法零守卫）

## 枚举

- **[0005](0005-enum-short-name.md)** — 枚举短名标识设计

## 文档元

- **[0036](0036-p3c-surface-format-retrofit.md)** — P3C 表面格式借鉴（DP / Entity / AIP / 注释规范）
