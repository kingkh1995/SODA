---
type: Decision Record
title: Verification 双概念模型：不透明 source 与多态 recipient
description: 设计 Verification source/recipient 模型或发码端点时读：Verification 只感知不透明 source 与多态密封 recipient，通道判别栖身 recipient，码形策略由调用方按场景选择。
tags: [verification, dp, modeling]
status: stable
---

# 0026 — Verification 双概念模型：不透明 source 与多态 recipient

聚合字段为两个双概念值对象：source（{scene, subject} 纯字符串、非空即足——subject 是槽位占用者裸键串：UCC/ULG/UPR＝userId、URG＝投递端点值）与
recipient（密封多态 DP——channel 派生＋类型化地址 Mobile/Email，通道判别只栖身此处，持久化 channel/target 双列）。Verification
对 source 零解析、领域内零 channel 行为；scene 是调用方词汇（UserVerificationScene，方法即场景、不进 api 命令）；码形策略为
create 必传参数，由调用方工厂按用例场景选择（UCC 双通道统一纯数字短时效）。预认证/无资源场景的发码端点用集合级自定义方法（如
POST /api/users:requestRegisterCode），认证态换绑维持资源级端点。Verification 为独立聚合根与协助方、发送走 AFTER_COMMIT 事件，见
ADR-0021。

## Consequences

- recipient 泛型边界 T extends StringLiteralType：基础设施直接 target ().value () 落裸串免判别，restore 经双参工厂按通道枚举分派
- 未来 ULG/UPR 的 subject 由 findByMobile/findByEmail 反查推导（存在性校验即 subject 推导）；URG 的 subject 即端点值
