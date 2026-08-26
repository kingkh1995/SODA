---
type: Decision Record
title: Verification 类型的模块位置
description: 决定 Verification 类型模块位置时读：聚合与值对象整体位于 soda-user-domain，跨域抽离待第二消费方出现时纯搬移。
tags: [verification, module, modularity]
status: stable
---

# 0029 — Verification 类型的模块位置

Verification 聚合及其值对象（`VerificationSource`/`VerificationRecipient`/`UserVerificationScene` 等）、gateway 端口与事件整体位于
soda-user-domain 模块（com.soda.user.domain 及子包）——当前仅服务用户域用例，下沉独立模块或公共组件包待第二个消费方出现再做（YAGNI）。聚合已零用户概念依赖（source
纯字符串、recipient 用组件层地址 DP），届时抽离是纯包搬移、soda-user 仅替换引用。
