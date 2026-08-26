---
type: Decision Record
title: 错误响应结构：ErrorInfo 语义详情
description: 设计错误响应结构时读——Result 信封增机器可读 ErrorInfo（reason/domain/metadata）对齐 AIP-193，客户端按 reason 分支处理不依赖魔法数字。
tags: [error-response, api, aip-193]
status: stable
---

# 0013 — 错误响应结构：ErrorInfo 语义详情

错误响应在 `Result<T>` 信封上增加机器可读的 `ErrorInfo`——`reason`（UPPER_SNAKE_CASE 语义码）、`domain`（服务域）、`metadata`
（键值上下文，如哪个用户名重复），对齐 AIP-193：客户端按 `reason` 程序化分支处理，不硬编码数字 code。信封形状
`{code, msg, data?, error?}`，null 字段整体省略；`msg` 面向开发者（英文，不本地化），面向用户的中文提示由前端翻译层承担。

## Consequences

- 权限检查必须先于存在性检查（`PERMISSION_DENIED` 403，不得暴露资源是否存在）
