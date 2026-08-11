# 0012 — URL 命名规范：遵循 Google AIP camelCase

URL 路径中的资源集合名和自定义方法动词统一使用 camelCase，遵循 Google AIP 规范，不使用主流 REST 的 kebab-case。

**Status**: accepted

## Context

URL 命名有两种主流风格：

| 风格 | 示例 | 来源 |
|---|---|---|
| camelCase | `/userAccounts/{id}`、`:changePassword` | Google AIP (gRPC transcoding) |
| kebab-case | `/user-accounts/{id}`、`:change-password` | 主流 REST 惯例 |

Google AIP 是 gRPC 优先的设计规范——protobuf RPC 名是 camelCase，HTTP transcoding 保持这个格式。AIP-122 要求集合标识符使用 camelCase，AIP-136 要求自定义方法动词使用 camelCase。

SODA 虽然是纯 REST API，但选择遵循 AIP 规范，原因：
1. **一致性** — 团队已采用 AIP 作为 API 设计规范，URL 风格应保持一致
2. **未来兼容** — 如果未来引入 gRPC，URL 格式无需变更
3. **工具链兼容** — Google Cloud 客户端库、API Gateway 等工具期望 camelCase

## Decision

### 命名规则

| 场景 | 规范 | 示例 | AIP |
|---|---|---|---|
| 资源类型名（protobuf/message） | PascalCase | `UserAccount` | AIP-123 |
| 集合标识符（URL 路径中的集合名） | camelCase | `userAccounts` | AIP-122 |
| 资源 ID 段（系统生成 Long 类型） | 数字（无格式约束） | `42`（URL 渲染为 `/users/42`） | AIP-122 |
| 资源 ID 段（用户指定 string 类型） | 全小写+中划线 | `vhugo1802`, `my-instance` | AIP-122 |
| 自定义方法动词（URL 中 `:` 后） | camelCase | `:changePassword` | AIP-136 |

### URL 示例

```
GET    /users/{id}                    ← 集合名 camelCase（单单词）
PATCH  /users/{id}
POST   /users/{id}:changePassword     ← 自定义方法 camelCase

GET    /authAccounts/{id}             ← 集合名 camelCase（多单词）
PATCH  /authAccounts/{id}
```

### 资源 ID 格式说明

| ID 类型 | 来源 | 示例 URL | 规则 |
|---|---|---|---|
| Long（数字） | 系统自动生成（自增/序列） | `/users/42` | 数字本身无格式问题，无需转换 |
| UUID（字符串） | 系统自动生成 | `/users/a1b2c3d4-...` | 全小写+中划线保持 AIP 规范 |
| 语义化 ID（字符串） | 用户指定 | `/authAccounts/vhugo1802` | 必须全小写+中划线，正则 `^[a-z]([a-z0-9-]{0,61}[a-z0-9])?$` |

自定义方法动词统一首字母小写 camelCase，如 `:changePassword` 而非 `:change-password` 或 `:ChangePassword`。

### 对照表

| 操作 | AIP (camelCase) | 主流 REST (kebab-case) |
|---|---|---|
| 获取用户列表 | `GET /users` | `GET /users` |
| 获取单个用户 | `GET /users/{id}` | `GET /users/{id}` |
| 修改密码 | `POST /users/{id}:changePassword` | `POST /users/{id}:change-password` |
| 发送换绑验证码 | `POST /users/{id}:requestChangeMobileCode` | `POST /users/{id}:request-change-mobile-code` |

> 注（2026-08-10）：示例动词随端点改名更新（原 `:verifyMobile` → `:requestChangeMobileCode`，见 ADR-0011 2026-08-10 修订）；约定本身（camelCase 自定义方法）不变。

## Considered Options

- **camelCase (AIP)** — 符合 Google AIP 规范，gRPC 兼容，但与主流 REST 惯例冲突。选择。
- **kebab-case (主流 REST)** — curl/浏览器友好，行业共识，但违反 AIP-136 硬性要求。拒绝。

## Consequences

- 所有 URL 路径统一使用 camelCase，无 kebab-case 混用
- 自定义方法动词使用 camelCase，如 `:changePassword` 而非 `:change-password`
- 集合标识符使用 camelCase，如 `userAccounts` 而非 `user-accounts`
- 如果未来引入 gRPC，URL 格式无需变更
- 前端调用时需注意 camelCase 拼写（如 `changePassword` 而非 `change-password`）
