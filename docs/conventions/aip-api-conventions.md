---
type: Convention
title: SODA HTTP API 设计规范（基于 Google AIP）
description: 设计/评审 SODA HTTP API（Controller 写侧 / 查询服务读侧 / 资源命名 / 字段 / 错误 / 兼容）时读——P3C 化【强制/推荐/参考】三级标定 + ❌/✅ 反例正例，章节按 google.aip.dev 全谱分组；与 docs/research/google-aip-api-design-spec.md 配套（Research 溯源 / 本规范定案）。
tags: [ aip, api-design, rest, p3c, convention ]
status: stable
---

# SODA HTTP API 设计规范（基于 Google AIP）

> **单源指针**：
> - **本规范**：SODA HTTP API 设计的 **定案**与 **项目偏离**。
> - **Research 溯源**：[google-aip-api-design-spec.md](../research/google-aip-api-design-spec.md) — Google AIP 51
    个主文档规则逐行收录 + SODA 已有引用链。
> - **AIP 全谱**：[google-aip-full-pull.md](../research/google-aip-full-pull.md) — 51/66 个 AIP
    规则表（[google.aip.dev](https://google.aip.dev) 分组）。
> - **现状基线**：[soda-current-api-state.md](../research/soda-current-api-state.md) — UserController 改造前 11 端点 +
    Result/ErrorInfo 旧信封 + 10 项明显偏差（历史基线，非现行形态）。
>
> **格式**：P3C【强制 / 推荐 / 参考】三级 + ❌/✅ 反例正例 + 表格列统一 `规则 | 级别 | AIP | SODA 落地 | 关联 ADR`。

---

## 目录

1. [概述](#1-概述)
2. [资源设计](#2-资源设计)
3. [HTTP 方法与操作](#3-http-方法与操作)
4. [字段设计](#4-字段设计)
5. [设计模式](#5-设计模式)
6. [向后兼容性](#6-向后兼容性)
7. [命名规范](#7-命名规范)
8. [错误处理](#8-错误处理)
9. [AIP 偏离表](#9-aip-偏离表)
10. [与现有 ADR / 文档的引用关系](#10-与现有-adr--文档的引用关系)

---

## 1. 概述

### 1.1 设计哲学

| 原则         | 说明                                                                 | 级别 | AIP                                   | 关联 ADR |
|--------------|----------------------------------------------------------------------|------|---------------------------------------|----------|
| 资源导向     | API 以命名资源（名词）为中心；操作围绕资源展开                       | 强制 | [AIP-121](https://google.aip.dev/121) | ADR-0017 |
| 标准方法优先 | 优先 Get / List / Create / Update / Delete；仅无法表达时用自定义方法 | 推荐 | [AIP-130](https://google.aip.dev/130) | ADR-0012 |
| 一致性       | 同一概念用同一名称；不同概念用不同名称                               | 推荐 | [AIP-190](https://google.aip.dev/190) | ADR-0009 |
| 无状态协议   | 每次请求独立；服务器负责持久化；客户端负责应用状态                   | 强制 | [AIP-121](https://google.aip.dev/121) | —        |

❌ 在 Controller 层混用 `UserService`（业务命名）与 `users`（资源命名）:

```java
// ❌ 业务命名暴露
@PostMapping("/createUser")  // AIP-136 严禁
public User createUser(...) {
}

// ✅ 资源命名 + 集合 POST
@PostMapping("/users")
public UserResponse createUser(...) {
}
```

### 1.2 方法选择优先级

按顺序选择（[AIP-130](https://google.aip.dev/130)）：

1. **标准方法**（Get / List / Create / Update / Delete）
2. **标准批量方法**（BatchGet / BatchCreate / BatchUpdate / BatchDelete）
3. **自定义方法**（挂载到资源或集合上的 `:verb`）
4. **流式方法**（最后手段）

**SODA 例外**：Get / List **走查询服务**（读侧架构），Controller 端 Get/List 端点 **可选实现**（读写分离偏离，见
§2.1）。写侧（Create / Update / Delete / 自定义方法）由 Controller 强制承担。设计方法时仍按 AIP-130 优先级选择：标准 → 批量 →
自定义 → 流式。

**SODA 语义二分**：标准方法（Create / Update / Delete）= 资源表示的纯 CRUD 操作，
属基础设施/持久化关注；自定义方法（`:verb`）= 领域行为（disable / enable / deregister /
changePassword 等），属领域关注。标准方法是"资源接口"，行为方法是"领域接口"。
例如注销是聚合根吸收态终态迁移（见 ADR-0017），与 disable / enable 同质，
由 `POST /{resource}/{id}:deregister` 承载， **不**使用标准 Delete。

### 1.3 适用范围

本规范适用于 SODA 项目的 **Adapter 层**（HTTP 边界）和 **API 层**（共享 DTO / Command / Query）。领域层和基础设施层遵循各自的
DDD 约定，不受本规范约束。

**SODA 架构**：

- **写侧**（DDD + COLA）：Controller → AppService → DomainService → Aggregate
- **读侧**（yudao 风格简单查询）：Controller（或查询服务）→ QueryService → 数据访问（直用 `soda-user-infrastructure`
  的 PO / Repository 混装；查询服务模块白名单需同步声明 `infrastructure`，如需领域类型再补 `domain`）

### 1.4 何时读 / 何时不读

- **何时读**：设计 / 评审 / 重构任何 SODA HTTP 端点（Controller / 查询服务）；起草新的 WebAssembler / Request / Response 时。
- **何时不读**：领域层逻辑（Aggregate / Entity / DP）—
  走 [dp-conventions](../dp-conventions.md) + [framework-type-contracts](framework-type-contracts.md)；基础设施（持久化、消息）—
  走 [framework-conventions](../framework-conventions.md)。

---

## 2. 资源设计

### 2.1 资源导向设计 [AIP-121](https://google.aip.dev/121)

| 规则                                     | 级别 | AIP                                   | SODA 落地               | 关联 ADR |
|------------------------------------------|------|---------------------------------------|-------------------------|----------|
| 资源是 API 的基本构建块                  | 强制 | [AIP-121](https://google.aip.dev/121) | Aggregate 映射为资源    | ADR-0017 |
| 资源必须支持 Get 方法                    | 强制 | [AIP-121](https://google.aip.dev/121) | **不强制** — 走查询服务 | —        |
| 资源必须支持 List 方法（单例除外）       | 强制 | [AIP-121](https://google.aip.dev/121) | **不强制** — 走查询服务 | —        |
| 同一资源 schema 在所有标准方法中必须一致 | 强制 | [AIP-121](https://google.aip.dev/121) | XxxResponse 单一来源    | —        |
| 资源关系是有向无环图（DAG）              | 强制 | [AIP-121](https://google.aip.dev/121) | DDD 聚合根天然契合      | ADR-0017 |

❌ UserController 同时承担写侧（Create / Update / Delete）与读侧（Get / List）:

```text
# SODA 偏离 AIP-121 Get/List 强制：读写分离
# 写侧：Controller → AppService → DomainService
# 读侧：Controller（或查询服务）→ QueryService → 数据访问
```

### 2.2 资源名称 [AIP-122](https://google.aip.dev/122)

| 规则                                                   | 级别 | AIP                                   | SODA 落地                              | 关联 ADR |
|--------------------------------------------------------|------|---------------------------------------|----------------------------------------|----------|
| 资源名称在 API 内必须唯一                              | 强制 | [AIP-122](https://google.aip.dev/122) | 集合路径唯一                           | ADR-0012 |
| 集合标识符必须是 camelCase 复数                        | 强制 | [AIP-122](https://google.aip.dev/122) | `users` / `accounts` / `orders`        | ADR-0012 |
| 集合标识符必须以小写字母开头，仅含 `[a-z][a-zA-Z0-9]*` | 强制 | [AIP-122](https://google.aip.dev/122) | 同上                                   | ADR-0012 |
| 集合标识符不得生造复数（如 `infos`）                   | 强制 | [AIP-122](https://google.aip.dev/122) | `users` 而非 `userInfos`               | ADR-0012 |
| 嵌套集合可省略父集合前缀                               | 推荐 | [AIP-122](https://google.aip.dev/122) | `users/{id}/events/{eid}`（省略 user） | —        |
| 资源名称用 `name` 字符串格式（`collection/{id}`）      | 强制 | [AIP-122](https://google.aip.dev/122) | **不遵守** — 沿用 Long ID 路径变量     | ADR-0017 |

❌ 集合名生造复数或用大写:

```text
GET /UserInfo           # ❌ 大写 + 复数生造
GET /user-infos         # ❌ kebab-case
GET /userInfos          # ❌ 复数生造

GET /users              # ✅ camelCase 复数
GET /users/{id}         # ✅ 嵌套省略 + 强类型 Long ID
```

### 2.3 资源类型 [AIP-123](https://google.aip.dev/123)

| 规则                                               | 级别 | AIP                                   | SODA 落地                                    | 关联 ADR |
|----------------------------------------------------|------|---------------------------------------|----------------------------------------------|----------|
| 类型名必须以大写字母开头，仅含字母数字，PascalCase | 强制 | [AIP-123](https://google.aip.dev/123) | `User` / `Account` / `Order`                 | —        |
| 类型名必须是名词的单数形式                         | 强制 | [AIP-123](https://google.aip.dev/123) | 同上                                         | —        |
| 类型格式 `{ServiceName}/{Type}`                    | 强制 | [AIP-123](https://google.aip.dev/123) | 概念映射到 `com.soda.{module}.domain.{Type}` | —        |
| 用 `google.api.resource` 注解标注                  | 推荐 | [AIP-123](https://google.aip.dev/123) | **不遵守** — Java REST 无原生支持            | —        |

### 2.4 资源关联 [AIP-124](https://google.aip.dev/124)

| 规则                              | 级别 | AIP                                   | SODA 落地                      | 关联 ADR |
|-----------------------------------|------|---------------------------------------|--------------------------------|----------|
| 每个资源最多一个规范父资源        | 强制 | [AIP-124](https://google.aip.dev/124) | DDD 聚合根天然契合             | ADR-0017 |
| List 请求不得要求两个不同的父资源 | 强制 | [AIP-124](https://google.aip.dev/124) | 单一 parent 字段               | —        |
| 多对多关系用重复字段或子资源      | 推荐 | [AIP-124](https://google.aip.dev/124) | 子资源 + Add/Remove 自定义方法 | —        |

### 2.5 枚举 [AIP-126](https://google.aip.dev/126)

| 规则                               | 级别 | AIP                                   | SODA 落地                               | 关联 ADR |
|------------------------------------|------|---------------------------------------|-----------------------------------------|----------|
| 枚举值必须使用 UPPER_SNAKE_CASE    | 强制 | [AIP-126](https://google.aip.dev/126) | **不遵守** — 沿用单字母短名 + i18n desc | ADR-0005 |
| 第一个值应是 `{ENUM}_UNSPECIFIED`  | 推荐 | [AIP-126](https://google.aip.dev/126) | 现有枚举未含；新增枚举补                | ADR-0005 |
| 枚举应仅用于变化不频繁的值集合     | 推荐 | [AIP-126](https://google.aip.dev/126) | 短名 + i18n key 模式                    | ADR-0005 |
| 变化频繁的值集合应使用 string      | 推荐 | [AIP-126](https://google.aip.dev/126) | —                                       | —        |
| 单消息内使用的枚举应嵌套在该消息中 | 推荐 | [AIP-126](https://google.aip.dev/126) | —                                       | —        |

❌ 枚举值用 UPPER_SNAKE_CASE 短词（与 ADR-0005 冲突）:

```java
// ❌ 短词
public enum UserState {ACTIVE, DISABLED, LOCKED}

// ✅ 单字母短名 + desc 为英文 i18n key
public enum UserState {
    E("enabled"),
    D("disabled"),
    L("locked");
    // ADR-0005: 短名存储 + i18n 备扩展
}
```

### 2.6 单例资源 [AIP-156](https://google.aip.dev/156)

| 规则                            | 级别 | AIP                                   | SODA 落地                              | 关联 ADR |
|---------------------------------|------|---------------------------------------|----------------------------------------|----------|
| 单例资源不得有 Create / Delete  | 强制 | [AIP-156](https://google.aip.dev/156) | 当前无场景；未来如 `UserSettings` 适用 | —        |
| 单例资源不得有用户指定或系统 ID | 强制 | [AIP-156](https://google.aip.dev/156) | 同上                                   | —        |
| 单例资源应定义 Get 和 Update    | 推荐 | [AIP-156](https://google.aip.dev/156) | 同上                                   | —        |

---

## 3. HTTP 方法与操作

### 3.1 方法选择优先级

见 §1.2 — 标准 → 批量 → 自定义 → 流式；SODA 例外：Get / List 走查询服务。

### 3.2 Get 方法 [AIP-131](https://google.aip.dev/131)

| 规则                                           | 级别 | AIP                                   | SODA 落地                       | 关联 ADR |
|------------------------------------------------|------|---------------------------------------|---------------------------------|----------|
| HTTP 动词必须是 GET                            | 强制 | [AIP-131](https://google.aip.dev/131) | 查询服务 `@GetMapping("/{id}")` | —        |
| Get 不得有 body 字段                           | 强制 | [AIP-131](https://google.aip.dev/131) | 路径参数 + 查询参数             | —        |
| Get 响应必须是资源本身（无独立 Response 类型） | 强制 | [AIP-131](https://google.aip.dev/131) | 复用 Controller `XxxResponse`   | —        |
| Controller 端 Get 可选实现                     | 参考 | [AIP-131](https://google.aip.dev/131) | 写侧不强制；查询服务必需        | —        |
| 查询服务响应复用 Controller `XxxResponse`      | 强制 | [AIP-131](https://google.aip.dev/131) | 跨读侧统一，落点待定（见下）    | —        |

> **资源表示落点待定**：`XxxResponse` 现位于写侧 adapter 模块，而查询服务模块白名单为 `{api}`（编译期不可见）——
> 复用落点（上提 api 模块 / 放开白名单 / schema 同形三择）随读侧设计一并确定，本次范围外。

### 3.3 List 方法 [AIP-132](https://google.aip.dev/132)

| 规则                                           | 级别 | AIP                                   | SODA 落地        | 关联 ADR |
|------------------------------------------------|------|---------------------------------------|------------------|----------|
| HTTP 动词必须是 GET                            | 强制 | [AIP-132](https://google.aip.dev/132) | `@GetMapping`    | —        |
| List 必须包含 `page_size` 与 `page_token` 字段 | 强制 | [AIP-132](https://google.aip.dev/132) | 查询服务 Request | —        |
| List 响应必须包含 `next_page_token`            | 强制 | [AIP-132](https://google.aip.dev/132) | 响应字段         | —        |
| Controller 端 List 可选实现                    | 参考 | [AIP-132](https://google.aip.dev/132) | 走查询服务       | —        |

详见 §5.1 分页。

### 3.4 Create 方法 [AIP-133](https://google.aip.dev/133)

| 规则                              | 级别 | AIP                                   | SODA 落地                          | 关联 ADR |
|-----------------------------------|------|---------------------------------------|------------------------------------|----------|
| HTTP 动词必须是 POST              | 强制 | [AIP-133](https://google.aip.dev/133) | `@PostMapping`                     | —        |
| Create 响应必须是资源本身         | 强制 | [AIP-133](https://google.aip.dev/133) | 完整 `XxxResponse`                 | —        |
| 重复创建必须返回 `ALREADY_EXISTS` | 强制 | [AIP-133](https://google.aip.dev/133) | `409`（见 §8.2）                   | ADR-0013 |
| 请求必传资源 ID（admin plane）    | 强制 | [AIP-133](https://google.aip.dev/133) | **不遵守** — data plane 服务端生成 | ADR-0017 |
| 请求可选传资源 ID（data plane）   | 推荐 | [AIP-133](https://google.aip.dev/133) | `UserController.createUser`        | ADR-0017 |

**SODA data plane / admin plane 双轨**：

- **data plane**（用户自助）：请求可选传 ID；不传时服务端生成（Long ID by Snowflake）
- **admin plane**（管理端）：请求必传 ID；重复创建 ALREADY_EXISTS

❌ Data plane 强制传 ID（与 ADR-0017 冲突）:

```java
// ❌ UserController 强制 ID
public record CreateUserRequest(@NotNull Long id, ...) {
}

// ✅ Data plane 可选 ID
public record CreateUserRequest(@Nullable Long id, ...) {
}
// 服务端：id == null ? generateId() : id
```

### 3.5 Update 方法 [AIP-134](https://google.aip.dev/134)

| 规则                               | 级别 | AIP                                   | SODA 落地                                                                                                                                                                                                                                                                                         | 关联 ADR |
|------------------------------------|------|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| HTTP 动词应为 PATCH                | 强制 | [AIP-134](https://google.aip.dev/134) | `@PatchMapping`                                                                                                                                                                                                                                                                                   | ADR-0012 |
| 强烈不推荐用 PUT（向后不兼容）     | 强制 | [AIP-134](https://google.aip.dev/134) | **禁 PUT**                                                                                                                                                                                                                                                                                        | ADR-0012 |
| Update 必须包含 `update_mask` 字段 | 强制 | [AIP-134](https://google.aip.dev/134) | 逗号分隔 string                                                                                                                                                                                                                                                                                   | —        |
| `update_mask` 必须支持 `*` 通配符  | 强制 | [AIP-134](https://google.aip.dev/134) | 传 `*` = 全量替换，解析期展开为白名单全集（所有字段无条件写入，null 即清空）                                                                                                                                                                                                                      | ADR-0038 |
| Update 响应必须是资源本身          | 强制 | [AIP-134](https://google.aip.dev/134) | —                                                                                                                                                                                                                                                                                                 | —        |
| Update 不得触发副作用              | 强制 | [AIP-134](https://google.aip.dev/134) | 仅写字段                                                                                                                                                                                                                                                                                          | —        |
| 状态字段不得在 Update 中直接写入   | 强制 | [AIP-134](https://google.aip.dev/134) | 走 `:enable` / `:disable`                                                                                                                                                                                                                                                                         | ADR-0005 |
| 省略 update_mask 视为已填充字段    | 强制 | [AIP-134](https://google.aip.dev/134) | 省略 / 空白 = 隐式掩码「全部已填充字段」：仅写值非 null 的字段（全量替换由 `*` 表达，见上行）                                                                                                                                                                                                     | ADR-0038 |
| Update 必须支持 `etag` 乐观锁通道  | 强制 | [AIP-154](https://google.aip.dev/154) | `version: int` 承担 etag 角色（不新增 `etag` 字段）；`If-Match` 归一化 → `expectedVersion`（`@IfMatch` 参数仅支持 `Integer`），`HttpValidatorHeadersAdvice` 由 `HttpValidatorSource` 派生 `ETag: "n"`；**提供即校验，失配 → 412**；缺失 = 未做并发声明 → 放行（AIP-154 只约束「提供了必须校验」） | ADR-0039 |

❌ PUT Update（新增字段会静默丢失）:

```http
# ❌ PUT 全量替换，新增字段被擦除
PUT /users/1
{ "username": "alice", "mobile": "138..." }
# 后续若 API 新增 email 字段,旧 PUT 请求不会带 email,服务端默认 null → 丢失

# ✅ PATCH 部分更新 + update_mask
PATCH /users/1
{ "update_mask": "username,mobile", "username": "alice", "mobile": "138..." }
```

### 3.6 Delete 方法 [AIP-135](https://google.aip.dev/135)

| 规则                                                                               | 级别 | AIP                                                                          | SODA 落地                                                                                                          | 关联 ADR            |
|------------------------------------------------------------------------------------|------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|---------------------|
| HTTP 动词必须是 DELETE                                                             | 强制 | [AIP-135](https://google.aip.dev/135)                                        | `@DeleteMapping`                                                                                                   | —                   |
| Delete 不得有 body 字段                                                            | 强制 | [AIP-135](https://google.aip.dev/135)                                        | 路径参数                                                                                                           | —                   |
| 响应：硬删除返 Empty，软删除分支返资源本身                                         | 推荐 | [AIP-135](https://google.aip.dev/135)、[AIP-164](https://google.aip.dev/164) | User 无标准 Delete；deregister 是领域行为（`:deregister`），返 R 态 `UserResponse`（领域终态快照；键为释放前原值） | ADR-0017 / ADR-0023 |
| `allow_missing`（已删/从未存在 → 成功 no-op）                                      | 参考 | [AIP-135](https://google.aip.dev/135)                                        | **不做** — 无字段；不存在 → 404（`require` 守卫），重复注销 → 400（终态拒绝 IAE）                                  | —                   |
| 保护删除可用 etag（失配 → ABORTED，HTTP 层译 412）                                 | 参考 | [AIP-135](https://google.aip.dev/135)                                        | Update 共用 `If-Match → expectedVersion` 通道；deregister 为领域行为暂未接 `If-Match`，D→R 严格前置已拒并发误注销  | —                   |
| 存在子资源时必须返回 `FAILED_PRECONDITION`                                         | 强制 | [AIP-135](https://google.aip.dev/135)                                        | 400（AIP 语义 `FAILED_PRECONDITION`；状态码承载，见 §8.2；当前无级联场景）                                         | ADR-0013            |
| 资源不存在应返回 `NOT_FOUND`                                                       | 推荐 | [AIP-135](https://google.aip.dev/135)                                        | 404（`require` 守卫抛 `NotFoundException.entityNotFound`，见 §8.2）                                                | ADR-0013            |
| 级联删除应提供 `bool force` 字段                                                   | 推荐 | [AIP-135](https://google.aip.dev/135)                                        | **不强制** — SODA 当前无级联场景                                                                                   | —                   |
| SODA 无标准 Delete；deregister = 领域行为（AIP-136 `:deregister`），不可逆终态迁移 | 强制 | [AIP-136](https://google.aip.dev/136)                                        | D→R 吸收态（见 §1.2 语义二分）；行保留 + 三键置 NULL + `user_archive` 快照                                         | ADR-0017 / ADR-0023 |

### 3.7 自定义方法 [AIP-136](https://google.aip.dev/136)

| 规则                                             | 级别 | AIP                                   | SODA 落地                                                                                                                                                               | 关联 ADR |
|--------------------------------------------------|------|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| 自定义方法仅用于标准方法无法表达的功能           | 推荐 | [AIP-136](https://google.aip.dev/136) | 当前 9 个 `:verb` 端点（含 `:deregister` 领域终态迁移）                                                                                                                 | ADR-0012 |
| HTTP 必须是 GET（无副作用）或 POST（有副作用）   | 强制 | [AIP-136](https://google.aip.dev/136) | 写操作必须 POST；发码保持 POST（生成/发送/计数均属副作用）；如未来需查询发码状态可走 `GET :codeStatus`                                                                  | —        |
| URI 必须用 `:` 后跟自定义动词                    | 强制 | [AIP-136](https://google.aip.dev/136) | `POST /users/{id}:disable`                                                                                                                                              | ADR-0012 |
| URI 中的动词必须与 RPC 名一致，camelCase         | 强制 | [AIP-136](https://google.aip.dev/136) | —                                                                                                                                                                       | ADR-0012 |
| 自定义方法名不得包含介词（for / with / at / by） | 强制 | [AIP-136](https://google.aip.dev/136) | 全部 `:verb` 端点合规；曾含 `request` 介词的 `:requestChangeMobileCode` / `:requestChangeEmailCode` 已按 ADR-0012 收紧为 `:requestChangeMobile` / `:requestChangeEmail` | ADR-0012 |
| 自定义方法名不得是标准方法动词                   | 推荐 | [AIP-136](https://google.aip.dev/136) | 不得 `getUser` / `createUser`                                                                                                                                           | —        |
| 自定义方法名不得包含 `Async`                     | 强制 | [AIP-136](https://google.aip.dev/136) | —                                                                                                                                                                       | —        |
| body 应为 `"*"`                                  | 推荐 | [AIP-136](https://google.aip.dev/136) | —                                                                                                                                                                       | —        |
| 请求消息匹配 RPC 名 + Request 后缀               | 推荐 | [AIP-136](https://google.aip.dev/136) | `ChangePasswordRequest`                                                                                                                                                 | ADR-0009 |

**SODA 自定义方法命名示例**（`UserController`）：

| 操作       | URI                               | RPC 名           | 语义                           |
|------------|-----------------------------------|------------------|--------------------------------|
| 注销用户   | `POST /users/{id}:deregister`     | `Deregister`     | 领域终态迁移（吸收态，不可逆） |
| 禁用用户   | `POST /users/{id}:disable`        | `Disable`        | 单动词                         |
| 启用用户   | `POST /users/{id}:enable`         | `Enable`         | 单动词                         |
| 修改密码   | `POST /users/{id}:changePassword` | `ChangePassword` | 动词+名词                      |
| 修改用户名 | `POST /users/{id}:changeUsername` | `ChangeUsername` | 动词+名词                      |
| 修改手机号 | `POST /users/{id}:changeMobile`   | `ChangeMobile`   | 动词+名词                      |
| 修改邮箱   | `POST /users/{id}:changeEmail`    | `ChangeEmail`    | 动词+名词                      |

❌ 旧形态 `requestChangeMobileCode`（含 `request` 隐含介词，已废弃）:

```text
# ❌ requestChangeMobileCode — 含 request 介词 + Code 后缀
POST /users/{id}:requestChangeMobileCode

# ✅ requestChangeMobile — 已落地（ADR-0012）
POST /users/{id}:requestChangeMobile
```

### 3.8 Batch 方法 [AIP-231/233/234/235](https://google.aip.dev/231)

| 规则                                                                          | 级别 | AIP                                   | SODA 落地  | 关联 ADR |
|-------------------------------------------------------------------------------|------|---------------------------------------|------------|----------|
| BatchGet：URI 末尾 `:batchGet`，GET，atomic，不分页                           | 参考 | [AIP-231](https://google.aip.dev/231) | 当前无场景 | —        |
| BatchCreate：URI 末尾 `:batchCreate`，POST，atomic 或 partial success via LRO | 参考 | [AIP-233](https://google.aip.dev/233) | 同上       | —        |
| BatchUpdate：URI 末尾 `:batchUpdate`，POST，atomic 或 partial success via LRO | 参考 | [AIP-234](https://google.aip.dev/234) | 同上       | —        |
| BatchDelete：URI 末尾 `:batchDelete`，**POST（非 DELETE）**                   | 参考 | [AIP-235](https://google.aip.dev/235) | 同上       | —        |

### 3.9 Long-running operations [AIP-151](https://google.aip.dev/151)

| 规则                                                        | 级别 | AIP                                   | SODA 落地                  | 关联 ADR |
|-------------------------------------------------------------|------|---------------------------------------|----------------------------|----------|
| 耗时较长的方法应返回 `google.longrunning.Operation`         | 推荐 | [AIP-151](https://google.aip.dev/151) | 当前同步响应；异步场景按此 | —        |
| LRO 必须指定 `response_type` 与 `metadata_type`             | 强制 | [AIP-151](https://google.aip.dev/151) | —                          | —        |
| LRO 必须实现 `google.longrunning.Operations` 服务           | 强制 | [AIP-151](https://google.aip.dev/151) | —                          | —        |
| 修改 LRO 的 `response_type` 或 `metadata_type` 是破坏性变更 | 强制 | [AIP-151](https://google.aip.dev/151) | —                          | —        |

**SODA 场景**：UCC 发码 / 导入导出（ADR-0026 Verification 模型）/ 报表生成 未来可能需要。

---

## 4. 字段设计

### 4.1 字段命名 [AIP-140](https://google.aip.dev/140)

| 规则                                       | 级别 | AIP                                                                           | SODA 落地                  | 关联 ADR |
|--------------------------------------------|------|-------------------------------------------------------------------------------|----------------------------|----------|
| JSON 层字段名 lower_snake_case             | 强制 | [AIP-140](https://google.aip.dev/140)                                         | `@JsonProperty("xxx_yyy")` | ADR-0012 |
| Java 属性名 camelCase                      | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | Java 惯例                  | —        |
| 字段名应为美式英语                         | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | —                          | —        |
| 重复字段必须用复数                         | 强制 | [AIP-140](https://google.aip.dev/140)                                         | `roles` / `permissions`    | —        |
| 非重复字段应用单数                         | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | `username` / `email`       | —        |
| 字段名不得包含介词（with / for / at / by） | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | —                          | —        |
| 字段名不得是动词，必须是名词               | 强制 | [AIP-140](https://google.aip.dev/140)                                         | —                          | —        |
| 布尔字段应省略 `is_` 前缀                  | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | `enabled` / `disabled`     | —        |
| URI 字段用 `uri`，URL 字段用 `url`         | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | —                          | —        |
| 人类可读名称字段应用 `display_name`        | 推荐 | [AIP-140](https://google.aip.dev/140) + [AIP-148](https://google.aip.dev/148) | —                          | —        |
| 字段名应避免编程语言保留字                 | 推荐 | [AIP-140](https://google.aip.dev/140)                                         | —                          | —        |

常用缩写（推荐/避免）：

| 推荐          | 避免                  |
|---------------|-----------------------|
| `config`      | `configuration`       |
| `id`          | `identifier`          |
| `info`        | `information`         |
| `spec`        | `specification`       |
| `stats`       | `statistics`          |
| `distance_km` | `distance_kilometers` |

❌ 字段名带介词 / 过去时 / 复数生造:

```json
{
  "userName": "alice",
  "isActive": true,
  "firstName": "Alice",
  "publishedTime": "2025-01-02T10:30:00Z",
  "userInfos": [
    ...
  ]
}
```

✅ 字段名遵守 AIP-140:

```json
{
  "user_name": "alice",
  "active": true,
  "given_name": "Alice",
  "publish_time": "2025-01-02T10:30:00Z",
  "users": [
    ...
  ]
}
```

### 4.2 时间与持续时间 [AIP-142](https://google.aip.dev/142)

| 规则                                                    | 级别 | AIP                                   | SODA 落地                           | 关联 ADR |
|---------------------------------------------------------|------|---------------------------------------|-------------------------------------|----------|
| 时间戳字段 JSON 层 RFC-3339 格式                        | 推荐 | [AIP-142](https://google.aip.dev/142) | **不遵守** — 沿用 DecimalEpochMilli | ADR-0031 |
| 时长字段用 `google.protobuf.Duration`（JSON: `"1.5s"`） | 推荐 | [AIP-142](https://google.aip.dev/142) | 同上                                | ADR-0031 |
| 时间戳字段名以 `_time` / `_times` 结尾                  | 推荐 | [AIP-142](https://google.aip.dev/142) | `create_time` / `update_time`       | —        |
| 字段名不应使用过去时                                    | 推荐 | [AIP-142](https://google.aip.dev/142) | `create_time` 而非 `created_time`   | —        |
| 民用日期用 `google.type.Date`，字段名以 `_date` 结尾    | 推荐 | [AIP-142](https://google.aip.dev/142) | —                                   | —        |
| 相对时段字段名以 `_offset` 结尾                         | 推荐 | [AIP-142](https://google.aip.dev/142) | —                                   | —        |

### 4.3 标准化代码 [AIP-143](https://google.aip.dev/143)

| 规则                                           | 级别 | AIP                                   | SODA 落地 | 关联 ADR |
|------------------------------------------------|------|---------------------------------------|-----------|----------|
| 标准化代码字段必须用 string 类型               | 强制 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 字段名以 `_code` 或 `_type` 结尾               | 强制 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 国家/地区：Unicode CLDR，字段名 `region_code`  | 强制 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 货币：ISO-4217，字段名 `currency_code`         | 强制 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 语言：IETF BCP-47，字段名 `language_code`      | 强制 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 时区：IANA TZ，字段名 `time_zone`              | 推荐 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 内容类型：IANA media types，字段名 `mime_type` | 强制 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 输入大小写不敏感；输出规范大小写               | 推荐 | [AIP-143](https://google.aip.dev/143) | —         | —        |
| 不应使用枚举表示标准代码                       | 推荐 | [AIP-143](https://google.aip.dev/143) | —         | —        |

❌ 枚举表示标准代码:

```java
// ❌ 枚举
public enum Country {US, CN, JP}

// ✅ string + 字段名 _code
public record User(@JsonProperty("region_code") String regionCode) {
}
// "US" / "CN" / "JP"
```

### 4.4 重复字段 [AIP-144](https://google.aip.dev/144)

| 规则                                             | 级别 | AIP                                   | SODA 落地                  | 关联 ADR |
|--------------------------------------------------|------|---------------------------------------|----------------------------|----------|
| 重复字段名必须用复数                             | 强制 | [AIP-144](https://google.aip.dev/144) | —                          | —        |
| 重复字段应有上限（经验值 ~100），超限用子资源    | 推荐 | [AIP-144](https://google.aip.dev/144) | —                          | —        |
| 重复字段不得内联另一个资源的 body                | 强制 | [AIP-144](https://google.aip.dev/144) | —                          | —        |
| 首选 scalar 类型（string）                       | 推荐 | [AIP-144](https://google.aip.dev/144) | `roles: ["admin", "user"]` | —        |
| 需原子修改时应用 Add / Remove 自定义方法（POST） | 推荐 | [AIP-144](https://google.aip.dev/144) | —                          | —        |
| Add 方法遇已存在数据必须返回 `ALREADY_EXISTS`    | 强制 | [AIP-144](https://google.aip.dev/144) | `409`（见 §8.2）           | ADR-0013 |
| Remove 方法遇不存在数据必须返回 `NOT_FOUND`      | 强制 | [AIP-144](https://google.aip.dev/144) | `404`（见 §8.2）           | ADR-0013 |

### 4.5 标准字段 [AIP-148](https://google.aip.dev/148)

| 字段                         | 类型                 | 行为                           | SODA 落地                                                                                                                                            | 关联 ADR |
|------------------------------|----------------------|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `name`                       | `string`             | IDENTIFIER                     | **不遵守** — Long ID 路径                                                                                                                            | ADR-0017 |
| `parent`                     | `string`             | REQUIRED（List / Create 请求） | 参考                                                                                                                                                 | —        |
| `display_name`               | `string`             | OPTIONAL，≤63 字符             | 推荐                                                                                                                                                 | —        |
| `title`                      | `string`             | OPTIONAL                       | 参考                                                                                                                                                 | —        |
| `create_time`                | `Timestamp`          | OUTPUT_ONLY                    | **不遵守** — Entity 暂不暴露前端                                                                                                                     | —        |
| `update_time`                | `Timestamp`          | OUTPUT_ONLY                    | 同上                                                                                                                                                 | —        |
| `delete_time`                | `Timestamp`          | OUTPUT_ONLY                    | 仅软删除场景（见 §5.5）                                                                                                                              | ADR-0017 |
| `purge_time`                 | `Timestamp`          | OUTPUT_ONLY                    | 同上                                                                                                                                                 | ADR-0023 |
| `expire_time`                | `Timestamp`          | OPTIONAL                       | 参考                                                                                                                                                 | —        |
| `uid`                        | `string`             | OUTPUT_ONLY，UUID4             | 参考                                                                                                                                                 | —        |
| `etag`                       | `string`             | —                              | `version: int`（强 validator 源，派生 `ETag: "n"`；弱 validator `Last-Modified` 不提供——审计列不入出站模型（ADR-0031），RFC 7232 §2.4 双发降为单发） | —        |
| `annotations`                | `map<string,string>` | —                              | 参考                                                                                                                                                 | —        |
| `given_name` / `family_name` | `string`             | —                              | 不得用 `first_name` / `last_name`                                                                                                                    | —        |

### 4.6 未设置字段值 [AIP-149](https://google.aip.dev/149)

| 规则                                                                 | 级别 | AIP                                   | SODA 落地                         | 关联 ADR |
|----------------------------------------------------------------------|------|---------------------------------------|-----------------------------------|----------|
| DTO 字段用包装类型（Integer / Long / Boolean）区分 null vs 0 / false | 推荐 | [AIP-149](https://google.aip.dev/149) | 裸返回资源（null 字段按需序列化） | —        |

❌ int 字段区分「未设置」:

```java
// ❌ int 无法表达 null
public record User(@JsonProperty("age") int age) {
}

// ✅ Integer 包装类型
public record User(@JsonProperty("age") @Nullable Integer age) {
}
// null = 未设置；0 = 显式设为 0
```

### 4.7 字段格式 [AIP-202](https://google.aip.dev/202)

| 规则                                     | 级别 | AIP                                   | SODA 落地                                                  | 关联 ADR |
|------------------------------------------|------|---------------------------------------|------------------------------------------------------------|----------|
| 字段格式用 Bean Validation 注解          | 强制 | [AIP-202](https://google.aip.dev/202) | `@Email` / `@Pattern` / `@Size` / `@URL` / `@Min` / `@Max` | —        |
| UUID4 / IPV4 / IPV6 字段为 string 类型   | 强制 | [AIP-202](https://google.aip.dev/202) | Hibernate Validator `@UUID` / `@Length`                    | —        |
| 格式字段不得用原始文本比较做等价判断     | 强制 | [AIP-202](https://google.aip.dev/202) | Java `equals`                                              | —        |
| 新格式必须由 IETF RFC 或 Google AIP 管理 | 强制 | [AIP-202](https://google.aip.dev/202) | —                                                          | —        |

❌ 自定义正则校验:

```java
// ❌ 自定义正则
@Pattern(regexp = "^1[3-9]\\d{9}$", message = "invalid mobile")
private String mobile;

// ✅ 标准 Bean Validation
@org.hibernate.validator.constraints.UUID
private String userUid;
```

### 4.8 字段行为 [AIP-203](https://google.aip.dev/203)

| 行为                         | SODA 落地                               | 关联 ADR |
|------------------------------|-----------------------------------------|----------|
| REQUIRED                     | `@NotNull` / `@NotBlank`                | —        |
| OPTIONAL                     | `@Nullable`                             | —        |
| OUTPUT_ONLY                  | Javadoc `@outputOnly`                   | —        |
| IMMUTABLE                    | Javadoc `@immutable`                    | —        |
| INPUT_ONLY                   | Javadoc `@inputOnly`                    | —        |
| IDENTIFIER                   | Javadoc `@identifier`（仅 `name` 字段） | —        |
| `FIELD_BEHAVIOR_UNSPECIFIED` | **不得使用**                            | —        |

### 4.9 状态字段 [AIP-216](https://google.aip.dev/216)

| 规则                                           | 级别 | AIP                                   | SODA 落地                | 关联 ADR |
|------------------------------------------------|------|---------------------------------------|--------------------------|----------|
| 状态字段应使用枚举，名为 `state`               | 强制 | [AIP-216](https://google.aip.dev/216) | 字段名 `state`           | ADR-0005 |
| 状态字段应 OUTPUT_ONLY                         | 强制 | [AIP-216](https://google.aip.dev/216) | Javadoc 标 `@outputOnly` | —        |
| 状态转换用自定义方法（POST `:verb`）           | 强制 | [AIP-216](https://google.aip.dev/216) | `:enable` / `:disable`   | ADR-0012 |
| 状态枚举首个值应为 `STATE_UNSPECIFIED`         | 推荐 | [AIP-216](https://google.aip.dev/216) | 现有枚举未含；新增枚举补 | ADR-0005 |
| 状态转换不允许时必须返回 `FAILED_PRECONDITION` | 强制 | [AIP-216](https://google.aip.dev/216) | `400`（见 §8.2）         | ADR-0013 |
| 状态枚举不得通过 Update 方法直接更新           | 强制 | [AIP-216](https://google.aip.dev/216) | UpdateMask 过滤          | —        |

---

## 5. 设计模式

### 5.1 分页 [AIP-158](https://google.aip.dev/158)

| 规则                                            | 级别 | AIP                                   | SODA 落地          | 关联 ADR |
|-------------------------------------------------|------|---------------------------------------|--------------------|----------|
| 返回集合的 RPC 必须一开始就提供分页             | 强制 | [AIP-158](https://google.aip.dev/158) | 查询服务 List 强制 | —        |
| `page_size` 不得是必填字段                      | 强制 | [AIP-158](https://google.aip.dev/158) | `@Nullable`        | —        |
| 未指定 `page_size`（0）服务端选默认值，不得报错 | 强制 | [AIP-158](https://google.aip.dev/158) | 服务端默认         | —        |
| `page_size` 大于最大值时应降级到最大值          | 推荐 | [AIP-158](https://google.aip.dev/158) | —                  | —        |
| `page_size` 为负数时必须返回 `INVALID_ARGUMENT` | 强制 | [AIP-158](https://google.aip.dev/158) | `400`（见 §8.2）   | ADR-0013 |
| `page_token` 不得是必填字段                     | 强制 | [AIP-158](https://google.aip.dev/158) | `@Nullable`        | —        |
| 分页令牌必须是不透明 URL 安全字符串             | 强制 | [AIP-158](https://google.aip.dev/158) | Base64 + HMAC      | —        |
| 分页令牌不得可被用户解析                        | 强制 | [AIP-158](https://google.aip.dev/158) | 不暴露结构         | —        |
| 到达集合末尾时 `next_page_token` 必须为空       | 强制 | [AIP-158](https://google.aip.dev/158) | `@Nullable`        | —        |
| API 响应不得是流式响应                          | 强制 | [AIP-158](https://google.aip.dev/158) | JSON 数组          | —        |
| 分页令牌应有合理过期时间（经验值 3 天）         | 推荐 | [AIP-158](https://google.aip.dev/158) | —                  | —        |

### 5.2 过滤 [AIP-160](https://google.aip.dev/160)

| 规则                                         | 级别 | AIP                                   | SODA 落地        | 关联 ADR |
|----------------------------------------------|------|---------------------------------------|------------------|----------|
| List 过滤用 `string filter` 结构化字符串     | 推荐 | [AIP-160](https://google.aip.dev/160) | 查询服务按需     | —        |
| 过滤字段应叫 `filter`                        | 推荐 | [AIP-160](https://google.aip.dev/160) | —                | —        |
| 不合规的过滤字符串应返回 `INVALID_ARGUMENT`  | 推荐 | [AIP-160](https://google.aip.dev/160) | `400`（见 §8.2） | ADR-0013 |
| 字段名不得出现在比较运算符右侧               | 强制 | [AIP-160](https://google.aip.dev/160) | —                | —        |
| 遍历操作符 `.` 必须支持                      | 强制 | [AIP-160](https://google.aip.dev/160) | —                | —        |
| has 操作符 `:` 必须支持                      | 强制 | [AIP-160](https://google.aip.dev/160) | —                | —        |
| 自定义函数必须以双冒号开头（`::myFunction`） | 强制 | [AIP-160](https://google.aip.dev/160) | —                | —        |

### 5.3 字段掩码 [AIP-161](https://google.aip.dev/161)

| 规则                                                                              | 级别 | AIP                                   | SODA 落地                                                                                     | 关联 ADR |
|-----------------------------------------------------------------------------------|------|---------------------------------------|-----------------------------------------------------------------------------------------------|----------|
| 字段掩码用逗号分隔 string（如 `"nickname,mobile"`）                               | 强制 | [AIP-161](https://google.aip.dev/161) | 不用 protobuf FieldMask                                                                       | —        |
| 字段掩码必须相对于资源                                                            | 强制 | [AIP-161](https://google.aip.dev/161) | —                                                                                             | —        |
| 读和写使用相同掩码时数据必须自洽                                                  | 强制 | [AIP-161](https://google.aip.dev/161) | —                                                                                             | —        |
| 通配符 `*` 在重复字段 / map 上表示其子字段（嵌套遍历，如 `authors.*.given_name`） | 推荐 | [AIP-161](https://google.aip.dev/161) | **不适用** — 可更新字段均为标量，无嵌套遍历场景；整体 `*`（全量替换）是 AIP-134 规则，见 §3.5 | ADR-0038 |
| 字段掩码不得通过索引访问重复字段元素                                              | 强制 | [AIP-161](https://google.aip.dev/161) | —                                                                                             | —        |
| 写时遇到不存在字段条目应返回 `INVALID_ARGUMENT`                                   | 强制 | [AIP-161](https://google.aip.dev/161) | `400`（见 §8.2）                                                                              | ADR-0013 |
| 不得使用 `google.protobuf.FieldMask read_mask`（已废弃）                          | 强制 | [AIP-161](https://google.aip.dev/161) | 警告                                                                                          | —        |

详见 §3.5 Update。

### 5.4 变更验证 [AIP-163](https://google.aip.dev/163)

| 规则                                                  | 级别 | AIP                                   | SODA 落地                                                                                 | 关联 ADR |
|-------------------------------------------------------|------|---------------------------------------|-------------------------------------------------------------------------------------------|----------|
| 请求可选含 `bool validate_only` 字段                  | 推荐 | [AIP-163](https://google.aip.dev/163) | 契约默认（`Command.validateOnly()` 默认 false，`Command` 已含该成员）；尚无端点暴露该字段 | —        |
| `validate_only=true` 时仍执行权限检查和业务验证       | 强制 | [AIP-163](https://google.aip.dev/163) | —                                                                                         | —        |
| `validate_only=true` 响应与实际执行相同               | 强制 | [AIP-163](https://google.aip.dev/163) | —                                                                                         | —        |
| 声明式友好资源的变更方法必须包含 `validate_only` 字段 | 强制 | [AIP-163](https://google.aip.dev/163) | SODA 当前无声明式友好资源                                                                 | —        |

### 5.5 软删除 [AIP-164](https://google.aip.dev/164)

| 软删除资源应同时有 `delete_time` 和 `purge_time` 字段 | 推荐 | [AIP-164](https://google.aip.dev/164) | SODA 不设 — 终态以
`state=R` 表达，无时间戳字段（见 §4.5） | ADR-0017 |
| 软删除资源应包含 `DELETED` 状态值 | 推荐 | [AIP-164](https://google.aip.dev/164) | `UserState.R`
即终态值（单字母短名，ADR-0005） | ADR-0005 / ADR-0017 |
| 软删除资源应提供 `:undelete` 自定义方法（POST） | 推荐 | [AIP-164](https://google.aip.dev/164) | **不做** — R
为吸收态不可逆 | ADR-0017 |
| 软删除资源可提供 `:expunge` 自定义方法（永久删除） | 参考 | [AIP-164](https://google.aip.dev/164) | **不做** —
领域无擦除（ADR-0017）；审计到期清除为演进路径未建（ADR-0023） | ADR-0017 / ADR-0023 |
| List 默认不应包含软删除资源 | 推荐 | [AIP-164](https://google.aip.dev/164) | 无 List 端点（走查询服务）；查询侧默认滤
R，由查询服务定 | — |
| Undelete 对未删除资源必须返回 `ALREADY_EXISTS` | 强制 | [AIP-164](https://google.aip.dev/164) | 无
Undelete，不适用 | — |
| **SODA Delete = 不可逆终态注销**（软删除子集：标记不清行、无 Undelete） | 强制 | [AIP-164](https://google.aip.dev/164) |
D→R 吸收态；行保留 + 三键置 NULL + `user_archive` 快照；Delete 返 R 态资源（见 §3.6） | ADR-0017 / ADR-0023 |

### 5.6 鉴权前置 [AIP-211](https://google.aip.dev/211)

| 规则                                         | 级别 | AIP                                   | SODA 落地            | 关联 ADR |
|----------------------------------------------|------|---------------------------------------|----------------------|----------|
| 服务必须在验证请求前先检查授权               | 强制 | [AIP-211](https://google.aip.dev/211) | 接入 `@PreAuthorize` | —        |
| 授权失败时服务必须返回 `PERMISSION_DENIED`   | 强制 | [AIP-211](https://google.aip.dev/211) | `403`（见 §8.2）     | ADR-0013 |
| 错误信息应避免泄露资源存在性（标准模板）     | 推荐 | [AIP-211](https://google.aip.dev/211) | —                    | —        |
| 不可授权时检查父资源读权限，返回 `NOT_FOUND` | 推荐 | [AIP-211](https://google.aip.dev/211) | —                    | —        |

---

## 6. 向后兼容性 [AIP-180](https://google.aip.dev/180)

### 6.1 三种兼容性类型

| 类型           | 说明                       |
|----------------|----------------------------|
| **源代码兼容** | 客户端代码无需修改即可编译 |
| **线路兼容**   | 序列化/反序列化格式不变    |
| **语义兼容**   | 行为不变                   |

### 6.2 向后兼容规则

| 规则                                     | 级别 | AIP                                   | SODA 落地                             | 关联 ADR |
|------------------------------------------|------|---------------------------------------|---------------------------------------|----------|
| 新增字段必须向后兼容                     | 强制 | [AIP-180](https://google.aip.dev/180) | 新增字段用 `OPTIONAL` / `OUTPUT_ONLY` | —        |
| 不得更改已有字段的类型                   | 强制 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |
| 不得更改资源名称格式                     | 强制 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |
| 不得更改默认值                           | 强制 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |
| 不得更改序列化格式                       | 强制 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |
| 新增枚举值可，但不得更改已有值的数字映射 | 推荐 | [AIP-180](https://google.aip.dev/180) | ADR-0005 短名新增安全                 | ADR-0005 |
| 新增标准方法可                           | 推荐 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |
| 行为变更必须通过新方法或新字段实现       | 强制 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |
| 废弃字段应当保留至少 3 年                | 推荐 | [AIP-180](https://google.aip.dev/180) | —                                     | —        |

### 6.3 破坏性变更清单

| 变更                              | 是否破坏性 | AIP                                   |
|-----------------------------------|------------|---------------------------------------|
| 添加 `REQUIRED` 字段到已有请求    | 是         | [AIP-180](https://google.aip.dev/180) |
| 添加 `OUTPUT_ONLY` 字段到已有资源 | 否         | [AIP-180](https://google.aip.dev/180) |
| 添加 `OPTIONAL` 字段              | 否         | [AIP-180](https://google.aip.dev/180) |
| 删除字段                          | 是         | [AIP-180](https://google.aip.dev/180) |
| 更改字段名                        | 是         | [AIP-180](https://google.aip.dev/180) |
| 添加分页到已有方法                | 是         | [AIP-158](https://google.aip.dev/158) |
| 添加枚举值（不变已有映射）        | 否         | [AIP-180](https://google.aip.dev/180) |
| 新增标准方法                      | 否         | [AIP-180](https://google.aip.dev/180) |

---

## 7. 命名规范 [AIP-190](https://google.aip.dev/190)

### 7.1 总体原则

| 原则           | 说明                         | 关联 ADR |
|----------------|------------------------------|----------|
| 直觉性         | 使用直观、熟悉的术语         | —        |
| 一致性         | 同一概念用同一名称           | ADR-0009 |
| 简洁性         | 避免冗余词汇                 | —        |
| 美式英语       | 使用正确的美式英语拼写       | —        |
| UpperCamelCase | Java 类名使用 UpperCamelCase | —        |

### 7.2 方法命名

格式：`VerbNoun`（UpperCamelCase），名词通常是资源类型。

| 标准方法 | 命名               | AIP                                   |
|----------|--------------------|---------------------------------------|
| 获取     | `Get{Resource}`    | [AIP-131](https://google.aip.dev/131) |
| 列出     | `List{Resources}`  | [AIP-132](https://google.aip.dev/132) |
| 创建     | `Create{Resource}` | [AIP-133](https://google.aip.dev/133) |
| 更新     | `Update{Resource}` | [AIP-134](https://google.aip.dev/134) |
| 删除     | `Delete{Resource}` | [AIP-135](https://google.aip.dev/135) |
| 自定义   | `{Verb}{Resource}` | [AIP-136](https://google.aip.dev/136) |

### 7.3 消息命名

| 类型           | 模式                                       | AIP                                                                       | 关联 ADR |
|----------------|--------------------------------------------|---------------------------------------------------------------------------|----------|
| 标准方法请求   | `{Verb}{Resource}Request`                  | [AIP-131](https://google.aip.dev/131) ~ [135](https://google.aip.dev/135) | ADR-0009 |
| 标准方法响应   | `{Resource}` 或 `{Verb}{Resource}Response` | [AIP-131](https://google.aip.dev/131) ~ [135](https://google.aip.dev/135) | ADR-0009 |
| 自定义方法请求 | `{Verb}{Resource}Request`                  | [AIP-136](https://google.aip.dev/136)                                     | ADR-0009 |
| 自定义方法响应 | `{Verb}{Resource}Response`                 | [AIP-136](https://google.aip.dev/136)                                     | ADR-0009 |
| 接口名         | 名词或形容词+名词；不 Service 结尾         | [AIP-190](https://google.aip.dev/190)                                     | —        |

**单源指针**：现有 Research doc §6.2-6.3 完整覆盖；本节不重写。

---

## 8. 错误处理

### 8.1 错误响应结构 RFC 9457 ProblemDetail 最小集（状态码承载语义，AIP-193 不遵守）

SODA 错误响应采用 **Spring RFC 9457 `ProblemDetail` 最小集**，与框架 `ResponseEntityExceptionHandler` 的默认产出逐字段一致：

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid argument",
  "instance": "/api/users"
}
```

| 字段       | 类型   | 说明                                                                                                                          | 级别 | 规范     | 关联 ADR |
|------------|--------|-------------------------------------------------------------------------------------------------------------------------------|------|----------|----------|
| `title`    | string | 人类可读摘要（取 `forStatus` 默认 ReasonPhrase，不显式设置）                                                                  | 强制 | RFC 9457 | ADR-0013 |
| `status`   | int    | HTTP 状态码（取 `forStatus` 默认值，不显式设置）——**语义承载者**                                                              | 强制 | RFC 9457 | ADR-0013 |
| `detail`   | string | 取异常消息；例外为固定文案——500 `"Internal server error"`、持久层 409 `"Concurrent modification"`（面向开发者英文，不本地化） | 强制 | RFC 9457 | ADR-0013 |
| `instance` | URI    | 请求 URI                                                                                                                      | 推荐 | RFC 9457 | ADR-0013 |

> **不设 `type` 自定义词表**：错误语义完全由 `status` 承载（分配与客户端动作见 §8.2），不设扩展字段。
> AIP-193 `ErrorInfo{reason, domain, metadata}` **不遵守**（见 ADR-0013）：不提供机器可读错误标识，
> 客户端按 `status` 分支；将来若需字段级机器标识，走 AIP-193 `details[].reason` 正路，而非把 `type` 加回。

### 8.2 状态码分配（语义 + 客户端动作）

| HTTP | AIP 语义（溯源）             | 含义                     | 触发示例（SODA）                                                                                  | 客户端动作             | 关联 ADR            |
|------|------------------------------|--------------------------|---------------------------------------------------------------------------------------------------|------------------------|---------------------|
| 400  | `INVALID_ARGUMENT`           | 请求无效                 | Bean Validation、`update_mask` 未知字段、格式 / 值不可表示（IAE）、未类型化业务拒绝、状态前置拒绝 | 修改请求，不重试       | ADR-0013 / ADR-0015 |
| 401  | `UNAUTHENTICATED`            | 未认证                   | 待鉴权落地（AIP-211）                                                                             | 重新认证               | ADR-0013            |
| 403  | `PERMISSION_DENIED`          | 无权限（先于存在性检查） | 待鉴权落地（AIP-211）                                                                             | 无权操作               | ADR-0013 / ADR-0017 |
| 404  | `NOT_FOUND`                  | 资源不存在               | `require` 守卫抛 `NotFoundException.entityNotFound`（id 不存在）                                  | 不重试                 | ADR-0013 / ADR-0015 |
| 409  | `ALREADY_EXISTS` / `ABORTED` | 与当前状态冲突、可解决   | 唯一键占用抛 `ConflictException.alreadyExists`；未带 `If-Match` 的持久层乐观锁竞态                | 换值 / 重读后重试      | ADR-0013 / ADR-0037 |
| 412  | —（RFC 9110 §15.5.13）       | 条件头求值为假           | `If-Match` 失配抛 `PreconditionFailedException.versionMismatch`、弱标签（见 §3.5）                | 重读资源，带新版本重试 | ADR-0015 / ADR-0039 |
| 429  | `RESOURCE_EXHAUSTED`         | 配额耗尽                 | 待落地                                                                                            | 退避重试               | —                   |
| 500  | `INTERNAL`                   | 内部错误                 | 未捕获异常、防御守卫（裸 ISE）                                                                    | 报障                   | ADR-0013 / ADR-0015 |
| 503  | `UNAVAILABLE`                | 服务不可用               | 待落地                                                                                            | 退避重试               | —                   |

> 异常类型族（工厂与状态码）与翻译层归属见 ADR-0015 / ADR-0013；本表只承载状态码分配，不重述类型细节。

### 8.3 错误消息规则

| 规则                                    | 级别 | AIP                                   | SODA 落地                                        |
|-----------------------------------------|------|---------------------------------------|--------------------------------------------------|
| 错误消息帮助开发者理解和解决问题        | 推荐 | [AIP-193](https://google.aip.dev/193) | `detail` 面向开发者                              |
| 错误消息不假设用户了解底层实现          | 强制 | [AIP-193](https://google.aip.dev/193) | —                                                |
| 错误消息简洁但可操作                    | 推荐 | [AIP-193](https://google.aip.dev/193) | —                                                |
| 额外信息放在 `properties.metadata` 字段 | 推荐 | [AIP-193](https://google.aip.dev/193) | **不遵守**——最小集不设扩展（见 §8.1 / ADR-0013） |
| 错误消息不进行本地化（如翻译）          | 强制 | [AIP-193](https://google.aip.dev/193) | 英文固定文案                                     |
| 面向用户的本地化消息通过前端翻译层提供  | 强制 | [AIP-193](https://google.aip.dev/193) | 前端翻译层承担                                   |

### 8.4 权限检查顺序

```
权限检查 → 存在性检查 → 业务逻辑
```

| 规则                                             | 级别 | AIP                                                                           | 关联 ADR            |
|--------------------------------------------------|------|-------------------------------------------------------------------------------|---------------------|
| 权限检查必须先于存在性检查                       | 强制 | [AIP-193](https://google.aip.dev/193) + [AIP-211](https://google.aip.dev/211) | ADR-0013 / ADR-0017 |
| 无权限时必须返回 `PERMISSION_DENIED`（HTTP 403） | 强制 | [AIP-193](https://google.aip.dev/193) + [AIP-211](https://google.aip.dev/211) | ADR-0013            |
| 无权限时不得暴露资源是否存在                     | 强制 | [AIP-193](https://google.aip.dev/193) + [AIP-211](https://google.aip.dev/211) | ADR-0013            |

❌ 错误消息本地化:

```json
{
  "detail": "用户不存在"
  // ❌ 中文本地化
}
```

✅ 错误消息英文 + 前端翻译:

```json
{
  "status": 404,
  // ✅ 语义由状态码承载（见 §8.2）
  "detail": "User not found",
  // ✅ 英文，不本地化
  "instance": "/api/users/1"
}
```

---

## 9. AIP 偏离表

### 9.1 已遵守的 AIP 关键规则

| 规则                                              | SODA 实现                      | 关联 ADR |
|---------------------------------------------------|--------------------------------|----------|
| AIP-190 命名：UpperCamelCase                      | 类名                           | ADR-0009 |
| AIP-134 省略 `update_mask` = 已填充字段掩码       | 省略 / 空白 = 仅写非 null 字段 | ADR-0038 |
| AIP-134 / AIP-161 `update_mask` 支持 `*` 全量替换 | `*` = 所有字段无条件写入       | ADR-0038 |
| AIP-161 白名单外字段名 → `INVALID_ARGUMENT`       | 400（见 §8.2）                 | ADR-0038 |

### 9.2 偏离 AIP 的关键规则（ADR 锚定）

| 偏离规则                               | SODA 做法                                                                                                      | AIP 做法                               | 理由                                                                   | ADR                 |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------|----------------------------------------|------------------------------------------------------------------------|---------------------|
| AIP-121 资源导向「必须 Get/List」      | 读写分离；Get/List 走查询服务                                                                                  | Controller 强制实现                    | 读侧架构（yudao 风格）                                                 | —                   |
| AIP-122 name 字符串格式                | Long ID 路径变量                                                                                               | `users/{string_id}`                    | DDD 强类型                                                             | ADR-0017            |
| AIP-126 UPPER_SNAKE_CASE               | 单字母短名 + i18n desc                                                                                         | UPPER_SNAKE_CASE                       | i18n 备扩展                                                            | ADR-0005            |
| AIP-133 ID 必填（admin plane）         | data plane 可选；admin plane 必填                                                                              | 必须传 ID                              | 用户自助场景                                                           | ADR-0017            |
| AIP-142 RFC-3339 时间戳                | DecimalEpochMilli（Long）                                                                                      | RFC-3339 string                        | 现有 DTO 习惯                                                          | ADR-0031            |
| AIP-148 create_time / update_time 暴露 | Entity 暂不暴露前端                                                                                            | 必须暴露                               | 写操作不返回                                                           | —                   |
| AIP-164 软删除                         | Delete = 不可逆终态注销（无 Undelete/Expunge；Delete 返 R 态资源；`allow_missing` 不做）                       | 软删除（undelete/expunge/delete_time） | 业务终态语义                                                           | ADR-0017 / ADR-0023 |
| AIP-193 错误结构                       | 只遵 RFC 9457 最小集（`title/status/detail/instance`），不设 `type` 词表与 `ErrorInfo{reason,domain,metadata}` | ErrorInfo{reason,domain,metadata}      | 语义由状态码承载、客户端按 status 分支；最小集与框架默认产出逐字段一致 | ADR-0013            |
| AIP-211 鉴权前置                       | 写侧端点暂未接 `@PreAuthorize`；授权失败应返回 `PERMISSION_DENIED` 的规则一并待接入                            | 服务须先检查授权                       | 鉴权 capability 未启动                                                 | —                   |
| AIP-154 `Last-Modified` 弱验证器       | 只发 `ETag`（`HttpValidatorSource` 仅暴露 `version()`）                                                        | ETag + Last-Modified 双发              | 审计列属基础设施表示、不入领域与出站模型                               | ADR-0031            |
| AIP-154 失配 → `ABORTED`               | `If-Match` 失配译 **412**（RFC 9110 §15.5.13「请求头条件求值为假」的专用状态）                                 | ABORTED（Google HTTP 映射为 409）      | 412 语义更精确；未带条件头的竞态仍译 409，客户端动作一致（重读后重试） | ADR-0039            |

### 9.3 端点清单（UserController）

| #  | HTTP    | URI                                   | Request                      | Response                   | 语义                                         | AIP                                   |
|----|---------|---------------------------------------|------------------------------|----------------------------|----------------------------------------------|---------------------------------------|
| 1  | `POST`  | `/api/users`                          | `CreateUserRequest`          | `UserResponse`             | Create                                       | [AIP-133](https://google.aip.dev/133) |
| 2  | `PATCH` | `/api/users/{id}`                     | `UpdateUserRequest`          | `UserResponse`             | Update                                       | [AIP-134](https://google.aip.dev/134) |
| 3  | `POST`  | `/api/users/{id}:deregister`          | —                            | `UserResponse`（R 态快照） | 领域行为（不可逆终态迁移，见 §1.2 语义二分） | [AIP-136](https://google.aip.dev/136) |
| 4  | `POST`  | `/api/users/{id}:disable`             | —                            | `Void`                     | 状态转换                                     | [AIP-136](https://google.aip.dev/136) |
| 5  | `POST`  | `/api/users/{id}:enable`              | —                            | `Void`                     | 状态转换                                     | [AIP-136](https://google.aip.dev/136) |
| 6  | `POST`  | `/api/users/{id}:changeUsername`      | `ChangeUsernameRequest`      | `Void`                     | 自定义方法                                   | [AIP-136](https://google.aip.dev/136) |
| 7  | `POST`  | `/api/users/{id}:changePassword`      | `ChangePasswordRequest`      | `Void`                     | 自定义方法                                   | [AIP-136](https://google.aip.dev/136) |
| 8  | `POST`  | `/api/users/{id}:requestChangeMobile` | `RequestChangeMobileRequest` | `Void`                     | UCC 发码（按 ADR-0012 重命名后）             | [AIP-136](https://google.aip.dev/136) |
| 9  | `POST`  | `/api/users/{id}:changeMobile`        | `ChangeMobileRequest`        | `Void`                     | UCC 换绑                                     | [AIP-136](https://google.aip.dev/136) |
| 10 | `POST`  | `/api/users/{id}:requestChangeEmail`  | `RequestChangeEmailRequest`  | `Void`                     | UCC 发码（按 ADR-0012 重命名后）             | [AIP-136](https://google.aip.dev/136) |
| 11 | `POST`  | `/api/users/{id}:changeEmail`         | `ChangeEmailRequest`         | `Void`                     | UCC 换绑                                     | [AIP-136](https://google.aip.dev/136) |

**未实现**：`GET /api/users/{id}`（Get）、`GET /api/users`（List）——走查询服务（见 §9.2 AIP-121 偏离）；标准 `DELETE` —— 无场景，领域行为
`:deregister` 覆盖注销语义。



---

## 10. 与现有 ADR / 文档的引用关系

### 10.1 ADR 引用表

| ADR                                                                  | 内容                                                               | 本规范引用                       |
|----------------------------------------------------------------------|--------------------------------------------------------------------|----------------------------------|
| [ADR-0005](../adr/0005-enum-short-name.md)                           | 枚举短名 + i18n desc                                               | §2.5 / §4.9 / §6.2               |
| [ADR-0009](../adr/0009-adapter-web-req-resp-pattern.md)              | Adapter 层 Request/Response + WebAssembler 模式                    | §3.7 / §4.1 / §7.3               |
| [ADR-0012](../adr/0012-url-naming-convention.md)                     | 禁 PUT / URL 命名 / `:verb`                                        | §2.2 / §3.5 / §3.7 / §4.1        |
| [ADR-0013](../adr/0013-error-response-structure.md)                  | 错误响应结构（ProblemDetail 最小集 / 状态码承载语义 / 唯一翻译层） | §8（全文）                       |
| [ADR-0015](../adr/0015-exception-class-convention.md)                | 异常类型族与 IAE 校验通道（404 / 409 / 412 工厂）                  | §8.2                             |
| [ADR-0017](../adr/0017-aggregate-lifecycle-removal-semantics.md)     | 聚合生命周期（Long ID / 终态注销）                                 | §2.2 / §3.4 / §3.6 / §5.5 / §8.4 |
| [ADR-0021](../adr/0021-verification-aggregate-root-and-topology.md)  | Verification 聚合根 + 拓扑                                         | §3.7（UCC 发码场景）             |
| [ADR-0023](../adr/0023-terminal-key-release-and-archive.md)          | 注销终态键释放与归档                                               | §3.6 / §5.5                      |
| [ADR-0026](../adr/0026-verification-source-recipient.md)             | Verification 双概念模型（发码两域）                                | §3.9（导入导出场景）             |
| [ADR-0031](../adr/0031-wire-semantic-literals-decimal-epochmilli.md) | 线协议语义字面值 DecimalEpochMilli                                 | §4.2（时间戳）                   |
| [ADR-0036](../adr/0036-p3c-surface-format-retrofit.md)               | P3C 表面格式借鉴（保留 Research 不升格）                           | 整体格式（三级 + ❌/✅）         |
| [ADR-0038](../adr/0038-update-mask-domain-primitive.md)              | update_mask 语义与归一化（`UpdateMask` DP）                        | §3.5 / §5.3 / §9.1               |
| [ADR-0039](../adr/0039-conditional-request-transport-contract.md)    | 条件请求传输契约（ETag 派生 / If-Match 归一化）                    | §3.5 / §4.5                      |

### 10.2 文档引用表

| 文档                                                                         | 内容                                             | 本规范引用                |
|------------------------------------------------------------------------------|--------------------------------------------------|---------------------------|
| [google-aip-api-design-spec.md](../research/google-aip-api-design-spec.md)   | Google AIP 51 个主文档规则 + SODA 已有引用链     | Research 溯源（**不动**） |
| [google-aip-full-pull.md](../research/google-aip-full-pull.md)               | AIP 全谱 51/66 个主文档规则表                    | §1-6 规则索引             |
| [soda-current-api-state.md](../research/soda-current-api-state.md)           | SODA Controller / WebAssembler / Result 现状基线 | §9.3 现有端点清单         |
| [p3c-surface-format-retrofit.md](../research/p3c-surface-format-retrofit.md) | P3C 表面格式借鉴调研                             | 整体格式依据              |

### 10.3 关联规范

| 文档                                                       | 关联点                                       |
|------------------------------------------------------------|----------------------------------------------|
| [STYLEGUIDE.md](../../STYLEGUIDE.md) §3.4                  | MapStruct 注解用法（adapter 转换器）         |
| [dp-conventions.md](../dp-conventions.md)                  | Domain Primitive 设计规范（与字段格式联动）  |
| [framework-type-contracts.md](framework-type-contracts.md) | 框架类型契约（Entity / Aggregate / Gateway） |
| [adapter.md](adapter.md)                                   | Adapter 转换器约定（**不动**，独立 effort）  |
| [test-conventions.md](../test-conventions.md)              | 测试规范（HTTP API 测试）                    |
| [doc-conventions.md](../doc-conventions.md)                | 文档体系标准（本规范遵循的格式）             |

### 10.4 新模块设计检查清单

创建新的业务模块 / 端点时，对照本规范：

- [ ] **资源**（§2）：是否以资源为中心建模？集合路径 camelCase 复数？单例资源按 §2.6 处理？
- [ ] **标准方法**（§3.2-3.6）：是否优先 Get / List / Create / Update / Delete？Get/List 走查询服务？
- [ ] **自定义方法**（§3.7）：是否仅在标准方法无法表达时使用？命名不含介词？POST 副作用 / GET 无副作用？
- [ ] **HTTP 方法**（§3）：POST / GET / PATCH / DELETE 是否正确映射？是否禁 PUT？是否用 `update_mask`（逗号分隔 string）？
- [ ] **URL 命名**（§2.2 / §3.7）：集合名和方法动词是否使用 camelCase？是否省略冗余动词？
- [ ] **字段命名**（§4.1）：是否使用美式英语 / snake_case JSON / 无介词？
- [ ] **字段行为**（§4.8）：是否所有字段标注 `@NotNull`（REQUIRED）/ `@Nullable`（OPTIONAL）/ Javadoc OUTPUT_ONLY？
- [ ] **字段格式**（§4.7）：是否使用 Bean Validation 注解（`@Email` / `@Pattern`）？
- [ ] **标准字段**（§4.5）：是否使用标准字段名（`name` / `create_time` / `update_time` / `display_name`）？
- [ ] **状态字段**（§4.9）：状态机字段是否叫 `state`？是否走 `:enable` / `:disable` 自定义方法？
- [ ] **枚举值**（§2.5）：是否沿用 ADR-0005 短名 + i18n desc？
- [ ] **分页**（§5.1）：List 方法是否从一开始就包含 `page_size` / `page_token` / `next_page_token`？
- [ ] **过滤**（§5.2）：是否提供 `filter` 字符串？结构化语法？
- [ ] **字段掩码**（§5.3）：Update 是否用逗号分隔 string `update_mask`？
- [ ] **错误**（§8）：是否使用 RFC 9457 `ProblemDetail` 最小集（`title`/`status`/`detail`/`instance`）？
- [ ] **权限检查**（§5.6 / §8.4）：是否先于存在性检查？是否返回 `PERMISSION_DENIED` 403？
- [ ] **软删除 vs 终态**（§5.5）：Delete 是软删除还是终态？SODA 默认终态。
- [ ] **向后兼容**（§6）：是否避免破坏性变更？新增字段用 `OPTIONAL` / `OUTPUT_ONLY`？
- [ ] **命名**（§7）：Java 类 UpperCamelCase？Request/Response 后缀？
