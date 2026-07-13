# 0009 — Adapter-Web Request/Response + Assembler 模式

Adapter 接口层采用 `XxxRequest` / `XxxResponse` 作为 HTTP 专用入参出参，通过 `XxxWebAssembler` 做 `Request→Command` 和 `DTO→Response` 转换，使用统一的 `Result<T>` 信封包裹响应。

**Status**: accepted

## Context

Controller 不能直接把 api 模块的 `Command` 和 `DTO` 暴露为 HTTP 契约，原因：

1. **格式差异** — Request 承载 JSR 380 `@Valid` 校验（HTTP 边界拒绝），Command 承载领域 DP 构造（领域层拒绝），两者校验时机和粒度不同。即使字段相同，职责不同。
2. **项目级 API 规范** — 所有 HTTP 响应统一包裹 `{ code, msg, data }` 信封（参照 Yudao `CommonResult`），DTO 只描述 `data` 形状，不感知信封。
3. **独立变化** — Request/Response 随前端协议变化（字段命名、分组、可选性），Command/DTO 随领域变化。两种变化频率不同，不应耦合。
4. **Assembler 承载转换逻辑** — 转换不是 1:1 字段复制：分页包装、多实体聚合、URL 拼接、依赖注入等场景需要独立的可测试组件。

## Decision

每个业务模块的 adapter 层结构：

```
soda-xxx-adapter/src/main/java/com/soda/xxx/web/
├── request/
│   ├── CreateXxxRequest.java
│   ├── UpdateXxxRequest.java
│   └── …
├── response/
│   ├── XxxResponse.java
│   └── XxxPageItemResponse.java
├── XxxWebAssembler.java          ← @Component，双向转换
├── XxxController.java
└── package-info.java
```

命名规则：

| 层 | 类型 | 命名 | 职责 |
|---|---|---|---|
| Adapter Request | HTTP 入参 | `XxxRequest` | `@Valid` 校验，Jackson `@JsonProperty` 映射 |
| Adapter Response | HTTP 出参 | `XxxResponse` | `data` 段的数据形状，不含信封 |
| Adapter Assembler | 转换器 | `XxxWebAssembler` | `Request→Command` + `DTO→Response` |
| Api Command | 应用层入参 | `XxxCommand` | Structured 写操作入参 |
| Api DTO | 应用层出参 | `XxxDTO` | 应用层返回数据结构 |
| Shared Envelope | 统一响应 | `Result<T>` | `{ code, msg, data }` 信封 |

Controller 模式：

```java
@PostMapping("/create")
public Result<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
    CreateUserCommand cmd = assembler.toCommand(request);
    UserDTO dto = userService.createUser(cmd);
    return Result.success(assembler.toResponse(dto));
}
```
Controller 直接调用 `Result.success(data)` / `Result.success()` 构建响应，不需要基类。
soda-component-adapter-starter 只有一个公共同件：

```
soda-component-adapter-starter/src/main/java/com/soda/component/web/
├── package-info.java          ← @ApplicationModule(CLOSED, deps: {api})
└── Result.java              ← 统一响应信封

`Result<T>` 定义在 `soda-component-adapter-starter` 的 `com.soda.component.web` 包中，所有模块共用。

## Considered Options

- **静态方法 on Request/Response** — 违反 SRP，无法依赖注入，不适合分页/聚合等复杂转换。拒绝。
- **扁平无子目录** — 6+ Request + 6+ Response 混在 `web/` 下，文件数 15+ 时难以导航。选择子目录。
- **缩写命名 `XxxReq`/`XxxResp`** — 不符合项目「完整词优先」的命名风格。选择全称 `Request`/`Response`。
- **AbstractController 基类** — 只做 `Result.success()` 的纯委托，无行为收益，增加继承耦合。初始实现后删除，决策记录在此。

## Consequences

- Controller 变薄：只做 HTTP 协议映射，不碰业务逻辑。
- Assembler 是可单独单元测试的组件，覆盖所有转换边界。
- 新增模块时有一致样板可复制。
- 缺点是每个 HTTP 操作需要 3 个类（Request + Response + Assembler 方法），文件数量增加。
