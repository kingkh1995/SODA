---
type: Decision Record
title: 模块架构：读写分离的业务模块分层
description: 设计业务模块分层时读——写侧 COLA 四层 DDD 经 start 模块聚合部署，读侧 query-server 混装，两侧共用 api 模块，结构不对称是有意 CQRS 权衡。
tags: [modules, ddd, cqrs]
status: stable
---

# 0001 — 模块架构：读写分离的业务模块分层

业务模块按读写分离组织——写侧跑完整 DDD 分层（adapter / application / domain / infrastructure 四个子模块，`soda-xxx-start`
作启动与部署聚合入口），读侧用 yudao 风格混装的 `soda-xxx-query-server`（Controller + Service + DAO，不分层），两侧共用
`soda-xxx-api` 承载 DTO、Command、Query 与应用服务接口；框架共享代码归 `soda-components`
，业务子模块平铺在根模块下不设中间聚合层。写侧要完整领域模型承载业务规则，读侧简单直接即可，读写两侧结构不对称是有意的 CQRS
权衡；跨层引用方向（如 adapter 只 import api、application 实现仅运行时注入）由 Spring Modulith 在测试期强制。
