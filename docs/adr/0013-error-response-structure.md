---
type: Decision Record
title: 错误响应结构：RFC 9457 ProblemDetail 最小集（状态码承载语义，AIP-193 不遵守）
description: 设计错误响应结构、调整异常→状态码映射或新增 advice 时读——SODA HTTP API 错误响应采用 Spring RFC 9457 ProblemDetail 最小集，语义由 HTTP 状态码承载（不设 type 自定义词表），唯一翻译层 ProblemDetailAdvice 继承 ResponseEntityExceptionHandler 一并翻译框架异常；AIP-193 ErrorInfo 不设。
tags: [ error-response, api, aip-193, rfc-9457, problem-detail ]
status: stable
---

# 0013 — 错误响应结构：RFC 9457 ProblemDetail 最小集（状态码承载语义，AIP-193 不遵守）

错误响应采用 **Spring RFC 9457 `ProblemDetail`**：线上形态 `{ title, status, detail, instance }`（最小集）；错误语义完全由
**HTTP 状态码**承载—— **不设 `type` 自定义词表**，客户端按 `status` 分支（分配与客户端动作见
[aip-api-conventions.md](../conventions/aip-api-conventions.md) §8.2）；`title` / `status` 取 `ProblemDetail.forStatus`
默认值（ReasonPhrase / 状态码，不显式设置）；`detail` 面向开发者英文（不本地化，前端翻译层承担），默认取异常消息，例外为固定文案——
500 `"Internal server error"`、持久层 409 `"Concurrent modification"`；`instance` 是请求 URI。AIP-193
`ErrorInfo{reason, domain, metadata}` **不设**（记为不遵守，见
[aip-api-conventions.md](../conventions/aip-api-conventions.md) §9.2）。

## Consequences

- 翻译层唯一：`com.soda.component.web.error.ProblemDetailAdvice`（`soda-component-adapter-starter-web`，经
  `AutoConfiguration.imports` 注册，单一 `problem()` 出口） **继承 `ResponseEntityExceptionHandler`**——请求体校验、类型转换、
  媒体类型、方法不支持等框架异常由继承的 handler 一并翻译，项目不为框架异常另设 advice；Boot 的
  `ProblemDetailsExceptionHandler`
  带 `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`，本 advice 存在时它不注册。
- 自有映射四条：
  1. `ProblemDetailException` → 状态取 `ex.status()`，`detail = ex.getMessage()`（类型族见 ADR-0015）；
  2. `IllegalArgumentException`（格式 / 值不可表示通道，见 ADR-0015）→ 400，`detail = ex.getMessage()`；
  3. `ConstraintViolationException`（类级 `@Validated` 触发的 AOP 方法校验，不在框架异常清单内）→ 400，
     `detail = ex.getMessage()`；
  4. `OptimisticLockingFailureException`（持久层 `@Version` 竞态）→ 409，`detail` 固定文案 `"Concurrent modification"`；
     `Exception` → 500，`detail` 固定文案 `"Internal server error"` + `log.error`。
- 状态分配依据 RFC 9110 与 `HttpStatus` 语义，表在 [aip-api-conventions.md](../conventions/aip-api-conventions.md) §8.2（本
  ADR 不重述）；守卫的抛出点见 ADR-0037 / ADR-0039。
- `ConstraintViolationException` 的消息为 jakarta 格式 `propertyPath: message, ...`，`-parameters` 已开故为真实参数名。
- Controller 裸返回 + `@ResponseStatus`（见 ADR-0009；条件请求头由 `HttpValidatorHeadersAdvice` 统一补，见 ADR-0039）。
