---
type: Decision Record
title: 注销终态键释放与归档审计
description: 处理注销终态键释放或归档审计时读：D→R 同事务归档原键快照并三键置 NULL，领域零感知；终态行守卫按 StateEnumType.terminal() 泛化。
tags: [user, deregister, persistence, archive]
status: stable
---

# 0023 — 注销终态键释放与归档审计

注销（D→R）后 username/mobile/email 必须可再注册：gateway save 终态分支同事务将原键快照写入 `user_archive` 并把三键置
NULL——NULL 不参与 MySQL 唯一索引，`uk_*` 原样保留、查询零改动、键即时释放（终态保留行承接 ADR-0017 的表示自由度，归档写入走
ADR-0024 的 save 终态处理）。恢复路径为空 username 补内存默认值 `Username.REMOVED`（从不落库）；更新路径以持久化行状态做终态行不可写守卫，谓词泛化为
`StateEnumType.terminal()`——新增终态只需枚举成员标记（UserState R 与 VerificationState U 共用契约）。

## Consequences

- 归档表 insert-only 纯审计存储，原键仅经合规审计通道访问；保存期限届满的清除 job 为演进路径、本期未建
- 键释放动机含注销后 PII 删除/匿名化合规（个保法第 47 条）；行业实证键释放为主流（微博/抖音/B站/Telegram），Google 邮箱为反例
