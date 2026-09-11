---
type: Decision Record
title: 条件请求传输契约：ETag 派生与 If-Match 归一化
description: 设计或评审条件请求（ETag / If-Match / 乐观锁版本令牌）的传输侧行为时读——出站 ETag 由 HttpValidatorSource 派生、入站 If-Match 由 starter-web 归一化为 @IfMatch 参数，缺失与 `*` 放行、弱标签与版本失配 412、非数字 400；版本比对不在此，归应用层同事务守卫。
tags: [ conditional-request, etag, if-match, aip-154, optimistic-lock ]
status: stable
---

# 0039 — 条件请求传输契约：ETag 派生与 If-Match 归一化

条件请求的 **传输侧**——头怎么发、怎么读、语法非法怎么拒——与 **版本比对**分离：出站由响应体实现
`HttpValidatorSource`（仅暴露强验证器原料 `version()`）交给 `HttpValidatorHeadersAdvice` 统一补
`ETag: "n"`，入站由 starter-web `IfMatchResolver` 把 `If-Match` 归一化为 `@IfMatch Integer` 参数（缺失 / 空 /
`*` → null = 放行；取逗号列表首值；弱标签 `W/` 永不强匹配 → 412；非数字 → 400），
比对留在应用层同一事务内的 `AbstractAppService.requireIfMatch`（见 ADR-0037）。
Controller 因此只裸返回资源本身（无信封）、零设头样板，两端业务模块都不感知 HTTP 头。

## Consequences

- 强验证器单发：`Last-Modified` 弱验证器不提供——审计列属基础设施表示、不入领域与出站模型（ADR-0031），
  故无驻留可派生源；RFC 7232 §2.4 的双发降为单发，记为 AIP-154 偏离（见
  [aip-api-conventions.md](../conventions/aip-api-conventions.md) §9.2）。
- `ETag` 值即乐观锁令牌：`version` 承担 AIP-154 `etag` 角色（opaque concurrency token），
  出站响应不新增 `etag` 字段、入站请求体不新增 `etag` 字段（见
  [aip-api-conventions.md](../conventions/aip-api-conventions.md) §3.5/§4）。
- 版本失配与弱标签统一译 **412 `Precondition Failed`**（RFC 9110 §15.5.13「请求头字段中的条件求值为假」）：
  弱标签由 `IfMatchResolver` 在归一化时以 `ResponseStatusException(412)` 拒绝（经 `ProblemDetailAdvice` 继承的基类翻译）；
  版本失配由应用层守卫直抛 `PreconditionFailedException.versionMismatch`（`com.soda.component.api.error`，见 ADR-0015），
  web 层按 `ex.status()` 翻译——类型在 api 契约层，domain 不感知 HTTP（AIP-154 的 `ABORTED` 在 HTTP 层由 412 承担，见
  [aip-api-conventions.md](../conventions/aip-api-conventions.md) §9.2）。
- 缺失 `If-Match` 放行：AIP-154 只要求「提供时必须校验」（`etag` 字段本身是 MAY），不强制客户端必带；
  缺席是传输层语义、不构成并发声明，即按无版本冲突处理（持久层 `@Version` 兜底仍译 409）。
- 装配单入口 `IfMatchAutoConfiguration`（注册解析器 + 导入 advice），随组件位于运行时类路径生效；
  `webmvc` 切片下 `ResponseBodyAdvice` 类型不可见时不注册（见 [framework-conventions.md](../framework-conventions.md)
  §3.2）。
