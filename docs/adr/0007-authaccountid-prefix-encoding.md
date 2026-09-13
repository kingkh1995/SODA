---
type: Decision Record
title: AuthAccountId 前缀编码
description: 设计标识符编码或认证账户 ID 时读——AuthAccountId 用「{类型短名}:{业务键}」自描述编码把类型信息嵌入单字符串，落库与 wire 为同一值形态；反序列化入口下放各子类。
tags: [ auth-account, identifier ]
status: stable
---

# 0007 — AuthAccountId 前缀编码

AuthAccountId 的自描述编码「{AuthAccountType 短名}:{业务键}」（如 "P:42"）把类型信息嵌入单字符串，无需鉴别字段或包装对象即可
自证子类型——落库形态与 wire 形态同一，编码格式单源在本 ADR 与各类型 javadoc。反序列化以各子类的 `of(String)`（
`@JsonCreator`
挂具体子类）为入口，sealed 基类不声明 creator：`@JsonValue` 继承自字面量家族接口（见 ADR-0028 字面量类型家族），声明类型为基类的
JSON 边界不存在消费者。入口归属的一般判据＝ **边界声明类型**所在处（见
[dp-json-conventions §1 模式总表](../conventions/dp-json-conventions.md#1-模式总表)）；本族各边界均声明具体子类，故基类零入口。

前缀由基类从判别值渲染（子类提供 `accountType()` 与 payload 的规范串；不再各自声明 `ACCOUNT_TYPE` / `PREFIX` 常量），
**规范串始终由工厂从 payload 重新渲染**——线形态入口的入参原文只用于解析、永不直接入值。否则上游若送入同一 key 的
另一种写法（`"P:042"` 之于 `"P:42"`），同一逻辑账户会得到两个不相等的规范串身份；判别值恒在该实例的规范串开头，故
入参类型不匹配（以 `"S:…"` 构造密码账户）仍由入口守卫拒绝。
