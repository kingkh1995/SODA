---
type: Decision Record
title: 数据保护 DP 四分类与 SensitiveValue 基类
description: 分类敏感数据 DP 或选 SensitiveValue 基类时读：数据保护按泄漏面分四类（Raw/Encrypted/Hashed/Masked），PII DP 继承 SensitiveValue 编译期脱敏，SecretValue 互补不合并。
tags: [data-protection, dp, sensitive-value]
status: stable
---

# 0030 — 数据保护 DP 四分类与 SensitiveValue 基类

数据保护按泄漏面分四类治理： **Raw**（明文落库、展示与日志脱敏——`Mobile`/`Email`/`ChineseName` 等 PII DP 必须继承
`SensitiveValue` 基类，toString 脱敏由类型系统编译期强制）、 **Encrypted**（可逆密文）、 **Hashed**（不可逆哈希）、 **Masked**
（已脱敏值落库作展示形态）。加密/哈希族为类型擦除模型——变换即丢失原始类型信息，代表类型 `Ciphertext`/`PasswordHash`/`Digest`
与场景端口见 [ADR-0033](0033-crypto-hash-type-redesign.md)；脱敏族为 `MaskedXxx`
记录（掩码算法单一事实源）见 [ADR-0032](0032-masked-value-dps-and-naming.md)。`SecretValue`（瞬态凭证载体，永不展示、无
`value()` 序列化）与敏感基类刻意不合并——同一防御栈的互补层级，并入会使序列化输出明文凭证。

## Consequences

- DP 实现形态、工厂与校验规则单源于 [dp-conventions §2.4 敏感数据族](../dp-conventions.md#24-敏感数据族)；本 ADR
  只定分类与基类边界，不重复正文
- 遮蔽义务判定（字面值是否为攻击素材）见 [ADR-0033](0033-crypto-hash-type-redesign.md) 解耦律：明文 PII 与 PHC 串继承
  `SensitiveValue`，密文/摘要/掩码产物无遮蔽义务
