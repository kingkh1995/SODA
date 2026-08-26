---
type: Decision Record
title: Masked Value DP 与脱敏命名
description: 设计脱敏值 DP 或定三层词汇时读：已脱敏值是落库存储形态（Masked* record，非 SensitiveValue 子类），读回格式正则承重，三层词汇 SecretValue/SensitiveValue/Masked* 定界。
tags: [dp, data-protection, masking]
status: stable
---

# 0032 — Masked Value DP 与脱敏命名

已脱敏值（masked）是落库存储形态（展示场景），由 `Masked*` record 家族承载（`MaskedMobile`/`MaskedEmail`/`MaskedIdCard`/
`MaskedBankCard`/`MaskedChineseName`，实现 `StringLiteralType`）——脱敏串不含 PII 明文、不敏感，故不继承 `SensitiveValue`
，record 即满足简单 DP 形态；读回以格式正则校验承重（落库值不可篡改/非法）。三层词汇定界：`SecretValue`（瞬态凭证，永不展示）/
`SensitiveValue`（长期 PII，脱敏后展示）/`Masked*`（已脱敏落库形态）；每个 `MaskedXxx` 必须有成对原始值 DP（不接受孤儿掩码类），
`SensitiveValue` 基类收敛为单抽象方法 `maskedValue()`
。派生通道、掩码算法同址等机制约定见 [dp-conventions](../dp-conventions.md)。

## Consequences

- 防护模式总分类见 [ADR-0030](0030-data-protection-dp-categories.md)。
