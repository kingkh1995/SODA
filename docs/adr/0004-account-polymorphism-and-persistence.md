---
type: Decision Record
title: AuthAccount 多态与单表持久化
description: 设计 AuthAccount 多态建模或决定认证方式持久化策略时读——User 聚合下 sealed 多态子实体；持久化单表化：password_hash 列承载密码账户，Sms/Email 账户由 mobile/email 列派生。
tags: [user, auth-account, persistence]
status: stable
---

# 0004 — AuthAccount 多态与单表持久化

认证方式在领域层建模为 User 聚合下的多态子实体 AuthAccount（sealed 层级见 [0006](0006-sealed-authaccount-hierarchy.md)
）：PasswordAuthAccount 是 User 构造器必填的独立字段——「一个 User 必有一个密码账户」不变量由类型系统表达，无密码账户的 User
不可表示；accounts 列表仅存可选账户（Sms / Email / Social），拒绝密码账户入列。持久化单表化：user 单表以 password_hash
列承载密码账户，Sms / Email 账户不建表、恢复时由 mobile / email 列非空派生身份，active 由 sms_login_enabled /
email_login_enabled 开关列表达（false 仅关闭该渠道登录，账号标识保留）。领域多态与持久化扁平化分离——convertor
按列直接构造、无鉴别器分发；新增认证方式只需新增子类，其持久化形态届时按需设计。

## Consequences

- PasswordAuthAccount 恒启用不变量：deactivate 恒抛 UnsupportedOperationException、恢复路径拒绝 Active.FALSE，持久化无
  active 列
- 派生组装由 convertor 承担，恢复路径需随账户字段演进同步维护
