---
type: Decision Record
title: 异常类使用约定：类型族与 IAE 校验通道
description: 抛异常、写领域校验或新增错误状态码时读——需要 HTTP 状态码细分的业务拒绝走 ProblemDetailException 族（404 / 409 / 412，场景静态工厂，落点 com.soda.component.api.error）；其余客户端可改请求的失败（格式 / 值不可表示、未类型化业务拒绝）走带消息 IAE；domain 层不建族。
tags: [exception, validation, domain, api]
status: stable
---

# 0015 — 异常类使用约定：类型族与 IAE 校验通道

写侧异常收成两条通道。 **IAE（`IllegalArgumentException`）＝ 格式 / 值不可表示的校验通道**：DP 工具（`ValidateUtils` /
`ParseUtils` / `UpdateMask`）、domain 断言（`User` / `Verification` 的 `Assert`）、基础设施防御守卫都用它，统一译 400
——客户端可改请求的一切（错码、过期、状态前置拒绝、终态写入）都归此通道；校验的写法（构造器 DP 式校验、方法零守卫、带消息 IAE
业务校验）见 [framework-crosscutting.md](../conventions/framework-crosscutting.md) §5。 **需要被 HTTP
状态码细分的业务拒绝**（资源不存在 /
唯一键占用 / 条件请求失配）走 `ProblemDetailException` 族，由 app-service 与组件层守卫抛出——类型落点在
`com.soda.component.api.error`（`soda-component-api-starter`，api 契约层无 spring-web 依赖，故只承载 `int` 状态码、
不引用 Spring 的 `ProblemDetail`）。抽象基类 `ProblemDetailException extends RuntimeException` 暴露
`public final int status()`；
构造器 `protected ProblemDetailException(int status, String message)` 断言 4xx–5xx，越界抛 ISE。三个子类 `final` +
私有构造 + 场景静态工厂，状态码钉死：

| 类型                          | 状态码 | 静态工厂                                     | 消息                                                                     |
|-------------------------------|--------|----------------------------------------------|--------------------------------------------------------------------------|
| `NotFoundException`           | 404    | `entityNotFound(String resource, Object id)` | `"<resource> not found: <id>"`（如 `User not found: 1`）                 |
| `ConflictException`           | 409    | `alreadyExists(String field, Object value)`  | `"<field> already exists: <value>"`（如 `Username already exists: tom`） |
| `PreconditionFailedException` | 412    | `versionMismatch(int expected, int current)` | `"If-Match <expected> does not match current <current>"`                 |

domain 层不建异常族：`soda-xxx-domain` 看不见 api-starter（依赖方向 `api → domain`），族只在 app-service 与组件守卫侧使用。

## Consequences

- 未类型化的业务拒绝（同值换绑、活跃验证码已存在、无待用验证码）本轮维持 IAE → 400；需要细分时再逐点升格为族（延期项）。
- 守卫的抛出点见 ADR-0037 / ADR-0039，web 层翻译归属见
  ADR-0013，状态码分配见 [aip-api-conventions.md](../conventions/aip-api-conventions.md) §8.2。
- 防御编程守卫（网关终态拒写、`getId()` 未标识调用、`assignId` / `assignVersion` 回调非法等兜底）不携消息——异常类型 +
  栈帧即语义（裸 ISE / NPE / NSE），非客户端反馈通道。
