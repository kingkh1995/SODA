---
type: Decision Record
title: 实体相等性 — 引用相等，身份判断经 getId()
description: 设计或评审领域实体（Entity / Aggregate / AuthAccount）的相等性、集合语义或身份判断时读——Entity 族一律不重写 equals/hashCode（继承 Object 引用相等），实体不具备可比较性、不进 Set/Map 键，跨实例身份判断一律经 getId() 显式表达；值对象（DP / Identifier）反之必须值相等。
tags: [ entity, ddd, equals, hashcode, identity ]
status: stable
---

# 0040 — 实体相等性：引用相等，身份判断经 `getId()`

领域 **实体**（`Entity` / `Aggregate` / `AuthAccount` 族）一律 **不重写** `equals`/`hashCode`，保持 `Object`
的引用相等；需要「是否同一逻辑实体」等跨实例身份判断时，一律经 `getId()` **显式**表达。 **值对象**（DP / `Identifier` 族 /
字面量家族）相反：其相等性必须基于值。

## 为什么

Evans《Domain-Driven Design》 (2003) 定义实体为「贯穿时间的身份线」，引用相等只回答「是否同一对象引用」，不回答「是否同一实体」——后者必须经
`getId()` 显式表达。Vernon (2013) 主张基于 identity 重写 equals 的论证前提是「实体进入 `java.util.Set`」，本仓实测实体零进
hash 集合，不满足其前提。Jakarta Persistence 3.2 §4.6.14 定义实体相等为「同主键值」， **实体类无重写义务**；Hibernate ORM User
Guide §3.4.7 明确说 *"you may want to consider not implementing equals/hashCode altogether"*。

一手取证全集（含反方与证据缺口）见 [research/entity-equality-in-ddd.md](../research/entity-equality-in-ddd.md)
；实现/测试迁移细节见 [conventions/lombok.md](../conventions/lombok.md) §5
与 [framework-type-contracts.md](../conventions/framework-type-contracts.md) Entity 段。

## Consequences

- 跨实例身份判断唯一合法形式：`userA.getId().equals(userB.getId())`；实体不得用作 `Set` 成员或 `Map` 键。
- 值对象不受影响：DP / `Identifier` / 字面量家族保留值相等（`@EqualsAndHashCode` 照旧）。
- 前置条件：① 实体不进 hash 集合；② 每处「同一逻辑实体」判定按 id 表达；③ 不引入 JPA 实体集合关联（`@OneToMany` 等）——否则与本决定冲突。
- 移除须整批：`Entity` 与子类同批移除 `equals`/`hashCode`，避免 Lombok `callSuper=true` 指向已移除父类的静默退化。
