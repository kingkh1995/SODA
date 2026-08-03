# 0013 — 错误响应结构：引入 ErrorInfo

错误响应在 `Result<T>` 基础上增加 `ErrorInfo` 结构，包含语义化错误码 `reason`、服务域 `domain`、上下文 `metadata`，遵循 AIP-193。

**Status**: accepted

## Context

当前 SODA 错误响应只有 `code`（数字）+ `msg`（描述），存在以下问题：

1. **语义不明** — 客户端需要硬编码数字 code 判断错误类型（如 `100001` = 用户名已存在）
2. **无法携带上下文** — 无法告诉客户端"哪个"用户名重复
3. **本地化问题** — `msg` 直接返回中文，违反 AIP-193「错误消息 **不应当** 进行本地化」

Google AIP-193 要求所有错误响应**必须**包含 `ErrorInfo`：

```json
{
  "code": 409,
  "message": "Username already exists",
  "details": [{
    "@type": "type.googleapis.com/google.rpc.ErrorInfo",
    "reason": "ALREADY_EXISTS",
    "domain": "soda-user.example.com",
    "metadata": { "username": "admin" }
  }]
}
```

## Decision

### 错误响应格式

```json
{
  "code": 409,
  "msg": "Username already exists",
  "error": {
    "reason": "ALREADY_EXISTS",
    "domain": "soda-user.example.com",
    "metadata": {
      "username": "admin"
    }
  }
}
```

> 修订（2026-08-03）：`Result` / `ErrorInfo` 均标注 `@JsonInclude(NON_NULL)`——`data` 为 null（错误路径）时整体省略，`error.metadata` 为 null 时同样省略。信封形状为 `{code, msg, data?, error?}`。

### 字段说明

| 字段 | 类型 | 说明 | AIP |
|---|---|---|---|
| `code` | `int` | HTTP 状态码 | AIP-193 |
| `msg` | `string` | 面向开发者的调试信息（英文） | AIP-193 |
| `error` | `ErrorInfo` | 机器可读的错误详情 | AIP-193 |
| `error.reason` | `string` | UPPER_SNAKE_CASE 语义码 | AIP-193 |
| `error.domain` | `string` | 服务域，格式 `{service}.example.com` | AIP-193 |
| `error.metadata` | `map` | 键值对上下文 | AIP-193 |

### 错误码映射

| HTTP 状态码 | reason | 使用场景 |
|---|---|---|
| 400 | `INVALID_ARGUMENT` | 参数无效 |
| 401 | `UNAUTHENTICATED` | 未认证 |
| 403 | `PERMISSION_DENIED` | 无权限 |
| 404 | `NOT_FOUND` | 资源不存在 |
| 409 | `ALREADY_EXISTS` | 资源已存在 |
| 429 | `RESOURCE_EXHAUSTED` | 配额耗尽 |
| 500 | `INTERNAL` | 内部错误 |

### 权限检查顺序

遵循 AIP-193：权限检查 **必须** 先于存在性检查。

```
权限检查 → 存在性检查 → 业务逻辑
```

无权限时返回 `PERMISSION_DENIED`（403），**不得** 暴露资源是否存在。

## Considered Options

- **扩展 Result + ErrorInfo** — 符合 AIP-193，客户端可程序化处理错误。选择。
- **保持 code + msg** — 简单，但客户端需硬编码数字 code，无法携带上下文。拒绝。

## Consequences

- `Result<T>` 类需要增加可选的 `error` 字段
- 所有业务异常需要提供 `reason`、`domain`、`metadata`
- `msg` 统一使用英文（AIP-193 要求不本地化）
- 中文错误提示通过 `metadata` 或前端翻译层提供
- 客户端可以根据 `error.reason` 分支处理，不再依赖魔法数字
- 新增业务模块时，需要定义该模块的 `domain` 和 `reason` 常量
