---
type: Decision Record
title: AuthAccountId 前缀编码与集中反序列化
description: 设计标识符编码与 JSON 反序列化时读——AuthAccountId 用「{类型短名}:{业务键}」自描述编码，反序列化集中在 sealed 基类 @JsonCreator 按前缀路由。
tags: [auth-account, identifier, json]
status: stable
---

# 0007 — AuthAccountId 前缀编码与集中反序列化

AuthAccountId 的自描述编码「{AuthAccountType 短名}:{业务键}」（如 "P:42"）把类型信息嵌入单字符串，无需鉴别字段或包装对象即可自证子类型。JSON
反序列化集中在 sealed 基类的 @JsonCreator (DELEGATING) of (String)：按前缀拆解、switch 路由到各子类的 of (String)
——类型注册只此一处，sealed switch 的穷尽性使新增子类在编译期破坏分派，不依赖逐子类注册反序列化器。
