---
type: Decision Record
title: Adapter 层 Request/Response 与 WebAssembler 模式
description: 写 Web 层 Controller 与请求响应转换时读——入参出参用 XxxRequest/XxxResponse，MapStruct WebAssembler 双向转换，响应裸返回资源本身（无统一信封，错误统一 RFC 9457 ProblemDetail 最小集，状态码承载语义）。
tags: [adapter, mapstruct, rest]
status: stable
---

# 0009 — Adapter 层 Request/Response 与 WebAssembler 模式

adapter 层 HTTP 契约与应用层模型分离——Controller 入参出参用 `XxxRequest` / `XxxResponse`（JSR 380 校验留在 HTTP 边界），经
MapStruct 接口 `XxxWebAssembler` 做 Request→Command、DTO→Response 双向转换， **响应裸返回资源本身（无统一信封；错误统一 RFC
9457
`ProblemDetail` 最小集，见 ADR-0013）**。两者变化频率不同（协议形状随前端变，Command/DTO
随领域变），且转换不是字段直拷——分页包装、路径参数并入命令等需要独立可注入可单测的转换器；转换器契约细节单源见 [conventions/adapter.md](../conventions/adapter.md)。

## Consequences

- Controller 只做协议映射不碰业务逻辑；代价是每个 HTTP 操作伴随三个新件（Request、Response、assembler 方法）的样板成本。
