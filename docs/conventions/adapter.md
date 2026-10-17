---
type: Convention
title: Adapter 转换器约定 — WebAssembler 与 MapStruct
description: 写 WebAssembler 协议转换器（转换方向/方法命名/使用约定/适用边界）时读；写 MapStruct 映射约定时按指针去 mapstruct.md。仅协议转换用 MapStruct，infrastructure convertor 手写。本文只收契约语义，MapStruct 机制规则与四条定案单源在 conventions/mapstruct.md。
tags: [ convention, adapter, mapstruct ]
status: stable
---

# Adapter 转换器约定

> 指针：MapStruct 机制规则与四条定案 **单源**在 [mapstruct.md](mapstruct.md)；adapter
> 层测试映射（分层归属）见 [test-conventions](../test-conventions.md) §2.3。本文只收转换器的 **契约语义**（转换什么、怎么命名、怎么用）。

**适用范围（何时读 / 何时不读）**：写或评审 `XxxWebAssembler`（转换方向 / 方法命名 / 使用约定 / 适用边界）时读；MapStruct 机制（`@Mapper`
形态、注入、映射语义、构建接线、null 缺口）→ [mapstruct.md](mapstruct.md)；端点与字段命名 →
[aip-api-conventions.md](aip-api-conventions.md)；持久化 convertor 手写规则 →
[framework-type-contracts.md](framework-type-contracts.md)「Convertor（基础设施）」；测试分层与写法 →
[test-conventions.md](../test-conventions.md)。

**级别与来源**：§1 的方向 / 命名 / 使用约定为【强制】·`SODA-POLICY`（项目定案，取舍记为 `SODA-DECISION`：
ADR-0009、ADR-0021、ADR-0024）；§2 的 MapStruct 机制规则为披露指针，正文单源在 [mapstruct.md](mapstruct.md)；§3 只是指针，不承载规则正文。

## 写清单

新增或修改 assembler 时按动作序逐条执行（括号内为落点）：

| #  | 动作                                                                                                                                        |
|----|---------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | 判方向：只做入站 `Request → Command` 与出站 `DTO → Response` 两类转换，不越层（§1）                                                          |
| 2  | 校验留边界：Request 承载 JSR 380 注解 + `@Valid`，Command 不带校验注解（§1，ADR-0009）                                                       |
| 3  | 命名：入站 `to{Action}Command`、出站 `toResponse` / `toResponseList`（§1）                                                                    |
| 4  | 同参数个数的方法名在 assembler 内全不同（§1 完成判据）                                                                                      |
| 5  | 隐式参数（scene、channel）进方法名或方法参数、不进 Request；路径参数走 URL、不进 Request body（§1，ADR-0009 / ADR-0021）                      |
| 6  | 非 1:1 字段复制的转换（分页包装、多实体聚合）由方法签名承载（§1）                                                                            |
| 7  | MapStruct 机制按指针去 mapstruct.md，本文件不写机制规则（§2）                                                                                |
| 8  | 完成判据：命名判据逐条 ＋ 单测经 `Mappers.getMapper(...)` 取实现 ＋ 文末检视清单无未决项（测试分层见 [test-conventions](../test-conventions.md) §2.3） |

## 1. WebAssembler（协议转换器）

**识别行**：`XxxWebAssembler`（每个业务模块一个）· `com.soda.xxx.web.assembler` 子包 · MapStruct `@Mapper` 接口。

**方向**（只有两类转换，不越层）：

| 方向 | 转换                | 语义                                                                                                                                                     |
|------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 入站 | `Request → Command` | HTTP 协议形状 → 应用层写命令。校验留在边界：Request 承载 JSR 380 注解 + `@Valid`（协议边界拒绝），Command 不带校验注解（领域校验在 DP 构造器，ADR-0009） |
| 出站 | `DTO → Response`    | 应用层出参 → HTTP 响应体形状（裸资源，无统一信封）                                                                                                       |

**命名**（完成判据：每个 assembler 内 **同参数个数的方法名必须全不同**，消重载歧义）：

- 入站方法 `to{Action}Command`（如 `toCreateCommand`、`toChangeMobileCommand`）；语义型变体用动作/通道限定（如
  `toRequestChangeMobileCommand` / `toRequestChangeEmailCommand`，ADR-0021）
- 出站方法 `toResponse`（单体）/ `toResponseList`（集合）
- 由方法语义决定、非 Request 数据的隐式参数（scene、channel）进方法名或方法参数，不进 Request（ADR-0021）

**使用约定**：

- Spring Bean 注入 Controller（`componentModel` 的常量形态与装配规则单源见 [mapstruct.md](mapstruct.md) §2）；Controller
  只注入 assembler 与 api 层接口
- 转换不是 1:1 字段复制：分页包装、多实体聚合、路径参数并入命令（`userId` 走 URL 路径，不进 Request body，ADR-0009）由方法签名承载
- 可直接实例化单测——测试中经 `Mappers.getMapper(...)` 获取实现；不在 Request/Response 上定义静态转换方法

## 2. MapStruct 用法（披露指针）

本文不重复 MapStruct 机制规则与四条定案（单源见上方指针 / [mapstruct.md](mapstruct.md)）。

## 3. 关联

- [ADR-0009](../adr/0009-adapter-web-req-resp-pattern.md)（Request/Response + Assembler 模式）、[ADR-0021](../adr/0021-verification-aggregate-root-and-topology.md)（per-use-case 命令与命名消歧）、[ADR-0024](../adr/0024-gateway-save-unified-routing.md)（持久化 convertor 手写的边界理由）
- 端点命名（`requestChange*Code` 介词偏离 / 自定义方法 RPC 名消歧）见 [aip-api-conventions.md](aip-api-conventions.md) §3.7
- [mapstruct.md](mapstruct.md)（MapStruct 机制规则单源）、[framework-type-contracts.md](framework-type-contracts.md)「API・Adapter 类型指针」（类型索引）、[test-conventions.md](../test-conventions.md) §2.3（adapter 层测试分层）

## 检视清单

1. 方向：只有 `Request → Command` / `DTO → Response` 两类，无越层转换；校验注解只在 Request、不在 Command（§1）。
2. 命名：入站 `to{Action}Command`、出站 `toResponse`/`toResponseList`；同参数个数方法名全不同；隐式参数与路径参数不进 Request body（§1）。
3. 使用：Controller 只注入 assembler 与 api 层接口；不在 Request/Response 上定义静态转换方法（§1）。
4. 单源：MapStruct 机制规则不在本文出现（单源 mapstruct.md）；本文只保留契约语义（§2、§3）。
5. 来源与级别：规则组标注【强制】·`SODA-POLICY` / `SODA-DECISION`（ADR-0009 / 0021 / 0024），无未登记来源。
6. 链接可达：mapstruct.md、test-conventions §2.3、aip-api-conventions §3.7、ADR-0009/0021/0024 均指向现存目标。
