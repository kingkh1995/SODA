---
type: Decision Record
title: AuthAccount sealed 层级
description: 新增认证方式或建模认证类型层级时读——AuthAccount 用 sealed class 封闭恰好四种，穷尽模式匹配由编译器强制。
tags: [auth-account, sealed, type-hierarchy]
status: stable
---

# 0006 — AuthAccount sealed 层级

AuthAccount 采用 sealed class 层级，permits PasswordAuthAccount、SmsAuthAccount、EmailAuthAccount、SocialAuthAccount
恰好四种，替代抽象类加类型鉴别字段的形态。穷尽模式匹配由编译器强制：新增认证方式必须修改 permits
子句，遗漏分支在编译期暴露而非运行时静默漏过；封闭集合同时镜像领域不变量——认证方式集合封闭，不可在运行时扩展。
