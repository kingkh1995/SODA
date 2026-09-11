---
type: Decision Record
title: 乐观锁版本校验收敛：Versioned 聚合契约与 VersionAwareCommand 命令契约
description: 写带 If-Match 的更新用例、给聚合加版本或新增状态前置守卫时读——版本比对归 AbstractAppService.requireIfMatch 基类守卫（加载 + 断言），失配直抛 PreconditionFailedException.versionMismatch 译 412；命令侧 VersionAwareCommand 显式 opt-in，聚合侧 Versioned 声明可比对版本并在落库后回填权威版本。
tags: [ optimistic-lock, guard, application-service ]
status: stable
---

# 0037 — 乐观锁版本校验收敛：Versioned 聚合契约与 VersionAwareCommand 命令契约

条件请求版本比对是跨用例横切关注点，不应以内联散在各 ApplicationService 方法中——命令侧以 VersionAwareCommand
声明期望版本（Integer 承载），聚合侧以 Versioned 声明可比对版本（`ifMatch` 判定原语），实际比对收敛在
`AbstractAppService.requireIfMatch`（加载 + 断言一体，返回实体，与 `require` / `requireNotTerminal` 同族）：
失配直抛 `PreconditionFailedException.versionMismatch(expected, current)` → 412（类型落点
`com.soda.component.api.error`，见 ADR-0015；web 层翻译见 ADR-0013 / ADR-0039）。版本令牌的权威值由基础设施在
写库 flush 后经 `Versioned.assignVersion` 回填聚合——递增归基础设施层（JPA `@Version`），聚合不自增。

## Consequences

- 未实现 VersionAwareCommand 的命令自然放行（显式 opt-in，类型系统保证）；守卫对不实现 `Versioned` 的聚合拒绝
  （fail-loud：条件请求不适用于无版本列的实体，不静默丢弃 If-Match），裸 ISE 译 500（防御编程，见 ADR-0015）。
- 计算与加载不再分离：`requireIfMatch` 复用 `require`（未找到 → `NotFoundException.entityNotFound` →
  404），比对在基类一次完成，调用点一句编排。
- 失配译 **412 `Precondition Failed`**（RFC 9110 §15.5.13「请求头字段中的条件求值为假」）：抛出点是守卫的类型工厂
  `PreconditionFailedException.versionMismatch`，web 层按 `ex.status()` 翻译——类型在 api 契约层，domain 不感知 HTTP
  （AIP-154 的 `ABORTED` 在 HTTP 层由 412 承担，见 [aip-api-conventions.md](../conventions/aip-api-conventions.md)
  §9.2）。与持久层
  `OptimisticLockingFailureException` 译 409 的关系：本守卫是同一事务内的前置检查（客户端带了条件头），
  持久层是提交时 `WHERE version = ?` 兜底（客户端未带条件头时的竞态）；两者客户端动作一致（重读后重试）。
- `expectedVersion == null`（If-Match 缺失 / `*`）= 放行——缺席是传输层语义、不构成并发声明；判定在 `requireIfMatch`
  入口一次完成，不把可空版本下沉进领域。
- 聚合令牌与行版本恒一致：网关 `saveAndFlush` 后回填落库版本而非领域自增——是否发 UPDATE（从而递增）由持久化层
  按脏字段与审计列决定，聚合自增会漂移；一旦漂移，响应 `ETag`（`HttpValidatorHeadersAdvice` 由
  `HttpValidatorSource.version()` 派生）与行版本不再一致，客户端回带同一次响应发出的 `If-Match` 必被拒。
  `Versioned.assignVersion` 与 `Entity.assignId` 同族：聚合不自造持久化事实，只接受回填——两个回填方法均为
  基础设施回调，参数守卫（`assignId` 非空、异值抛裸 ISE；`assignVersion` 非空、回退抛裸 ISE）只拦调用方 bug，
  不构成业务分支（见 ADR-0015）。
