---
type: Convention
title: Adapter 转换器约定 — WebAssembler 与 MapStruct
description: 写 WebAssembler 协议转换器（转换方向/方法命名/使用约定/适用边界）时读；写 MapStruct 映射约定时按指针去 STYLEGUIDE §3.4。仅协议转换用 MapStruct，infrastructure convertor 手写。本文只收契约语义，注解速查与四条定案单源在 STYLEGUIDE §3.4。
tags: [ convention, adapter, mapstruct ]
status: stable
---

# Adapter 转换器约定

> 指针：MapStruct 注解用法速查表与四条定案 **单源**在根 [STYLEGUIDE](../../STYLEGUIDE.md) §3.4；adapter
> 层测试映射见 [test-conventions](../test-conventions.md) §2。本文只收转换器的 **契约语义**（转换什么、怎么命名、怎么用）。

## 1. WebAssembler（协议转换器）

**识别行**：`XxxWebAssembler`（每个业务模块一个）· `com.soda.xxx.web.assembler` 子包 · MapStruct `@Mapper` 接口。

**方向**（只有两类转换，不越层）：

| 方向 | 转换                | 语义                                                                                                                                                     |
|------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 入站 | `Request → Command` | HTTP 协议形状 → 应用层写命令。校验留在边界：Request 承载 JSR 380 注解 + `@Valid`（协议边界拒绝），Command 不带校验注解（领域校验在 DP 构造器，ADR-0009） |
| 出站 | `DTO → Response`    | 应用层出参 → HTTP `data` 段形状（不含 Result 信封）                                                                                                      |

**命名**（完成判据：每个 assembler 内 **同参数个数的方法名必须全不同**，消重载歧义）：

- 入站方法 `to{Action}Command`（如 `toCreateCommand`、`toChangeMobileCommand`）；语义型变体用动作/通道限定（如
  `toRequestChangeMobileCodeCommand` / `toRequestChangeEmailCodeCommand`，ADR-0021）
- 出站方法 `toResponse`（单体）/ `toResponseList`（集合）
- 由方法语义决定、非 Request 数据的隐式参数（scene、channel）进方法名或方法参数，不进 Request（ADR-0021）

**使用约定**：

- Spring Bean 注入 Controller（`componentModel = "spring"`）；Controller 只注入 assembler 与 api 层接口
- 转换不是 1:1 字段复制：分页包装、多实体聚合、路径参数并入命令（`userId` 走 URL 路径，不进 Request body，ADR-0009）由方法签名承载
- 可直接实例化单测——测试中经 `Mappers.getMapper(...)` 获取实现；不在 Request/Response 上定义静态转换方法、不写多个
  `toCommand`/`toResponse` 重载（被否方案，见 ADR-0009 Adapter 层 Request/Response 与 WebAssembler 模式）

## 2. MapStruct 用法（披露指针）

本文不重复 MapStruct 注解用法与四条定案（单源见上方指针 / 根 [STYLEGUIDE](../../STYLEGUIDE.md) §3.4）。

## 3. 关联

- ADR-0009（Adapter-Web Request/Response + Assembler 模式）、ADR-0021（API 契约与命名消歧）、ADR-0024（持久化 convertor
  手写的边界理由）
- [STYLEGUIDE](../../STYLEGUIDE.md) §3.4（MapStruct
  注解用法速查）、[framework-type-contracts.md](../conventions/framework-type-contracts.md)「API・Adapter 类型指针」（类型索引）
