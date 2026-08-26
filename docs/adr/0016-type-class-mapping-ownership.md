---
type: Decision Record
title: 判别值映射归属（type↔class）
description: 设计 type↔class 映射或多态分派时读——固定种类集用 sealed class、调用方消费 class，判别枚举只作数据形态词汇不持 Class 引用，映射归基础设施。
tags: [sealed, enum, polymorphism]
status: stable
---

# 0016 — 判别值映射归属（type↔class）

固定种类集（如 `AuthAccount` 四子类）用 sealed class 层次表达，行为差异留在子类；判别值由领域短名枚举承载，但枚举只是「数据形态」词汇——边界值、JSON
判别串、ID 前缀，不持 `Class` 引用、不提供反查，class↔判别值映射集中发生在基础设施层，domain 内不存在注册表。调用方（domain/app）一律消费
class：构造、按类型查询、分派、领域方法传参；domain/app 禁止显式强转，类型收敛靠方法签名编译期契约与模式匹配，桥接强转只允许存在于
gateway 实现内部一处。JSON 判别走 `@JsonTypeName` + Jackson 3 从 `permits` 子句自动发现（无 `@JsonSubTypes`
）；判别值三方一致（permits 完备、判别串唯一、与枚举 name 一致）由一致性测试锁定。

## Consequences

-
何时用类、何时用枚举的完整分派规则见 [conventions/framework-type-contracts.md](../conventions/framework-type-contracts.md)
「多态实体分派」。
