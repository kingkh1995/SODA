# SODA API 设计规范（基于 Google AIP）

> **唯一信息来源**：[google.aip.dev](https://google.aip.dev) — Google API Improvement Proposals。
> 本文档将 Google AIP 规范适配为 Java / Spring Boot / DDD REST API 设计规范。
>
> **引用格式说明**：
> - 表格中 `AIP` 列标注源 AIP 编号
> - 正文中使用 `[来源: AIP-XXX](https://google.aip.dev/XXX)` 标注引用
> - 所有规则和声明均可溯源至 Google AIP 原文

---

## 目录

1. [概述](#1-概述)
2. [资源设计](#2-资源设计)
3. [HTTP 方法与操作](#3-http-方法与操作)
4. [字段设计](#4-字段设计)
5. [设计模式](#5-设计模式)
6. [命名规范](#6-命名规范)
7. [错误处理](#7-错误处理)
8. [向后兼容性](#8-向后兼容性)
9. [SODA 项目适配说明](#9-soda-项目适配说明)

---

## 1. 概述

### 1.1 设计哲学

Google AIP 的核心原则 [来源: AIP-1](https://google.aip.dev/1)：

| 原则 | 说明 | AIP |
|---|---|---|
| **资源导向** | API 以命名资源（名词）为中心，操作围绕资源展开 | [AIP-121](https://google.aip.dev/121) |
| **标准方法优先** | 优先使用 Get/List/Create/Update/Delete 标准方法 | [AIP-121](https://google.aip.dev/121), [AIP-130](https://google.aip.dev/130) |
| **一致性** | 同一概念用同一名称，不同概念用不同名称 | [AIP-190](https://google.aip.dev/190) |
| **无状态协议** | 每次请求独立，服务器负责持久化，客户端负责应用状态 | [AIP-121](https://google.aip.dev/121) |
| **无循环引用** | 资源关系必须是有向无环图（DAG） | [AIP-121](https://google.aip.dev/121) |

### 1.2 方法选择优先级 [来源: AIP-130](https://google.aip.dev/130)

设计方法时，按以下顺序选择 [来源: AIP-130](https://google.aip.dev/130)：

1. **标准方法**（Get / List / Create / Update / Delete）
2. **标准批量方法**（BatchGet / BatchCreate / BatchUpdate / BatchDelete）
3. **自定义方法**（挂载到资源或集合上）
4. **流式方法**（最后手段）

### 1.3 适用范围

本规范适用于 SODA 项目的 **Adapter 层**（HTTP 边界）和 **API 层**（共享 DTO/Command）。
领域层和基础设施层遵循各自的 DDD 约定，不受本规范约束。

---

## 2. 资源设计

### 2.1 资源导向设计 [来源: AIP-121](https://google.aip.dev/121)

API **应当**建模为资源层次结构，每个节点是简单资源或资源集合 [来源: AIP-121](https://google.aip.dev/121)。

**核心规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 资源是 API 的基本构建块，每个资源有各自命名 | MUST | [AIP-121](https://google.aip.dev/121) |
| 资源名称在 API 内 **必须** 唯一 | MUST | [AIP-122](https://google.aip.dev/122) |
| 资源 **必须** 支持 Get 方法 | MUST | [AIP-121](https://google.aip.dev/121) |
| 资源 **必须** 支持 List 方法（单例资源除外） | MUST | [AIP-121](https://google.aip.dev/121) |
| 资源的 schema 在所有标准方法中 **必须** 一致 | MUST | [AIP-121](https://google.aip.dev/121) |
| 管理平面操作完成后，资源状态 **必须** 达到稳态 | MUST | [AIP-121](https://google.aip.dev/121) |
| API **应当** 暴露大量资源，每个资源少量方法 | SHOULD | [AIP-121](https://google.aip.dev/121) |
| API **不应当** 与底层数据库 schema 一一对应 | SHOULD NOT | [AIP-121](https://google.aip.dev/121) |

资源名称格式：`collection/{id}`，使用 `/` 分隔 [来源: AIP-122](https://google.aip.dev/122)。

**命名规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 资源名称 **必须** 在 API 内唯一 | MUST | [AIP-122](https://google.aip.dev/122) |
| 集合标识符 **必须** 是名词的 `camelCase` 复数形式 | MUST | [AIP-122](https://google.aip.dev/122) |
| 集合标识符 **必须** 以小写字母开头，仅含 `[a-z][a-zA-Z0-9]*` | MUST | [AIP-122](https://google.aip.dev/122) |
| 资源 ID 段 **应当** 符合 RFC-1034（字母、数字、连字符） | SHOULD | [AIP-122](https://google.aip.dev/122) |
| 资源 ID **应当** 限制为小写字母 | SHOULD | [AIP-122](https://google.aip.dev/122) |
| 资源 **必须** 暴露 `name` 字段包含资源名称 | MUST | [AIP-122](https://google.aip.dev/122) |
| 资源名称 **必须** 仅使用 RFC-1123 DNS 名称中的字符 | MUST | [AIP-122](https://google.aip.dev/122) |

**示例**：

```
users/vhugo1802
publishers/123/books/les-miserables
```

**嵌套集合的冗余省略**（AIP-122）：

如果父集合名称已作为前缀，子集合名称可以省略前缀：

```
users/vhugo1802/userEvents/birthday-dinner-226
→ users/vhugo1802/events/birthday-dinner-226  ✅ 推荐
```

### 2.3 资源类型 [来源: AIP-123](https://google.aip.dev/123)

资源类型格式：`{ServiceName}/{Type}`，使用 PascalCase 单数形式 [来源: AIP-123](https://google.aip.dev/123)。

**规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 资源类型名称 **必须** 以大写字母开头，仅含字母数字，使用 PascalCase | MUST | [AIP-123](https://google.aip.dev/123) |
| 资源类型名称 **必须** 是名词的单数形式 | MUST | [AIP-123](https://google.aip.dev/123) |
| API **应当** 使用 `google.api.resource` 注解标注资源类型 | SHOULD | [AIP-123](https://google.aip.dev/123) |
| 注解 **必须** 包含 `pattern`、`singular`、`plural` | MUST | [AIP-123](https://google.aip.dev/123) |
### 2.4 资源关联 [来源: AIP-124](https://google.aip.dev/124)

| 规则 | 级别 | AIP |
|---|---|---|
| 每个资源最多有一个规范父资源 | MUST | [AIP-124](https://google.aip.dev/124) |
| 多对一关系：**必须** 选择一个父资源作为规范父资源 | MUST | [AIP-124](https://google.aip.dev/124) |
| 多对一关系：其他关联通过字段引用 | MAY | [AIP-124](https://google.aip.dev/124) |
| 多对多关系：使用重复字段或子资源 | SHOULD | [AIP-124](https://google.aip.dev/124) |
| List 请求 **不得** 要求两个不同的父资源才能工作 | MUST NOT | [AIP-124](https://google.aip.dev/124) |
### 2.5 枚举 [来源: AIP-126](https://google.aip.dev/126)

| 规则 | 级别 | AIP |
|---|---|---|
| 枚举值 **必须** 使用 `UPPER_SNAKE_CASE` | MUST | [AIP-126](https://google.aip.dev/126) |
| 第一个值 **应当** 是 `{ENUM}_UNSPECIFIED` | SHOULD | [AIP-126](https://google.aip.dev/126) |
| 枚举 **应当** 仅用于变化不频繁的值集合 | SHOULD | [AIP-126](https://google.aip.dev/126) |
| 变化频繁的值集合 **应当** 使用 `string` | SHOULD | [AIP-126](https://google.aip.dev/126) |
| 有广泛采用的标准表示时，**不应当** 使用枚举 | SHOULD NOT | [AIP-126](https://google.aip.dev/126) |
| 枚举 **应当** 文档化是否冻结或未来会添加值 | SHOULD | [AIP-126](https://google.aip.dev/126) |
| 仅在单个消息中使用的枚举 **应当** 嵌套在该消息内 | SHOULD | [AIP-126](https://google.aip.dev/126) |
| 非零值 **不应** 加枚举名前缀 | SHOULD NOT | [AIP-126](https://google.aip.dev/126) |
### 2.6 单例资源 [来源: AIP-156](https://google.aip.dev/156)

单例资源：每个父资源恰好一个实例 [来源: AIP-156](https://google.aip.dev/156)。

| 规则 | 级别 | AIP |
|---|---|---|
| 单例资源 **不得** 有 Create / Delete 方法 | MUST NOT | [AIP-156](https://google.aip.dev/156) |
| 单例资源 **应当** 提供 Get / Update 方法 | SHOULD | [AIP-156](https://google.aip.dev/156) |
| 单例资源 **不得** 有用户指定的 ID 或系统 ID | MUST NOT | [AIP-156](https://google.aip.dev/156) |
| 单例资源 **可以** 定义 List 方法（需按 AIP-159 实现） | MAY | [AIP-156](https://google.aip.dev/156) |
| 若所有字段均为 output-only，则 **不应** 定义 Update | SHOULD NOT | [AIP-156](https://google.aip.dev/156) |
---

## 3. HTTP 方法与操作

### 3.1 标准方法映射 [来源: AIP-130](https://google.aip.dev/130)

| 操作 | HTTP 方法 | URL 模式 | AIP |
|---|---|---|---|
| 获取单个资源 | `GET` | `/{resource}/{id}` | [AIP-131](https://google.aip.dev/131) |
| 列出集合 | `GET` | `/{parent}/{resource}` | [AIP-132](https://google.aip.dev/132) |
| 创建资源 | `POST` | `/{parent}/{resource}` | [AIP-133](https://google.aip.dev/133) |
| 部分更新 | `PATCH` | `/{resource}/{id}` | [AIP-134](https://google.aip.dev/134) |
| 删除资源 | `DELETE` | `/{resource}/{id}` | [AIP-135](https://google.aip.dev/135) |
| 自定义方法 | `POST` | `/{resource}/{id}:{action}` | [AIP-136](https://google.aip.dev/136) |

### 3.2 Get 方法 [来源: AIP-131](https://google.aip.dev/131)

| 规则 | 级别 | AIP |
|---|---|---|
| HTTP 方法 **必须** 是 `GET` | MUST | [AIP-131](https://google.aip.dev/131) |
| URI **应当** 包含对应资源名称的单个变量 | SHOULD | [AIP-131](https://google.aip.dev/131) |
| **不应当** 有 `body` | MUST NOT | [AIP-131](https://google.aip.dev/131) |
| 响应 **必须** 是资源本身（无独立 Response 类型） | MUST | [AIP-131](https://google.aip.dev/131) |

**Request 模式**：

```java
record GetUserRequest(
    @JsonProperty("id") Long id    // 资源标识符，映射到 URI 路径
) {}
```

**SODA 适配**：使用 `{id}` 路径变量，而非 AIP 的 `name` 字符串路径（DDD 中 ID 是强类型）。

### 3.3 List 方法 [来源: AIP-132](https://google.aip.dev/132)

| 规则 | 级别 | AIP |
|---|---|---|
| HTTP 方法 **必须** 是 `GET` | MUST | [AIP-132](https://google.aip.dev/132) |
| **必须** 包含 `page_size` 和 `page_token` 字段 | MUST | [AIP-132](https://google.aip.dev/132) |
| 集合标识符 **必须** 是字面字符串 | MUST | [AIP-132](https://google.aip.dev/132) |
| 响应 **必须** 包含资源列表和 `next_page_token` | MUST | [AIP-132](https://google.aip.dev/132) |
| **应当** 支持 `filter` 和 `order_by` 可选字段 | SHOULD | [AIP-132](https://google.aip.dev/132) |

**Request 模式**：

```java
record ListUsersRequest(
    @JsonProperty("page_size") @Nullable Integer pageSize,
    @JsonProperty("page_token") @Nullable String pageToken,
    @JsonProperty("filter") @Nullable String filter,
    @JsonProperty("order_by") @Nullable String orderBy
) {}
```

**Response 模式**：

```java
record ListUsersResponse(
    @JsonProperty("users") List<UserResponse> users,
    @JsonProperty("next_page_token") @Nullable String nextPageToken,
    @JsonProperty("total_size") @Nullable Integer totalSize
) {}
```

### 3.4 Create 方法 [来源: AIP-133](https://google.aip.dev/133)

| 规则 | 级别 | AIP |
|---|---|---|
| HTTP 方法 **必须** 是 `POST` | MUST | [AIP-133](https://google.aip.dev/133) |
| URI **应当** 映射到父集合路径 | SHOULD | [AIP-133](https://google.aip.dev/133) |
| 响应 **必须** 是资源本身 | MUST | [AIP-133](https://google.aip.dev/133) |
| **必须** 允许用户指定资源 ID（管理平面） | MUST | [AIP-133](https://google.aip.dev/133) |
| ID 字段 **必须** 在 Request 上，不在资源本身 | MUST | [AIP-133](https://google.aip.dev/133) |
| 重复创建 **必须** 返回 `ALREADY_EXISTS` | MUST | [AIP-133](https://google.aip.dev/133) |

**SODA 适配**：DDD 中服务端生成 ID，不暴露 `{resource}_id` 字段给前端。

### 3.5 Update 方法 [来源: AIP-134](https://google.aip.dev/134)

| 规则 | 级别 | AIP |
|---|---|---|
| **应当** 支持部分更新，HTTP 方法 **应当** 是 `PATCH` | SHOULD | [AIP-134](https://google.aip.dev/134) |
| 如果只支持全量替换，**可以** 用 `PUT`，但 **强烈不推荐** | MAY | [AIP-134](https://google.aip.dev/134) |
| **必须** 包含 `update_mask` 字段（FieldMask 类型） | MUST | [AIP-134](https://google.aip.dev/134) |
| 省略 `update_mask` 时，**必须** 视为所有填充字段 | MUST | [AIP-134](https://google.aip.dev/134) |
| `update_mask` **必须** 支持 `*` 通配符（全量替换） | MUST | [AIP-134](https://google.aip.dev/134) |
| 响应 **必须** 是资源本身 | MUST | [AIP-134](https://google.aip.dev/134) |
| 更新方法 **不应当** 触发副作用 | SHOULD NOT | [AIP-134](https://google.aip.dev/134) |
| 状态字段 **不得** 在更新方法中直接写入 | MUST NOT | [AIP-134](https://google.aip.dev/134) |

**为什么不用 PUT** [来源: AIP-134](https://google.aip.dev/134)：

> PUT 是全量替换。当 API 新增字段时，之前的 PUT 请求会静默丢失新字段的数据。PATCH 只更新指定字段，是向后兼容的。

**SODA 适配**：项目已决定禁止 PUT（ADR-0009），与 AIP-134 一致。

**Etag 乐观锁** [来源: AIP-134](https://google.aip.dev/134), [AIP-154](https://google.aip.dev/154)：

资源可以包含 `string etag` 字段。如果提供了 etag，请求 **必须** 仅在与服务器计算的 etag 匹配时成功，否则 **必须** 返回 `ABORTED` 错误。

### 3.6 Delete 方法 [来源: AIP-135](https://google.aip.dev/135)

| 规则 | 级别 | AIP |
|---|---|---|
| HTTP 方法 **必须** 是 `DELETE` | MUST | [AIP-135](https://google.aip.dev/135) |
| **不应当** 有 `body` | MUST NOT | [AIP-135](https://google.aip.dev/135) |
| 存在子资源时 **必须** 返回 `FAILED_PRECONDITION` | MUST | [AIP-135](https://google.aip.dev/135) |
| 级联删除 **应当** 提供 `bool force` 字段 | SHOULD | [AIP-135](https://google.aip.dev/135) |
| 保护删除 **可以** 接受 etag | MAY | [AIP-135](https://google.aip.dev/135) |
| 资源不存在 **应当** 返回 `NOT_FOUND` | SHOULD | [AIP-135](https://google.aip.dev/135) |

### 3.7 自定义方法 [来源: AIP-136](https://google.aip.dev/136)

| 规则 | 级别 | AIP |
|---|---|---|
| **应当** 仅用于标准方法无法表达的功能 | SHOULD | [AIP-136](https://google.aip.dev/136) |
| 方法名 **应当** 是 `动词 + 名词`（VerbNoun） | SHOULD | [AIP-136](https://google.aip.dev/136) |
| 方法名 **不得** 包含介词（for, with 等） | MUST NOT | [AIP-136](https://google.aip.dev/136) |
| 方法名 **不应当** 包含标准方法动词（Get, List, Create, Update, Delete） | SHOULD NOT | [AIP-136](https://google.aip.dev/136) |
| HTTP 方法 **必须** 是 `GET` 或 `POST` | MUST | [AIP-136](https://google.aip.dev/136) |
| 有副作用或修改数据时 **必须** 用 `POST` | MUST | [AIP-136](https://google.aip.dev/136) |
| URI **必须** 使用 `:` 后跟自定义动词（`:archive`） | MUST | [AIP-136](https://google.aip.dev/136) |
| 自定义动词 **必须** 使用 `camelCase` | MUST | [AIP-136](https://google.aip.dev/136) |
| `body` **应当** 是 `"*"` | SHOULD | [AIP-136](https://google.aip.dev/136) |

**SODA 适配**：项目使用 `POST /{resource}/{id}:{action}` 模式（如 `POST /users/1:disable`），与 AIP-136 一致。集合级自定义方法（无 parent 顶层集合）同样允许：`POST /collection:verb`（如 Firebase Identity Toolkit `POST /v1/accounts:signUp`、`accounts:sendOobCode`）——预认证/无资源场景（ULG/UPR/URG 发码）适用，见 ADR-0026。

**SODA 自定义方法命名规范**（[来源: AIP-136](https://google.aip.dev/136)）：

| 规则 | 级别 | AIP |
|---|---|---|
| URI 中的自定义动词 **必须** 使用 `camelCase` | MUST | [AIP-136](https://google.aip.dev/136) |
| RPC 名称与 URI 动词 **必须** 匹配 | MUST | [AIP-136](https://google.aip.dev/136) |
| 方法名 **应当** 是 `动词 + 名词`（VerbNoun） | SHOULD | [AIP-136](https://google.aip.dev/136) |
| 方法名 **不得** 包含介词（for, with 等） | MUST NOT | [AIP-136](https://google.aip.dev/136) |

**user 模块自定义方法示例**：

| 操作 | URI | RPC 名 | 语义 |
|---|---|---|---|
| 修改密码 | `POST /users/{id}:changePassword` | `ChangePassword` | 领域行为，触发密码校验 |
| 修改用户名 | `POST /users/{id}:changeUsername` | `ChangeUsername` | 领域行为，触发唯一性校验 |
| 修改手机号 | `POST /users/{id}:changeMobile` | `ChangeMobile` | 领域行为，需要验证码验证 |
| 修改邮箱 | `POST /users/{id}:changeEmail` | `ChangeEmail` | 领域行为，需要验证码验证 |
| 验证手机号 | `POST /users/{id}:verifyMobile` | `VerifyMobile` | 发送换绑手机号的验证码 |
| 验证邮箱 | `POST /users/{id}:verifyEmail` | `VerifyEmail` | 发送换绑邮箱的验证码 |
| 禁用用户 | `POST /users/{id}:disable` | `Disable` | 单词动词，无需驼峰 |
| 启用用户 | `POST /users/{id}:enable` | `Enable` | 单词动词，无需驼峰 |

**命名原则**：
1. **语义自明** — URL 本身应表达操作意图，不依赖文档
2. **动词 + 名词** — `changePassword`、`verifyMobile` 而非 `password`、`mobile`
3. **camelCase** — AIP-136 硬性要求，不使用 kebab-case 或 snake_case
4. **单动词优先** — `disable`/`enable` 是单动词，无需后缀

---

## 4. 字段设计

### 4.1 字段命名 [来源: AIP-140](https://google.aip.dev/140)

| 规则 | 级别 | AIP |
|---|---|---|
| 字段名 **应当** 使用正确的美式英语 | SHOULD | [AIP-140](https://google.aip.dev/140) |
| 重复字段 **必须** 使用正确的复数形式 | MUST | [AIP-140](https://google.aip.dev/140) |
| 非重复字段 **应当** 使用单数形式 | SHOULD | [AIP-140](https://google.aip.dev/140) |
| 字段名 **不应当** 包含介词（with, for, at, by） | SHOULD NOT | [AIP-140](https://google.aip.dev/140) |
| 字段名 **不得** 是动词，必须是名词 | MUST NOT | [AIP-140](https://google.aip.dev/140) |
| 布尔字段 **应当** 省略 `is_` 前缀 | SHOULD | [AIP-140](https://google.aip.dev/140) |
| 保留字（new, class, import 等）**应当** 避免 | SHOULD | [AIP-140](https://google.aip.dev/140) |
| URI 字段 **应当** 使用 `uri`，URL 字段 **应当** 使用 `url` | SHOULD | [AIP-140](https://google.aip.dev/140) |

**常用缩写** [来源: AIP-140](https://google.aip.dev/140)：

| 推荐 | 避免 |
|---|---|
| `config` | `configuration` |
| `id` | `identifier` |
| `info` | `information` |
| `spec` | `specification` |
| `stats` | `statistics` |
| `distance_km` | `distance_kilometers` |

**形容词位置** [来源: AIP-140](https://google.aip.dev/140)：形容词放在名词前。

| 推荐 | 避免 |
|---|---|
| `collected_items` | `items_collected` |
| `imported_objects` | `objects_imported` |

**显示名称** [来源: AIP-140](https://google.aip.dev/140), [AIP-148](https://google.aip.dev/148)：人类可读名称字段 **应当** 叫 `display_name`，正式名称字段 **可以** 叫 `title`。

### 4.2 字段行为 [来源: AIP-203](https://google.aip.dev/203)

每个请求/资源字段 **必须** 标注 `google.api.field_behavior` [来源: AIP-203](https://google.aip.dev/203)。

| 行为 | 说明 | 使用场景 |
|---|---|---|
| `REQUIRED` | 字段必须存在且非空 | 必需的请求参数 |
| `OPTIONAL` | 字段可选 | 可选的请求参数 |
| `OUTPUT_ONLY` | 仅出现在响应中，请求中忽略 | `create_time`, `update_time` |
| `IMMUTABLE` | 创建后不可修改 | 不可变配置字段 |
| `INPUT_ONLY` | 仅出现在请求中，响应中不返回 | 密码、临时令牌 |
| `IDENTIFIER` | 资源标识符（仅用于 `name` 字段） | 资源名称 |

**规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 请求消息的每个字段 **必须** 标注 field_behavior | MUST | [AIP-203](https://google.aip.dev/203) |
| **必须** 至少使用 REQUIRED、OPTIONAL 或 OUTPUT_ONLY 之一 | MUST | [AIP-203](https://google.aip.dev/203) |
| `FIELD_BEHAVIOR_UNSPECIFIED` **不得** 使用 | MUST NOT | [AIP-203](https://google.aip.dev/203) |
| 输出字段（响应消息）**不应当** 标注 OUTPUT_ONLY | SHOULD NOT | [AIP-203](https://google.aip.dev/203) |

### 4.3 标准字段 [来源: AIP-148](https://google.aip.dev/148)

| 字段名 | 类型 | 行为 | 说明 | AIP |
|---|---|---|---|---|
| `name` | `string` | IDENTIFIER | 资源名称（第一个字段） | [AIP-148](https://google.aip.dev/148) |
| `parent` | `string` | REQUIRED | 父资源名称（List/Create 请求） | [AIP-148](https://google.aip.dev/148) |
| `display_name` | `string` | OPTIONAL | 人类可读名称，≤63 字符 | [AIP-148](https://google.aip.dev/148) |
| `title` | `string` | OPTIONAL | 正式名称 | [AIP-148](https://google.aip.dev/148) |
| `create_time` | `Timestamp` | OUTPUT_ONLY | 创建时间 | [AIP-148](https://google.aip.dev/148) |
| `update_time` | `Timestamp` | OUTPUT_ONLY | 最后更新时间 | [AIP-148](https://google.aip.dev/148) |
| `delete_time` | `Timestamp` | OUTPUT_ONLY | 软删除时间 | [AIP-148](https://google.aip.dev/148) |
| `purge_time` | `Timestamp` | OUTPUT_ONLY | 彻底清除时间 | [AIP-148](https://google.aip.dev/148) |
| `expire_time` | `Timestamp` | OPTIONAL | 过期时间 | [AIP-148](https://google.aip.dev/148) |
| `uid` | `string` | OUTPUT_ONLY | 系统分配的 UUID4 唯一标识 | [AIP-148](https://google.aip.dev/148) |
| `etag` | `string` | — | 乐观锁令牌（不标注 field_behavior） | [AIP-154](https://google.aip.dev/154) |
| `annotations` | `map<string,string>` | — | 客户端工具存储状态信息 | [AIP-148](https://google.aip.dev/148) |

### 4.4 标准化代码 [来源: AIP-143](https://google.aip.dev/143)

| 概念 | 字段命名 | 标准 | AIP |
|---|---|---|---|
| 国家/地区 | `region_code` | Unicode CLDR | [AIP-143](https://google.aip.dev/143) |
| 货币 | `currency_code` | ISO-4217 | [AIP-143](https://google.aip.dev/143) |
| 语言 | `language_code` | IETF BCP-47 | [AIP-143](https://google.aip.dev/143) |
| 时区 | `time_zone` | IANA TZ | [AIP-143](https://google.aip.dev/143) |
| UTC 偏移 | `utc_offset` | ISO-8601 | [AIP-143](https://google.aip.dev/143) |
| 内容类型 | `mime_type` | IANA Media Types | [AIP-143](https://google.aip.dev/143) |

**规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 标准化代码字段 **必须** 使用正确的数据类型（通常是 `string`） | MUST | [AIP-143](https://google.aip.dev/143) |
| **不应当** 使用枚举表示标准代码 | SHOULD NOT | [AIP-143](https://google.aip.dev/143) |
| 接受用户输入时 **应当** 大小写不敏感 | SHOULD | [AIP-143](https://google.aip.dev/143) |
| 提供给用户时 **应当** 使用规范大小写 | SHOULD | [AIP-143](https://google.aip.dev/143) |

### 4.5 字段格式 [来源: AIP-202](https://google.aip.dev/202)

字段格式通过 `google.api.FieldInfo.Format` 枚举标注 [来源: AIP-202](https://google.aip.dev/202)。

| 格式 | 说明 | 规则 | AIP |
|---|---|---|---|
| `UUID4` | UUID v4（RFC 4122） | 仅用于 `string` 类型 | [AIP-202](https://google.aip.dev/202) |
| `IPV4` | IPv4 地址（RFC 791） | 仅用于 `string` 类型 | [AIP-202](https://google.aip.dev/202) |
| `IPV6` | IPv6 地址（RFC 4291） | 仅用于 `string` 类型 | [AIP-202](https://google.aip.dev/202) |
| `IPV4_OR_IPV6` | IPv4 或 IPv6 | 仅用于 `string` 类型 | [AIP-202](https://google.aip.dev/202) |

**规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 格式化字段 **不得** 通过原始文本比较进行等价判断 | MUST NOT | [AIP-202](https://google.aip.dev/202) |
| 服务端 **可以** 对格式化值进行规范化 | MAY | [AIP-202](https://google.aip.dev/202) |
| 新格式 **必须** 由 IETF RFC 或 Google AIP 管理 | MUST | [AIP-202](https://google.aip.dev/202) |

### 4.6 时间和持续时间 [来源: AIP-142](https://google.aip.dev/142)

- 时间戳字段 **应当** 使用 `google.protobuf.Timestamp`（JSON: RFC-3339 格式）[来源: AIP-142](https://google.aip.dev/142)
- 持续时间字段 **应当** 使用 `google.protobuf.Duration` [来源: AIP-142](https://google.aip.dev/142)
- 日期时间字段 **应当** 使用 ISO-8601 格式 [来源: AIP-142](https://google.aip.dev/142)

### 4.7 重复字段 [来源: AIP-144](https://google.aip.dev/144)

| 规则 | 级别 | AIP |
|---|---|---|
| 重复字段名 **必须** 使用复数形式 | MUST | [AIP-144](https://google.aip.dev/144) |
| 重复字段 **应当** 文档化是否保证顺序 | SHOULD | [AIP-144](https://google.aip.dev/144) |

### 4.8 未设置字段值 [来源: AIP-149](https://google.aip.dev/149)

| 规则 | 级别 | AIP |
|---|---|---|
| 服务端 **应当** 将未设置的可选字段视为零值 | SHOULD | [AIP-149](https://google.aip.dev/149) |
| 需要区分"未设置"和"设置为零值"时，**应当** 使用包装类型 | SHOULD | [AIP-149](https://google.aip.dev/149) |

### 4.9 状态字段 [来源: AIP-216](https://google.aip.dev/216)

状态字段是特殊枚举，表示资源的生命周期状态 [来源: AIP-216](https://google.aip.dev/216)。

| 规则 | 级别 | AIP |
|---|---|---|
| 状态字段 **应当** 使用枚举 | SHOULD | [AIP-216](https://google.aip.dev/216) |
| 第一个值 **应当** 是 `{TYPE}_UNSPECIFIED` | SHOULD | [AIP-216](https://google.aip.dev/216) |
| 状态转换 **应当** 通过自定义方法触发，**不得** 在 Update 中直接写入 | SHOULD | [AIP-216](https://google.aip.dev/216) |
| 状态字段 **应当** 是 OUTPUT_ONLY | SHOULD | [AIP-216](https://google.aip.dev/216) |

---

## 5. 设计模式

### 5.1 分页 [来源: AIP-158](https://google.aip.dev/158)

| 规则 | 级别 | AIP |
|---|---|---|
| 返回集合的 RPC **必须** 在一开始就提供分页 | MUST | [AIP-158](https://google.aip.dev/158) |
| `page_size` **不得** 是必填字段 | MUST NOT | [AIP-158](https://google.aip.dev/158) |
| 未指定 `page_size` 或为 0 时，服务端选择默认值，**不得** 报错 | MUST | [AIP-158](https://google.aip.dev/158) |
| `page_size` 大于最大值时，**应当** 强制降级到最大值 | SHOULD | [AIP-158](https://google.aip.dev/158) |
| `page_size` 为负数时，**必须** 返回 `INVALID_ARGUMENT` | MUST | [AIP-158](https://google.aip.dev/158) |
| `page_token` **不得** 是必填字段 | MUST NOT | [AIP-158](https://google.aip.dev/158) |
| 分页令牌 **必须** 是不透明的 URL 安全字符串 | MUST | [AIP-158](https://google.aip.dev/158) |
| 分页令牌 **不得** 可被用户解析 | MUST NOT | [AIP-158](https://google.aip.dev/158) |
| 到达集合末尾时，`next_page_token` **必须** 为空 | MUST | [AIP-158](https://google.aip.dev/158) |
| 响应 **不得** 是流式响应 | MUST NOT | [AIP-158](https://google.aip.dev/158) |
| API **应当** 支持 `int32 skip` 字段跳过前 N 条结果 | SHOULD | [AIP-158](https://google.aip.dev/158) |
| 分页令牌 **应当** 有合理的过期时间（如 5 分钟） | SHOULD | [AIP-158](https://google.aip.dev/158) |
### 5.2 过滤 [来源: AIP-160](https://google.aip.dev/160)

过滤使用结构化字符串语法，放在 `string filter` 字段中 [来源: AIP-160](https://google.aip.dev/160)。

**语法要素**：

| 要素 | 示例 | 说明 |
|---|---|---|
| 字面量 | `Victor Hugo` | 搜索特定字段（由服务端定义） |
| 逻辑运算符 | `AND`, `OR` | `OR` 优先级高于 `AND` |
| 否定运算符 | `NOT`, `-` | 两种格式等价 |
| 比较运算符 | `=`, `!=`, `<`, `>`, `<=`, `>=` | 字段名必须在左侧 |
| 遍历运算符 | `a.b` | 穿透消息/映射 |
| Has 运算符 | `r:42`, `m:foo` | 检查集合/映射包含 |
| 通配符 | `a = "*.foo"` | 字符串相等性匹配 |
**规则**：

| 规则 | 级别 | AIP |
|---|---|---|
| 过滤字段 **应当** 叫 `filter` | SHOULD | [AIP-160](https://google.aip.dev/160) |
| 不合规的过滤字符串 **应当** 返回 `INVALID_ARGUMENT` | SHOULD | [AIP-160](https://google.aip.dev/160) |
| 字段名 **不得** 出现在比较运算符右侧 | MUST NOT | [AIP-160](https://google.aip.dev/160) |
| API **应当** 支持标准函数（`has`, `subsetOf`, `containsAny`, `containsAll`, `empty`） | SHOULD | [AIP-160](https://google.aip.dev/160) |
| 自定义函数 **必须** 以双冒号开头（`::myFunction`） | MUST | [AIP-160](https://google.aip.dev/160) |
### 5.3 字段掩码 [来源: AIP-161](https://google.aip.dev/161)

字段掩码用于指定 Update 请求中需要更新的字段 [来源: AIP-161](https://google.aip.dev/161)。

| 规则 | 级别 | AIP |
|---|---|---|
| 字段掩码 **必须** 使用 `google.protobuf.FieldMask` 类型 | MUST | [AIP-161](https://google.aip.dev/161) |
| 字段掩码 **必须** 相对于资源本身 | MUST | [AIP-161](https://google.aip.dev/161) |
| 读写行为 **必须** 自洽 | MUST | [AIP-161](https://google.aip.dev/161) |
| **不得** 允许通过索引访问重复字段元素 | MUST NOT | [AIP-161](https://google.aip.dev/161) |
| 输出字段在掩码中出现时，服务端 **必须** 忽略 | MUST | [AIP-161](https://google.aip.dev/161) |

**SODA 适配**：Java 中使用 `String` 逗号分隔列表模拟 FieldMask（如 `"nickname,mobile"`），而非 protobuf FieldMask。

### 5.4 变更验证 [来源: AIP-163](https://google.aip.dev/163)

| 规则 | 级别 | AIP |
|---|---|---|
| API **应当** 提供 `bool validate_only` 字段 | SHOULD | [AIP-163](https://google.aip.dev/163) |
| `validate_only` 请求 **必须** 执行权限检查和验证 | MUST | [AIP-163](https://google.aip.dev/163) |
| `validate_only` 请求 **必须** 返回与实际执行相同的响应 | MUST | [AIP-163](https://google.aip.dev/163) |
### 5.5 软删除 [来源: AIP-164](https://google.aip.dev/164)

| 规则 | 级别 | AIP |
|---|---|---|
| 支持软删除时，Delete 方法 **必须** 标记而非移除资源 | MUST | [AIP-164](https://google.aip.dev/164) |
| 软删除 **应当** 返回更新后的资源（而非 Empty） | SHOULD | [AIP-164](https://google.aip.dev/164) |
| **应当** 有 `delete_time` 和 `purge_time` 字段 | SHOULD | [AIP-164](https://google.aip.dev/164) |
| **应当** 提供 `Undelete` 自定义方法 | SHOULD | [AIP-164](https://google.aip.dev/164) |
| **可以** 提供 `Expunge` 自定义方法（彻底删除） | MAY | [AIP-164](https://google.aip.dev/164) |
| List 默认 **不应当** 返回已删除资源 | SHOULD NOT | [AIP-164](https://google.aip.dev/164) |
| List **应当** 提供 `bool show_deleted` 字段 | SHOULD | [AIP-164](https://google.aip.dev/164) |
| Get 已删除资源 **应当** 返回资源（而非 NOT_FOUND） | SHOULD | [AIP-164](https://google.aip.dev/164) |
| Undelete 对未删除资源 **必须** 返回 `ALREADY_EXISTS` | MUST | [AIP-164](https://google.aip.dev/164) |

---

## 6. 命名规范 [来源: AIP-190](https://google.aip.dev/190)

### 6.1 总体原则

| 原则 | 说明 | AIP |
|---|---|---|
| 直觉性 | 使用直观、熟悉的术语 | [AIP-190](https://google.aip.dev/190) |
| 一致性 | 同一概念用同一名称 | [AIP-190](https://google.aip.dev/190) |
| 简洁性 | 避免冗余词汇 | [AIP-190](https://google.aip.dev/190) |
| 美式英语 | 使用正确的美式英语拼写 | [AIP-190](https://google.aip.dev/190) |
| UpperCamelCase | 定义名使用 UpperCamelCase | [AIP-190](https://google.aip.dev/190) |

### 6.2 方法命名

格式：`VerbNoun`（UpperCamelCase），名词通常是资源类型 [来源: AIP-190](https://google.aip.dev/190)。

| 标准方法 | 命名 | AIP |
|---|---|---|
| 获取 | `Get{Resource}` | [AIP-131](https://google.aip.dev/131) |
| 列出 | `List{Resources}` | [AIP-132](https://google.aip.dev/132) |
| 创建 | `Create{Resource}` | [AIP-133](https://google.aip.dev/133) |
| 更新 | `Update{Resource}` | [AIP-134](https://google.aip.dev/134) |
| 删除 | `Delete{Resource}` | [AIP-135](https://google.aip.dev/135) |
| 自定义 | `{Verb}{Resource}` | [AIP-136](https://google.aip.dev/136) |

### 6.3 消息命名

| 规则 | 级别 | AIP |
|---|---|---|
| 消息名 **应当** 简短精炼 | SHOULD | [AIP-190](https://google.aip.dev/190) |
| **不应当** 包含介词（With, For） | SHOULD NOT | [AIP-190](https://google.aip.dev/190) |
| 如果没有不带形容词的对应消息，形容词可以省略 | SHOULD | [AIP-190](https://google.aip.dev/190) |
| 接口名 **应当** 使用名词或形容词+名词 | SHOULD | [AIP-190](https://google.aip.dev/190) |
| 接口名 **不应** 以 "Service" 结尾 | SHOULD NOT | [AIP-190](https://google.aip.dev/190) |
**请求/响应命名**：

| 类型 | 模式 | AIP |
|---|---|---|
| 标准方法请求 | `{Verb}{Resource}Request` | [AIP-131](https://google.aip.dev/131)~[135](https://google.aip.dev/135) |
| 标准方法响应 | `{Resource}` 或 `{Verb}{Resource}Response` | [AIP-131](https://google.aip.dev/131)~[135](https://google.aip.dev/135) |
| 自定义方法请求 | `{Verb}{Resource}Request` | [AIP-136](https://google.aip.dev/136) |
| 自定义方法响应 | `{Verb}{Resource}Response` | [AIP-136](https://google.aip.dev/136) |

---

## 7. 错误处理 [来源: AIP-193](https://google.aip.dev/193)

### 7.1 错误响应结构

服务端 **必须** 返回结构化错误响应 [来源: AIP-193](https://google.aip.dev/193)，包含：

| 字段 | 类型 | 说明 | AIP |
|---|---|---|---|
| `code` | `int` | HTTP 状态码 | [AIP-193](https://google.aip.dev/193) |
| `message` | `string` | 面向开发者的调试信息（英文） | [AIP-193](https://google.aip.dev/193) |
| `details` | `object` | 机器可读的错误详情 | [AIP-193](https://google.aip.dev/193) |

### 7.2 标准错误码

| HTTP 状态码 | gRPC 等价码 | 使用场景 | AIP |
|---|---|---|---|
| 200 | OK | 成功 | — |
| 400 | INVALID_ARGUMENT | 参数无效 | [AIP-193](https://google.aip.dev/193) |
| 401 | UNAUTHENTICATED | 未认证 | [AIP-193](https://google.aip.dev/193) |
| 403 | PERMISSION_DENIED | 无权限（先于存在性检查） | [AIP-193](https://google.aip.dev/193) |
| 404 | NOT_FOUND | 资源不存在 | [AIP-193](https://google.aip.dev/193) |
| 409 | ALREADY_EXISTS | 资源已存在 | [AIP-193](https://google.aip.dev/193) |
| 429 | RESOURCE_EXHAUSTED | 配额耗尽 | [AIP-193](https://google.aip.dev/193) |
| 500 | INTERNAL | 内部错误 | [AIP-193](https://google.aip.dev/193) |

### 7.3 错误消息规则

| 规则 | 级别 | AIP |
|---|---|---|
| 错误消息 **应当** 帮助开发者理解和解决问题 | SHOULD | [AIP-193](https://google.aip.dev/193) |
| 错误消息 **不得** 假设用户了解底层实现 | MUST NOT | [AIP-193](https://google.aip.dev/193) |
| 错误消息 **应当** 简洁但可操作 | SHOULD | [AIP-193](https://google.aip.dev/193) |
| 额外信息 **应当** 放在 `details` 字段 | SHOULD | [AIP-193](https://google.aip.dev/193) |
| 所有错误响应 **必须** 包含 `ErrorInfo` | MUST | [AIP-193](https://google.aip.dev/193) |
| 错误消息 **不应当** 进行本地化（如翻译） | SHOULD NOT | [AIP-193](https://google.aip.dev/193) |
| 面向用户的本地化消息 **应当** 通过 `details` 字段提供 | SHOULD | [AIP-193](https://google.aip.dev/193) |
### 7.4 ErrorInfo

`ErrorInfo` 是机器可读的错误标识符 [来源: AIP-193](https://google.aip.dev/193)。

| 字段 | 规则 | AIP |
|---|---|---|
| `reason` | **必须** 是 UPPER_SNAKE_CASE，≤63 字符，匹配 `[A-Z][A-Z0-9_]+[A-Z0-9]` | [AIP-193](https://google.aip.dev/193) |
| `domain` | **必须** 是全局唯一的域名（如 `soda-user.example.com`） | [AIP-193](https://google.aip.dev/193) |
| `metadata` | 键 **必须** ≤64 字符，匹配 `[a-z][a-zA-Z0-9-_]+` | [AIP-193](https://google.aip.dev/193) |

**reason 示例**：

| 推荐 | 避免 |
|---|---|
| `CPU_AVAILABILITY` | `THE_BOOK_YOU_WANT_IS_NOT_AVAILABLE` |
| `NO_STOCK` | `ERROR` |

### 7.5 权限检查顺序

```
权限检查 → 存在性检查 → 业务逻辑
```

| 规则 | 级别 | AIP |
|---|---|---|
| 权限检查 **必须** 先于存在性检查 | MUST | [AIP-193](https://google.aip.dev/193) |
| 无权限时 **必须** 返回 `PERMISSION_DENIED`（HTTP 403） | MUST | [AIP-193](https://google.aip.dev/193) |
| 无权限时 **不得** 暴露资源是否存在 | MUST NOT | [AIP-193](https://google.aip.dev/193) |

### 7.6 SODA 错误响应格式

```java
record Result<T>(
    @JsonProperty("code") int code,
    @JsonProperty("msg") String msg,
    @JsonProperty("data") @Nullable T data
) {}
```

错误示例：

```json
{
  "code": 404,
  "msg": "User not found",
  "data": null,
  "error": {
    "reason": "USER_NOT_FOUND",
    "domain": "soda-user.example.com",
    "metadata": {
      "userId": "42"
    }
  }
}
```

---

## 8. 向后兼容性 [来源: AIP-180](https://google.aip.dev/180)

### 8.1 三种兼容性类型

| 类型 | 说明 | AIP |
|---|---|---|
| **源代码兼容** | 客户端代码无需修改即可编译 | [AIP-180](https://google.aip.dev/180) |
| **线路兼容** | 序列化/反序列化格式不变 | [AIP-180](https://google.aip.dev/180) |
| **语义兼容** | 行为不变 | [AIP-180](https://google.aip.dev/180) |

### 8.2 向后兼容规则

| 规则 | 级别 | AIP |
|---|---|---|
| 新增字段 **必须** 向后兼容 | MUST | [AIP-180](https://google.aip.dev/180) |
| **不得** 更改已有字段的类型 | MUST NOT | [AIP-180](https://google.aip.dev/180) |
| **不得** 更改资源名称格式 | MUST NOT | [AIP-180](https://google.aip.dev/180) |
| **不得** 更改默认值 | MUST NOT | [AIP-180](https://google.aip.dev/180) |
| **不得** 更改序列化格式 | MUST NOT | [AIP-180](https://google.aip.dev/180) |
| 新增枚举值 **可以**，但不得更改已有值的数字映射 | MAY | [AIP-180](https://google.aip.dev/180) |
| 新增标准方法 **可以** | MAY | [AIP-180](https://google.aip.dev/180) |
| 行为变更 **必须** 通过新方法或新字段实现 | MUST | [AIP-180](https://google.aip.dev/180) |
| 废弃字段 **应当** 保留至少 3 年 | SHOULD | [AIP-180](https://google.aip.dev/180) |
### 8.3 破坏性变更

| 变更 | 是否破坏性 | AIP |
|---|---|---|
| 添加 `REQUIRED` 字段到已有请求 | 是 | [AIP-180](https://google.aip.dev/180) |
| 添加 `OUTPUT_ONLY` 字段到已有资源 | 否 | [AIP-180](https://google.aip.dev/180) |
| 添加 `OPTIONAL` 字段 | 否 | [AIP-180](https://google.aip.dev/180) |
| 删除字段 | 是 | [AIP-180](https://google.aip.dev/180) |
| 更改字段名 | 是 | [AIP-180](https://google.aip.dev/180) |
| 添加分页到已有方法 | 是 | [AIP-158](https://google.aip.dev/158) |

---

## 9. SODA 项目适配说明

### 9.1 已有约定与 AIP 的对齐

SODA 项目已有约定（ADR-0009）与 Google AIP 高度一致：

| SODA 约定 | AIP 对应 | 一致性 |
|---|---|---|
| `POST /users` 创建 | [AIP-133](https://google.aip.dev/133) | ✅ 一致 |
| `GET /users/{id}` 获取 | [AIP-131](https://google.aip.dev/131) | ✅ 一致（ID 替代 name） |
| `PATCH /users/{id}` 部分更新 | [AIP-134](https://google.aip.dev/134) | ✅ 一致 |
| `DELETE /users/{id}` 删除 | [AIP-135](https://google.aip.dev/135) | ✅ 一致 |
| `POST /users/{id}:disable` 自定义方法 | [AIP-136](https://google.aip.dev/136) | ✅ 一致 |
| 禁止 PUT | [AIP-134](https://google.aip.dev/134) 推荐 | ✅ 一致 |
| `Result<T>` 信封 | [AIP-193](https://google.aip.dev/193) HTTP 表示 | ✅ 适配 |
| `XxxRequest` / `XxxResponse` | [AIP-131](https://google.aip.dev/131)~[136](https://google.aip.dev/136) Request/Response | ✅ 一致 |
| `XxxWebAssembler` 转换 | 无直接 AIP 对应 | SODA 扩展 |

### 9.2 SODA 特有约定（与 AIP 差异）

| 差异点 | SODA 做法 | AIP 做法 | 原因 | ADR |
|---|---|---|---|---|
| 资源标识 | `{id}`（Long） | `name`（string 路径） | DDD 强类型 ID | — |
| 分页 | 委托给查询服务 | `page_size` / `page_token` | 读写分离架构 | — |
| 字段掩码 | 不使用 FieldMask | `google.protobuf.FieldMask` | Java REST 无 protobuf | — |
| 错误详情 | `ErrorInfo` + `reason`/`domain`/`metadata` | `google.rpc.Status` | 适配 REST 信封 | ADR-0013 |
| URL 命名 | camelCase（集合名、方法动词） | camelCase | 遵循 AIP-136 | ADR-0012 |
| 枚举值 | 短名存储（`E`/`D`），DTO 返回短名，desc 为英文 i18n key | UPPER_SNAKE_CASE | 保持简洁，为国际化保留扩展性 | ADR-0005 |
| 状态字段 | `state`（状态机）vs `status`（独立状态值） | `state` | 遵循 AIP-148 | ADR-0005 |
| 写操作响应 | 返回部分字段（无时间戳） | 返回完整资源 | 写接口不需要 | — |
| validate_only | `Command` 接口，默认 false，子类按需实现 | 每个变更方法 | 支持 | — |
| 权限检查 | 当前不实现 | 先于存在性检查 | 后续实现 | — |
### 9.3 新模块 API 设计检查清单

创建新的业务模块时，对照以下清单：

- [ ] **资源**：是否以资源为中心建模？（[AIP-121](https://google.aip.dev/121)）
- [ ] **标准方法**：是否优先使用 Get/List/Create/Update/Delete？（[AIP-130](https://google.aip.dev/130)）
- [ ] **自定义方法**：是否仅在标准方法无法表达时使用？（[AIP-136](https://google.aip.dev/136)）
- [ ] **HTTP 方法**：POST/GET/PATCH/DELETE 是否正确映射？（[AIP-130](https://google.aip.dev/130)）
- [ ] **URL 模式**：是否无冗余动词？（[AIP-133](https://google.aip.dev/133): `POST /users` 而非 `POST /users/create`）
- [ ] **URL 命名**：集合名和方法动词是否使用 camelCase？（[AIP-136](https://google.aip.dev/136)）
- [ ] **字段命名**：是否使用美式英语、camelCase、无介词？（[AIP-140](https://google.aip.dev/140)）
- [ ] **字段行为**：所有字段是否标注 REQUIRED/OPTIONAL/OUTPUT_ONLY？（[AIP-203](https://google.aip.dev/203)）
- [ ] **字段格式**：是否使用 Bean Validation 注解（`@Email`、`@Pattern`）？（[AIP-202](https://google.aip.dev/202)）
- [ ] **标准字段**：是否使用标准字段名（name, create_time, update_time）？（[AIP-148](https://google.aip.dev/148)）
- [ ] **状态字段**：状态机字段是否叫 `state`？（[AIP-148](https://google.aip.dev/148)）
- [ ] **枚举值**：RPC/HTTP 接口是否使用 String 类型？desc 是否为英文 i18n key？（[AIP-126](https://google.aip.dev/126)）
- [ ] **分页**：List 方法是否从一开始就包含分页？（[AIP-158](https://google.aip.dev/158)）
- [ ] **错误**：是否使用 ErrorInfo + reason/domain/metadata？（[AIP-193](https://google.aip.dev/193)）
- [ ] **权限检查**：是否先于存在性检查？（[AIP-193](https://google.aip.dev/193)）
- [ ] **软删除**：如适用，是否提供 Undelete？（[AIP-164](https://google.aip.dev/164)）
- [ ] **向后兼容**：是否避免破坏性变更？（[AIP-180](https://google.aip.dev/180)）

---

## 参考 AIP 索引

| AIP | 标题 | 关键内容 |
|---|---|---|
| [AIP-1](https://google.aip.dev/1) | Purpose and Guidelines | AIP 体系概述 |
| [AIP-8](https://google.aip.dev/8) | Style and Guidance | RFC 2119 关键词、写作风格 |
| [AIP-9](https://google.aip.dev/9) | Glossary | 术语定义 |
| [AIP-121](https://google.aip.dev/121) | Resource-oriented design | 资源导向设计核心原则 |
| [AIP-122](https://google.aip.dev/122) | Resource names | 资源命名规则 |
| [AIP-123](https://google.aip.dev/123) | Resource types | 资源类型定义 |
| [AIP-124](https://google.aip.dev/124) | Resource association | 资源关联模式 |
| [AIP-126](https://google.aip.dev/126) | Enumerations | 枚举设计规则 |
| [AIP-128](https://google.aip.dev/128) | Declarative-friendly interfaces | 声明式友好接口 |
| [AIP-130](https://google.aip.dev/130) | Methods | 方法分类与选择 |
| [AIP-131](https://google.aip.dev/131) | Standard methods: Get | Get 方法规范 |
| [AIP-132](https://google.aip.dev/132) | Standard methods: List | List 方法规范 |
| [AIP-133](https://google.aip.dev/133) | Standard methods: Create | Create 方法规范 |
| [AIP-134](https://google.aip.dev/134) | Standard methods: Update | Update 方法规范（PATCH vs PUT） |
| [AIP-135](https://google.aip.dev/135) | Standard methods: Delete | Delete 方法规范 |
| [AIP-136](https://google.aip.dev/136) | Custom methods | 自定义方法规范 |
| [AIP-140](https://google.aip.dev/140) | Field names | 字段命名规则 |
| [AIP-141](https://google.aip.dev/141) | Quantities | 数量字段规则 |
| [AIP-142](https://google.aip.dev/142) | Time and duration | 时间和持续时间 |
| [AIP-143](https://google.aip.dev/143) | Standardized codes | 标准化代码（货币、语言等） |
| [AIP-144](https://google.aip.dev/144) | Repeated fields | 重复字段规则 |
| [AIP-145](https://google.aip.dev/145) | Ranges | 范围字段规则 |
| [AIP-146](https://google.aip.dev/146) | Generic fields | 泛型字段规则 |
| [AIP-147](https://google.aip.dev/147) | Sensitive fields | 敏感字段规则 |
| [AIP-148](https://google.aip.dev/148) | Standard fields | 标准字段定义 |
| [AIP-149](https://google.aip.dev/149) | Unset field values | 未设置字段值处理 |
| [AIP-154](https://google.aip.dev/154) | Resource freshness validation | Etag 乐观锁 |
| [AIP-156](https://google.aip.dev/156) | Singleton resources | 单例资源 |
| [AIP-158](https://google.aip.dev/158) | Pagination | 分页规范 |
| [AIP-160](https://google.aip.dev/160) | Filtering | 过滤语法 |
| [AIP-161](https://google.aip.dev/161) | Field masks | 字段掩码 |
| [AIP-163](https://google.aip.dev/163) | Change validation | 变更验证（validate_only） |
| [AIP-164](https://google.aip.dev/164) | Soft delete | 软删除规范 |
| [AIP-180](https://google.aip.dev/180) | Backwards compatibility | 向后兼容性 |
| [AIP-190](https://google.aip.dev/190) | Naming conventions | 命名规范 |
| [AIP-193](https://google.aip.dev/193) | Errors | 错误处理规范 |
| [AIP-202](https://google.aip.dev/202) | Fields | 字段格式（UUID4, IPv4 等） |
| [AIP-203](https://google.aip.dev/203) | Field behavior documentation | 字段行为标注 |
| [AIP-216](https://google.aip.dev/216) | States | 状态枚举设计 |
