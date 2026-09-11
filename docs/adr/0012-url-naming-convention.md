---
type: Decision Record
title: URL 与 HTTP 方法规范：Google AIP 风格
description: 定义 URL 与 HTTP 方法时读——全程 camelCase，自定义方法冒号后缀 POST /{id}:action，更新一律 PATCH 禁 PUT，资源标识符走路径不进请求体。
tags: [url, api, aip]
status: stable
---

# 0012 — URL 与 HTTP 方法规范：Google AIP 风格

URL 与 HTTP 方法遵循 Google AIP——集合名 camelCase（`userAccounts` 而非 `user-accounts`），自定义方法为资源上的冒号后缀
`POST /{id}:action`（`:changePassword`、`:requestChangeMobile`，AIP-136），资源标识符走路径不进请求体；更新一律 PATCH
字段级部分更新、禁 PUT——全量替换语义在新增字段时会静默丢数据（AIP-134），触发领域逻辑的操作（校验旧凭证、状态迁移）用 POST
冒号方法，纯字段修改才用 PATCH。选 AIP 而非主流 REST kebab-case，为的是 API 设计规范全局一致，未来引入 gRPC 时 URL 形态不变。

## Consequences

- 现行端点全部为资源级（`/{id}:action`）；无资源可寻址场景（注册/登录发码）的集合级自定义方法形态归 ADR-0026 记载。
