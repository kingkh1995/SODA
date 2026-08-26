---
type: Decision Record
title: 加密族与哈希族类型设计
description: 设计加密族或哈希族 DP 及场景端口时读：加密/哈希为类型擦除变换，Ciphertext/PasswordHash/Digest 三类型 + 场景端口，算法词不出公共签名，入参必须为 DP。
tags: [dp, data-protection, crypto, hash]
status: stable
---

# 0033 — 加密族与哈希族类型设计

加密/哈希是类型擦除变换——原始类型信息在变换时丢失、不可从字面值验证，名义子类型承诺了验证不了的知识，故加密族唯一代表
`Ciphertext`（JWE compact 五段式自验证，解密由调用方提供目标类型），哈希族 `PasswordHash`（字面值即离线爆破素材——哈希族唯一
`SensitiveValue` 特例）与 `Digest`（恰好 32 字节、小写 hex 唯一线上形态的等值指纹）。类型归属判据是「字面值泄露后是否攻击素材」：攻击素材
`extends SensitiveValue` 强制 toString 遮蔽，非攻击素材（无钥惰性密文、单向指纹）为普通字面量 record、无遮蔽义务；域界判据——摘要
**替代**敏感原值参与持久化/查询/验证属哈希族，仅 **伴随**全量内容作地址/去重属内容标识域（零预建，触发条件出现再立）。算法选择不进公共
API——生产走场景端口 `Encryptor`/`Decryptor`（ISP 分离读写侧）、`PasswordHasher`（登录透明升级）、`Digester`（digest/index 双意图同产
32 字节摘要）；方法名=场景意图、类型名=能力契约，算法词不出现在任何公共签名，签名纪律：入参必须为 DP（禁 String/基本类型）。

## Consequences

- bcrypt 72 字节输入守卫只拦 `hash` 不拦 `verify`——存量截断哈希靠截断对称性照常匹配，verify
  侧拒绝只会锁门而无安全增益；上限属算法实现细节，不入端口契约
- 格式契约与端口方法表见 [conventions/framework-type-contracts.md](../conventions/framework-type-contracts.md)
  ；防护模式总分类见 [ADR-0030](0030-data-protection-dp-categories.md)
  ，脱敏族见 [ADR-0032](0032-masked-value-dps-and-naming.md)
