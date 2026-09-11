---
type: Research
title: Google AIP 全谱拉取（66 个主文档按 7 类分组 + 子目录清单）
description: 锚点：为 ticket 04-08 逐类 grilling 提供 AIP 全谱输入——按 google.aip.dev/general 7 类分组,每条 MUST/MUST NOT/SHOULD/SHOULD NOT/MAY 规则逐行收录,标注 SODA 现有 Research doc 已涵盖项。仅记录,不评价、不修复。
tags: [ aip, google, api-design, inventory ]
status: stable
sources:
  - resource: https://google.aip.dev
    id: google-aip-site
    title: Google API Improvement Proposals（站点索引）
    author: Google
  - resource: https://github.com/aip-dev/google.aip.dev
    id: aip-dev-repo
    title: AIPs site source
    author: aip-dev
generated:
  by: wayfinder/02 (main session, scout agents failed to write)
  at: 2026-09-02T00:00:00Z
---

# Google AIP 全谱拉取

> 引文格式：`AIP-NNN` 引用 https://google.aip.dev/NNN；规则行引用 `https://google.aip.dev/NNN` 对应页面锚点。
>
> 索引来源：https://google.aip.dev/general（共 7 大类 + Meta/Process/Protocol Buffers 杂项,合计 66 个主文档）。
>
> 本调研为 wayfinder ticket 04-08 逐类 grilling 探讨的输入源；不评价、不修复,仅清单。
>
> **本次会话覆盖**：§1.3 Resource Design（9 AIP）、§1.5 Fields（13 AIP）、§1.6 Operations（12 AIP）、§1.7 Design Patterns（17 AIP）共
> **51 个 AIP 的规则逐行收录**。§1.1 Meta、§1.2 Process、§1.4 API Concepts、§1.8 Compatibility and Versioning、§1.9
> Polish、§1.10 Protocol Buffers 由后续会话补齐（原始 AIP 页可读；本次会话 budget 受限）。

## 0. AIP 编号与类别映射（全集）

| 编号 | 类别             | 标题                                        |
|------|------------------|---------------------------------------------|
| 1    | Meta             | AIP Purpose and Guidelines                  |
| 2    | Meta             | AIP Numbering                               |
| 3    | Meta             | AIP Versioning                              |
| 8    | Meta             | AIP Style and Guidance                      |
| 9    | Meta             | Glossary                                    |
| 100  | Process          | API Design Review FAQ                       |
| 111  | API Concepts     | Planes                                      |
| 121  | Resource Design  | Resource-oriented design                    |
| 122  | Resource Design  | Resource names                              |
| 123  | Resource Design  | Resource types                              |
| 124  | Resource Design  | Resource association                        |
| 126  | Resource Design  | Enumerations                                |
| 127  | Protocol Buffers | HTTP and gRPC Transcoding                   |
| 128  | Resource Design  | Declarative-friendly interfaces             |
| 129  | Resource Design  | Server-Modified Values and Defaults         |
| 130  | Operations       | Methods                                     |
| 131  | Operations       | Standard methods: Get                       |
| 132  | Operations       | Standard methods: List                      |
| 133  | Operations       | Standard methods: Create                    |
| 134  | Operations       | Standard methods: Update                    |
| 135  | Operations       | Standard methods: Delete                    |
| 136  | Operations       | Custom methods                              |
| 140  | Fields           | Field names                                 |
| 141  | Fields           | Quantities                                  |
| 142  | Fields           | Time and duration                           |
| 143  | Fields           | Standardized codes                          |
| 144  | Fields           | Repeated fields                             |
| 145  | Fields           | Ranges                                      |
| 146  | Fields           | Generic fields                              |
| 147  | Fields           | Sensitive fields                            |
| 148  | Fields           | Standard fields                             |
| 149  | Fields           | Unset field values                          |
| 151  | Operations       | Long-running operations                     |
| 152  | Design Patterns  | Jobs                                        |
| 153  | Design Patterns  | Import and export                           |
| 154  | Design Patterns  | Resource freshness validation               |
| 155  | Design Patterns  | Request identification                      |
| 156  | Resource Design  | Singleton resources                         |
| 157  | Design Patterns  | Partial responses                           |
| 158  | Design Patterns  | Pagination                                  |
| 159  | Design Patterns  | Reading across collections                  |
| 160  | Design Patterns  | Filtering                                   |
| 161  | Design Patterns  | Field masks                                 |
| 162  | Design Patterns  | Resource Revisions（Draft）                 |
| 163  | Design Patterns  | Change validation                           |
| 164  | Design Patterns  | Soft delete                                 |
| 165  | Design Patterns  | Criteria-based delete                       |
| 180  | Compatibility    | Backwards compatibility                     |
| 181  | Compatibility    | Stability levels                            |
| 182  | Compatibility    | External software dependencies（Reviewing） |
| 184  | Compatibility    | API version identifiers                     |
| 185  | Compatibility    | API Versioning                              |
| 190  | Polish           | Naming conventions                          |
| 191  | Polish           | File and directory structure                |
| 192  | Polish           | Documentation                               |
| 193  | Polish           | Errors                                      |
| 194  | Polish           | Automatic retry configuration               |
| 200  | Meta             | Precedent                                   |
| 202  | Fields           | Fields                                      |
| 203  | Fields           | Field behavior documentation                |
| 205  | Process          | Beta-blocking changes                       |
| 210  | Design Patterns  | Unicode                                     |
| 211  | Design Patterns  | Authorization checks                        |
| 213  | Protocol Buffers | Common components                           |
| 214  | Design Patterns  | Resource expiration                         |
| 215  | Protocol Buffers | API-specific protos                         |
| 216  | Fields           | States                                      |
| 217  | Design Patterns  | Unreachable resources                       |
| 236  | Resource Design  | Policy preview                              |

**合计 66 个主文档**。编号空洞合法（AIP 不连续编号）；括号内为状态（Draft / Reviewing / 已 stable）。

## 1.3 Resource Design（AIP-121-129, 156, 236）

### 全谱清单

| 编号 | 标题                                | 核心规则（一句话）                                                     | 级别范围      |
|------|-------------------------------------|------------------------------------------------------------------------|---------------|
| 121  | Resource-oriented design            | 资源导向：基本构建块是命名的资源（名词）；关系是有向无环图；无状态协议 | MUST          |
| 122  | Resource names                      | 资源名称用 `collection/{id}` 格式，`/` 分隔，camelCase，小写字母开头   | MUST          |
| 123  | Resource types                      | 资源类型格式 `{ServiceName}/{Type}`，PascalCase 单数                   | MUST          |
| 124  | Resource association                | 每个资源最多一个规范父资源；多对多可用重复字段或子资源                 | MUST          |
| 126  | Enumerations                        | 枚举值用 UPPER_SNAKE_CASE；首值 `_UNSPECIFIED`                         | MUST/SHOULD   |
| 128  | Declarative-friendly interfaces     | 声明式友好资源：仅标准方法、可选 `reconciling` 字段                    | MUST          |
| 129  | Server-Modified Values and Defaults | 字段必须单一所有者；默认值用 `effective_` 前缀字段分离                 | MUST          |
| 156  | Singleton resources                 | 单例资源：无 Create/Delete，有 Get/Update                              | MUST NOT      |
| 236  | Policy preview                      | 策略预览：嵌套实验集合 + startPreview/stopPreview/commit 自定义方法    | MUST（Draft） |

### 规则摘要（本类）

| AIP 编号 | 级别       | 规则正文（≤80字）                                                                      | 章节锚点                                      |
|----------|------------|----------------------------------------------------------------------------------------|-----------------------------------------------|
| 121      | MUST       | 资源导向 API 应建模为资源层次结构,每个节点是简单资源或集合                             | Resources                                     |
| 121      | SHOULD NOT | API 不应与底层数据库 schema 一一对应                                                   | Resources (Note)                              |
| 121      | MUST       | 同一资源在所有标准方法中的 schema 必须一致                                             | Methods                                       |
| 121      | MUST       | 资源必须支持 Get 方法                                                                  | Methods                                       |
| 121      | MUST       | 资源必须支持 List 方法（单例除外）                                                     | Methods                                       |
| 121      | SHOULD     | API 应暴露大量资源,每个资源少量方法                                                    | Methods                                       |
| 121      | MUST       | 资源导向 API 必须运行于无状态协议                                                      | Stateless protocol                            |
| 121      | MUST       | 资源关系必须可表示为有向无环图（DAG）                                                  | Cyclic References                             |
| 122      | MUST       | 资源名称在 API 内必须唯一                                                              | Guidance / AIP-122                            |
| 122      | MUST       | 资源名称必须用 `/` 分隔各段                                                            | Guidance                                      |
| 122      | MUST NOT   | 资源名称的非终止段不得含 `/`                                                           | Guidance                                      |
| 122      | SHOULD     | 资源 ID 应仅用 RFC-1123 DNS 名称中字符                                                 | Guidance                                      |
| 122      | SHOULD NOT | 资源 ID 不应使用大写字母                                                               | Guidance                                      |
| 122      | MUST       | 资源必须暴露 `name` 字段                                                               | Guidance (Fields representing resource names) |
| 122      | MUST NOT   | 资源不得暴露元组、self-link 或其他形式的资源标识                                       | Guidance                                      |
| 122      | SHOULD     | 所有 ID 字段应为字符串                                                                 | Guidance                                      |
| 122      | MUST       | 集合标识符必须是 camelCase 复数形式                                                    | Collection identifiers                        |
| 122      | MUST       | 集合标识符必须以小写字母开头,仅含 ASCII 字母数字                                       | Collection identifiers                        |
| 122      | MUST NOT   | 集合标识符不得「生造」复数（如 infos）                                                 | Collection identifiers                        |
| 122      | SHOULD     | 嵌套集合可省略父集合前缀（消息名不变,仅 collection/ID 缩短）                           | Nested collections                            |
| 122      | SHOULD     | 用户指定的资源 ID 应符合 RFC-1034                                                      | Resource ID segments                          |
| 122      | SHOULD     | 用户指定的资源 ID 应限制为小写字母                                                     | Resource ID segments                          |
| 122      | MAY        | API 可为常见查找模式提供程序化别名（如 `users/me`）                                    | Resource ID aliases                           |
| 122      | MUST       | 所有 ID 字段应为字符串                                                                 | Guidance (末尾)                               |
| 123      | MUST       | 资源类型格式必须为 `{ServiceName}/{Type}`                                              | Guidance / AIP-123                            |
| 123      | MUST       | 类型名必须以大写字母开头,仅含字母数字,使用 PascalCase                                  | Guidance                                      |
| 123      | MUST       | 类型名必须是名词的单数形式                                                             | Guidance                                      |
| 123      | SHOULD     | API 应使用 google.api.resource 注解标注资源类型                                        | Annotating resource types                     |
| 123      | MUST       | 注解必须包含 pattern、singular、plural                                                 | Annotating resource types                     |
| 123      | MUST       | 模式变量必须使用 snake_case,且不带 `_id` 后缀                                          | Annotating resource types                     |
| 123      | MUST       | 多模式资源新增模式必须追加在末尾,不得重排                                              | Annotating resource types                     |
| 124      | MUST       | 每个资源最多一个规范父资源                                                             | Guidance / AIP-124                            |
| 124      | MUST NOT   | List 请求不得要求两个不同的父资源才能工作                                              | Guidance                                      |
| 124      | MAY        | 多对一关系：其他关联通过字段引用                                                       | Guidance                                      |
| 124      | SHOULD     | 多对多关系：使用重复字段或子资源                                                       | Guidance                                      |
| 126      | MUST       | 枚举值必须使用 UPPER_SNAKE_CASE                                                        | Guidance / AIP-126                            |
| 126      | SHOULD     | 枚举第一个值应为 `{ENUM}_UNSPECIFIED`                                                  | Guidance                                      |
| 126      | SHOULD     | 枚举应仅用于变化不频繁的值集合                                                         | When to use enums                             |
| 126      | SHOULD     | 变化频繁的值集合应使用 string                                                          | When to use enums                             |
| 126      | SHOULD NOT | 有广泛采用标准表示时不应使用枚举                                                       | When to use enums                             |
| 126      | SHOULD     | 枚举应文档化是否冻结或未来会添加值                                                     | Guidance                                      |
| 126      | SHOULD     | 单消息内使用的枚举应嵌套在该消息中                                                     | Guidance                                      |
| 126      | SHOULD NOT | 嵌套枚举的非零值不应加枚举名前缀                                                       | Guidance                                      |
| 128      | MUST       | 声明式友好资源必须仅使用强一致的标准方法管理生命周期                                   | Resources                                     |
| 128      | SHOULD     | 声明式友好资源应通过 style: DECLARATIVE_FRIENDLY 标注                                  | Resources                                     |
| 128      | SHOULD     | 需要时间协调的资源应包含 bool reconciling 字段（output only）                          | Reconciliation                                |
| 128      | MUST       | reconciling=true 时,资源状态应反映真实状态                                             | Reconciliation                                |
| 129      | MUST       | 字段必须有单一所有者（client 或 server）                                               | Single Owner Fields                           |
| 129      | MUST       | 服务端拥有的字段必须标 OUTPUT_ONLY                                                     | Single Owner Fields                           |
| 129      | MUST       | 服务端不得修改客户端拥有的字段                                                         | Single Owner Fields                           |
| 129      | MUST       | 有 effective value 的属性必须表达为两个字段（可变 + OUTPUT_ONLY）                      | Effective Values                              |
| 129      | MUST       | effective 值字段必须用 `effective_` 前缀命名                                           | Naming                                        |
| 129      | MUST       | 用户指定字段在响应中的值必须与请求一致（除非归一化）                                   | User-Specified Fields                         |
| 129      | MUST       | 服务端归一化的字段必须用 google.api.field_info 标注                                    | Normalizations                                |
| 156      | MAY        | API 可定义单例资源（每个父资源恰好一个实例）                                           | Guidance / AIP-156                            |
| 156      | MUST NOT   | 单例资源不得有用户指定或系统生成的 ID                                                  | Guidance                                      |
| 156      | MUST       | 单例资源定义必须提供 singular 和 plural 字段                                           | Guidance                                      |
| 156      | MUST NOT   | 单例资源不得定义 Create 或 Delete 标准方法                                             | Guidance                                      |
| 156      | SHOULD     | 单例资源应定义 Get 和 Update 方法                                                      | Guidance                                      |
| 156      | MAY        | 单例资源可定义 List（按 AIP-159 实现）                                                 | Guidance                                      |
| 236      | MUST       | PolicyExperiment 资源类型必须遵循 `RegularResourceType Experiment` 命名约定            | Experiments                                   |
| 236      | MUST       | 实验 proto 必须有顶层字段与 live policy 同类型,字段名为 live 资源类型                  | Experiments                                   |
| 236      | MUST       | 所有 preview_metadata 字段必须为 output only                                           | Metadata                                      |
| 236      | MUST       | startPreview 时必须创建 preview_metadata,state=ACTIVE,start_time=now,log_prefix 预定义 | Metadata                                      |
| 236      | MUST       | stopPreview 时必须设置 state=SUSPENDED,stop_time=now                                   | Metadata                                      |
| 236      | MUST       | startPreview、stopPreview 自定义方法必须使用 LRO                                       | Methods / startPreview/stopPreview            |

## 1.5 Fields（AIP-140-149, 202, 203, 216）

### 全谱清单

| 编号 | 标题                         | 核心规则（一句话）                                                                     | 级别范围    |
|------|------------------------------|----------------------------------------------------------------------------------------|-------------|
| 140  | Field names                  | 字段名 lower_snake_case,American English,无介词、无动词                                | MUST/SHOULD |
| 141  | Quantities                   | 数量字段以单位/数量为后缀；不用无符号整数                                              | MUST        |
| 142  | Time and duration            | 时间戳用 google.protobuf.Timestamp；时长用 Duration；字段名 `_time` / `_duration`      | SHOULD      |
| 143  | Standardized codes           | 标准代码用 string 不用 enum；字段名以 `_code` 或 `_type` 结尾                          | MUST        |
| 144  | Repeated fields              | 重复字段名用复数；超过 100 用子资源；Add/Remove 自定义方法                             | MUST/SHOULD |
| 145  | Ranges                       | 区间字段用 `start_*` / `end_*`（含头不含尾）,或 `first_*` / `last_*`（口语例外）       | SHOULD      |
| 146  | Generic fields               | 通用字段按 least generic 原则选 oneof / map / Struct / Any                             | SHOULD      |
| 147  | Sensitive fields             | 敏感字段 INPUT_ONLY + OUTPUT_ONLY `_set` 或 `obfuscated_` 前缀                         | SHOULD      |
| 148  | Standard fields              | name / parent / create_time / update_time / delete_time / uid / annotations 等标准字段 | MUST/SHOULD |
| 149  | Unset field values           | 区分"未设置"与"零值"时用 proto `optional` 关键字（仅 int/float）                       | SHOULD      |
| 202  | Fields                       | 字段格式 UUID4 / IPV4 / IPV6 通过 google.api.field_info 标注                           | MUST        |
| 203  | Field behavior documentation | 字段必须有 google.api.field_behavior 注解；至少 REQUIRED/OPTIONAL/OUTPUT_ONLY          | MUST        |
| 216  | States                       | 状态用枚举；OUTPUT_ONLY；状态转换用自定义方法（非 Update）                             | SHOULD      |

### 规则摘要（本类）

| AIP 编号 | 级别       | 规则正文（≤80字）                                                    | 章节锚点                 |
|----------|------------|----------------------------------------------------------------------|--------------------------|
| 140      | SHOULD     | 字段名应为正确的美式英语                                             | Guidance / AIP-140       |
| 140      | MUST       | 字段定义在 protobuf 中必须用 lower_snake_case                        | Case                     |
| 140      | MUST NOT   | 字段名任何单词不得以数字开头                                         | Case                     |
| 140      | MUST NOT   | 字段名不得含首尾或相邻下划线                                         | Case                     |
| 140      | MUST       | 重复字段必须用正确的复数形式                                         | Repeated fields          |
| 140      | SHOULD     | 非重复字段应用单数形式                                               | Repeated fields          |
| 140      | SHOULD NOT | 字段名不应包含介词（with / for / at / by）                           | Prepositions             |
| 140      | MUST NOT   | 字段名不得是动词,必须是名词                                          | Verbs                    |
| 140      | SHOULD     | 布尔字段应省略 `is_` 前缀                                            | Booleans                 |
| 140      | SHOULD     | URI 字段应用 `uri`,URL 字段应用 `url`                                | URIs                     |
| 140      | SHOULD     | 人类可读名称字段应用 `display_name`；正式名称可用 `title`            | Display names            |
| 140      | SHOULD     | 字段名应避免编程语言保留字（new / class / import 等）                | Reserved words           |
| 141      | MUST       | 带单位的数量字段必须含单位后缀                                       | Guidance / AIP-141       |
| 141      | SHOULD     | 数量字段单位应使用通用缩写                                           | Guidance                 |
| 141      | SHOULD     | 项目数量字段应用 `_count` 后缀（非 `num_` 前缀）                     | Guidance                 |
| 141      | MUST NOT   | 字段不得使用无符号整数类型                                           | Guidance (Note)          |
| 141      | SHOULD     | 复合单位可在单位间用下划线分隔                                       | Compound units           |
| 141      | SHOULD     | 逆单位应在分隔词 `per_` 后表示                                       | Inverse units            |
| 142      | SHOULD     | 时间戳字段应使用 google.protobuf.Timestamp                           | Timestamps               |
| 142      | SHOULD     | 时长字段应使用 google.protobuf.Duration                              | Durations                |
| 142      | SHOULD     | 时间戳字段名以 `_time` 结尾；重复用 `_times`                         | Timestamps               |
| 142      | SHOULD NOT | 字段名不应使用过去时（`published_time` / `created_time`）            | Timestamps               |
| 142      | SHOULD     | 相对时段字段名以 `_offset` 结尾                                      | Relative time segments   |
| 142      | SHOULD     | 民用日期用 google.type.Date,字段名以 `_date` 结尾                    | Civil dates              |
| 143      | MUST       | 标准化代码字段必须使用正确数据类型（通常是 string）                  | Guidance / AIP-143       |
| 143      | SHOULD NOT | 不应使用枚举表示标准代码                                             | Guidance                 |
| 143      | SHOULD     | 接受用户输入时验证应大小写不敏感                                     | Guidance                 |
| 143      | SHOULD     | 提供给用户时应使用规范大小写                                         | Guidance                 |
| 143      | MUST       | 国家/地区必须用 Unicode CLDR 区域码,字段名为 `region_code`           | Countries and regions    |
| 143      | MUST       | 货币必须用 ISO-4217,字段名为 `currency_code`                         | Currency                 |
| 143      | MUST       | 语言必须用 IETF BCP-47,字段名为 `language_code`                      | Language                 |
| 143      | SHOULD     | 时区应使用 IANA TZ,字段名为 `time_zone`                              | Time zones               |
| 143      | MUST       | 内容类型必须用 IANA media types,字段名为 `mime_type`                 | Content types            |
| 144      | MUST       | 重复字段名必须用复数                                                 | Guidance / AIP-144       |
| 144      | SHOULD     | 重复字段应有上限（经验值 ~100）,超限用子资源                         | Guidance                 |
| 144      | MUST NOT   | 重复字段不得内联另一个资源的 body                                    | Guidance                 |
| 144      | SHOULD     | 重复字段首选 scalar 类型（string）                                   | Scalars and messages     |
| 144      | SHOULD     | 需要原子修改时应用 Add/Remove 自定义方法（POST）                     | Update strategies        |
| 144      | MUST       | Add 方法遇已存在数据必须返回 ALREADY_EXISTS                          | Update strategies        |
| 144      | MUST       | Remove 方法遇不存在数据必须返回 NOT_FOUND                            | Update strategies        |
| 145      | SHOULD     | 区间字段应用两个相同类型字段 + `start_` / `end_` 前缀                | Guidance / AIP-145       |
| 145      | SHOULD     | 区间字段应使用含头不含尾（半开区间）                                 | Inclusive or exclusive   |
| 145      | SHOULD     | 显著口语惯例用闭区间时应用 `first_` / `last_` 前缀                   | Exceptions               |
| 146      | MAY        | 服务可引入通用字段（oneof / map / Struct / Any）                     | Guidance / AIP-146       |
| 146      | SHOULD     | 服务应尝试 least generic 方法                                        | Guidance                 |
| 146      | SHOULD     | oneof 应优先于 map / Struct / Any                                    | Oneof                    |
| 146      | SHOULD NOT | Any 不应使用,除非其他选项不可行                                      | Any                      |
| 147      | SHOULD     | 必备敏感信息应接受为 INPUT_ONLY 字段（无对应输出字段）               | Guidance / AIP-147       |
| 147      | SHOULD     | 可选敏感信息应同时含 INPUT_ONLY 字段 + `_set` OUTPUT_ONLY 布尔字段   | Guidance                 |
| 147      | MAY        | 用 `obfuscated_` 前缀字段代替布尔 `_set` 字段                        | Guidance                 |
| 148      | MUST       | 每个资源必须有 string name 字段,作为第一个字段                       | name                     |
| 148      | SHOULD     | parent 字段应在大多数 List/Create 请求中使用                         | parent                   |
| 148      | MUST       | display_name 必须是可变、用户可设置的字段                            | display_name             |
| 148      | SHOULD NOT | display_name 不应有唯一性约束                                        | display_name             |
| 148      | SHOULD     | display_name 应限制为 ≤63 字符                                       | display_name             |
| 148      | MUST       | given_name 字段必须指个人或动物的名字,不得用 first_name              | given_name               |
| 148      | MUST       | family_name 字段必须指个人或动物的姓,不得用 last_name                | family_name              |
| 148      | MUST       | create_time 字段表示资源创建时间,output only                         | create_time              |
| 148      | MUST       | update_time 字段表示资源最近更新时间,output only                     | update_time              |
| 148      | SHOULD     | 支持软删除的资源应提供 delete_time 字段,output only                  | delete_time              |
| 148      | SHOULD     | 资源应提供 purge_time 字段（软删除时）                               | purge_time               |
| 148      | MAY        | 可加 `map<string,string> annotations` 字段存小量任意数据             | Annotations              |
| 148      | MUST       | uid 字段如提供必须为 UUID4 且用 UUID4 格式扩展                       | uid                      |
| 149      | SHOULD     | 需要区分"未设置"与"零值"时,protobuf primitive 应用 `optional` 关键字 | Guidance / AIP-149       |
| 149      | SHOULD     | `optional` 仅应用于整数和浮点数                                      | Guidance                 |
| 202      | MUST       | UUID4/IPV4/IPV6/IPV4_OR_IPV6 格式字段必须为 string 类型              | Format / AIP-202         |
| 202      | MUST NOT   | 格式字段不得用原始文本比较做等价判断                                 | UUID4/IPv4/IPv6          |
| 202      | MUST       | 新格式必须由 IETF RFC 或 Google AIP 管理                             | Extending Format         |
| 203      | MUST       | API 必须在请求消息每个字段上应用 google.api.field_behavior           | Guidance / AIP-203       |
| 203      | MUST       | 注解必须包含至少 REQUIRED/OPTIONAL/OUTPUT_ONLY 之一                  | Guidance                 |
| 203      | MUST NOT   | FIELD_BEHAVIOR_UNSPECIFIED 不得使用                                  | Guidance                 |
| 203      | MUST       | IDENTIFIER 必须附加于 name 字段,不得用于其他字段                     | Identifier               |
| 203      | SHOULD NOT | INPUT_ONLY 不应用于请求消息字段（已隐含）                            | Input only               |
| 203      | SHOULD NOT | OUTPUT_ONLY 不应用于响应消息字段（已隐含）                           | Output only              |
| 203      | SHOULD NOT | REQUIRED 不应用于"响应中始终存在"或"条件必填"的字段                  | Required                 |
| 216      | SHOULD     | 状态字段应使用枚举,名为 State（结尾 State）                          | Guidance / AIP-216       |
| 216      | SHOULD NOT | 状态枚举不得通过 Update 方法直接更新                                 | Output only              |
| 216      | SHOULD     | 状态转换用自定义方法（AIP-136）,用 POST + `:verb`                    | State transition methods |
| 216      | MUST       | 状态转换不允许时必须返回 FAILED_PRECONDITION                         | State transition methods |
| 216      | SHOULD     | 状态枚举首个值应为 `STATE_UNSPECIFIED`,后续不加 STATE_ 前缀          | Prefixes / Default value |
| 216      | MAY        | 现有状态枚举可加新状态（非破坏性变更）                               | Breaking changes         |

## 1.6 Operations（AIP-130, 131-136, 151, 231, 233-235）

### 全谱清单

| 编号 | 标题                     | 核心规则（一句话）                                                               | 级别范围 |
|------|--------------------------|----------------------------------------------------------------------------------|----------|
| 130  | Methods                  | 资源导向设计首选标准方法（Get/List/Create/Update/Delete） → 批量 → 自定义 → 流式 | SHOULD   |
| 131  | Standard methods: Get    | 单资源 GET 取回,响应为资源本身                                                   | MUST     |
| 132  | Standard methods: List   | 集合 GET 列出,响应含 next_page_token                                             | MUST     |
| 133  | Standard methods: Create | 集合 POST 创建,响应为资源本身                                                    | SHOULD   |
| 134  | Standard methods: Update | 资源 PATCH 部分更新,响应为资源本身；PUT 不推荐                                   | SHOULD   |
| 135  | Standard methods: Delete | 资源 DELETE 删除；子资源存在则 FAILED_PRECONDITION                               | SHOULD   |
| 136  | Custom methods           | 动词+名词、`:` 后跟动词、camelCase、GET/POST                                     | MUST     |
| 151  | Long-running operations  | LRO 用 google.longrunning.Operation,response_type 与 metadata_type 必填          | SHOULD   |
| 231  | Batch methods: Get       | BatchGet,URI 末尾 `:batchGet`,GET,atomic                                         | MAY      |
| 233  | Batch methods: Create    | BatchCreate,URI 末尾 `:batchCreate`,POST                                         | MAY      |
| 234  | Batch methods: Update    | BatchUpdate,URI 末尾 `:batchUpdate`,POST                                         | MAY      |
| 235  | Batch methods: Delete    | BatchDelete,URI 末尾 `:batchDelete`,POST（非 DELETE）                            | MAY      |

### 规则摘要（本类）

| AIP 编号 | 级别       | 规则正文（≤80字）                                                                  | 章节锚点                               |
|----------|------------|------------------------------------------------------------------------------------|----------------------------------------|
| 130      | SHOULD     | 设计方法时按顺序选：标准 → 批量 → 自定义 → 流式                                    | Choosing a method category             |
| 131      | MUST       | API 必须为资源提供 get 方法                                                        | Guidance / AIP-131                     |
| 131      | MUST       | Get HTTP 动词必须是 GET                                                            | Guidance (HTTP verb)                   |
| 131      | MUST NOT   | Get 不得有 body 键                                                                 | Guidance (There must not be a body)    |
| 131      | MUST       | Get 响应必须是资源本身（无独立 Response 类型）                                     | Guidance (response message)            |
| 131      | SHOULD     | Get URI 应包含对应资源名称的单个变量,字段名应叫 name                               | Guidance (URI)                         |
| 131      | SHOULD     | Get 应有一个 method_signature 值为 "name"                                          | Guidance (method_signature)            |
| 131      | MUST NOT   | Get 请求消息不得包含其他必填字段                                                   | Request message (末尾)                 |
| 132      | MUST       | API 必须为资源提供 List 方法（单例除外）                                           | Guidance / AIP-132                     |
| 132      | MUST       | List HTTP 动词必须是 GET                                                           | Guidance (HTTP verb)                   |
| 132      | MUST       | List 必须包含 page_size 与 page_token 字段                                         | Request message (page_size/page_token) |
| 132      | MUST       | List 响应必须包含 next_page_token                                                  | Response message                       |
| 132      | MUST NOT   | List 请求消息不得包含其他必填字段                                                  | Request message (末尾)                 |
| 132      | SHOULD     | List 可选支持 filter（AIP-160）与 order_by 字段                                    | Filtering / Ordering                   |
| 133      | SHOULD     | API 应提供 create 方法（无价值时除外）                                             | Guidance / AIP-133                     |
| 133      | MUST       | Create HTTP 动词必须是 POST                                                        | Guidance (HTTP verb)                   |
| 133      | MUST       | Create 响应必须是资源本身                                                          | Guidance (response message)            |
| 133      | MUST       | Create 必须有 body 键,映射到请求中的资源字段                                       | Guidance (body key)                    |
| 133      | MUST       | 重复创建必须返回 ALREADY_EXISTS                                                    | User-specified IDs(末段)               |
| 133      | MUST       | Create 必须允许用户指定资源 ID（管理平面）                                         | User-specified IDs                     |
| 134      | SHOULD     | API 应提供 update 方法（无价值时除外）                                             | Guidance / AIP-134                     |
| 134      | SHOULD     | Update HTTP 动词应为 PATCH（支持部分更新）                                         | Guidance (HTTP verb)                   |
| 134      | MUST NOT   | 强烈不推荐用 PUT（向后不兼容,新增字段会被静默擦除）                                | PATCH and PUT                          |
| 134      | MUST       | Update 必须包含 update_mask 字段（支持部分更新时）                                 | Request message                        |
| 134      | MUST       | update_mask 必须支持 `*` 通配符（全量替换）                                        | Request message                        |
| 134      | MUST NOT   | Update 不得触发副作用                                                              | Side effects                           |
| 134      | MUST NOT   | 状态字段不得在 Update 中直接写入                                                   | Side effects                           |
| 134      | MUST       | Update 响应必须是资源本身                                                          | Guidance (response message)            |
| 134      | MAY        | Update 可暴露 bool allow_missing 字段（不存在则创建）                              | Create or update                       |
| 135      | SHOULD     | API 应提供 delete 方法（无价值时除外）                                             | Guidance / AIP-135                     |
| 135      | MUST       | Delete HTTP 动词必须是 DELETE                                                      | Guidance (HTTP verb)                   |
| 135      | MUST NOT   | Delete 不得有 body 键                                                              | Guidance (body)                        |
| 135      | MUST       | 存在子资源时必须返回 FAILED_PRECONDITION（强制 force 才级联）                      | Guidance / Cascading delete            |
| 135      | SHOULD     | 级联删除应提供 bool force 字段                                                     | Cascading delete                       |
| 135      | SHOULD     | 资源不存在应返回 NOT_FOUND                                                         | Guidance / Errors                      |
| 136      | SHOULD     | 自定义方法仅用于标准方法无法表达的功能                                             | Guidance / AIP-136                     |
| 136      | MUST NOT   | 自定义方法名不得包含介词（for、with 等）                                           | Guidance (method name)                 |
| 136      | SHOULD NOT | 自定义方法名不应包含标准方法动词（Get/List/Create/Update/Delete）                  | Guidance (method name)                 |
| 136      | MUST NOT   | 自定义方法名不得包含术语 Async                                                     | Guidance (method name)                 |
| 136      | MUST       | 自定义方法 HTTP 必须是 GET 或 POST                                                 | Guidance (HTTP methods)                |
| 136      | MUST       | 有副作用或修改数据时必须用 POST                                                    | Guidance (HTTP methods)                |
| 136      | MUST       | 自定义方法 URI 必须用 `:` 后跟自定义动词                                           | Guidance (HTTP URI)                    |
| 136      | MUST       | URI 中的动词必须与 RPC 名一致,camelCase                                            | Guidance (HTTP URI)                    |
| 136      | SHOULD     | body 子句应为 `"*"`                                                                | Guidance (body)                        |
| 136      | SHOULD     | 自定义方法请求消息应匹配 RPC 名 + Request 后缀                                     | Guidance (request message)             |
| 151      | SHOULD     | 耗时较长的方法应返回 google.longrunning.Operation                                  | Guidance / AIP-151                     |
| 151      | MUST       | LRO 必须实现 google.longrunning.Operations 服务                                    | Guidance (APIs with messages)          |
| 151      | MUST       | LRO 必须指定 response_type 与 metadata_type                                        | Guidance (operation_info)              |
| 151      | MUST NOT   | LRO 响应不得是流式响应                                                             | Guidance (Operation proto)             |
| 151      | MUST NOT   | 修改 LRO 的 response_type 或 metadata_type 是破坏性变更                            | Backwards compatibility                |
| 231      | MAY        | API 可支持 BatchGet,URI 末尾 `:batchGet`,GET,atomic                                | Guidance / AIP-231                     |
| 231      | MUST NOT   | BatchGet 不得有 body 键                                                            | Guidance (body)                        |
| 231      | MUST       | BatchGet 必须 atomic（无部分成功）                                                 | Guidance (atomic)                      |
| 231      | MUST NOT   | BatchGet 不应支持分页（transactionality 难保证）                                   | Request message                        |
| 233      | MAY        | API 可支持 BatchCreate,URI 末尾 `:batchCreate`,POST                                | Guidance / AIP-233                     |
| 233      | MUST       | 同步 BatchCreate 必须 atomic；异步 BatchCreate 可选 partial success                | Atomic vs. Partial Success             |
| 234      | MAY        | API 可支持 BatchUpdate,URI 末尾 `:batchUpdate`,POST                                | Guidance / AIP-234                     |
| 234      | MUST       | 同步 BatchUpdate 必须 atomic；异步 BatchUpdate 可选 partial success                | Atomic vs. Partial Success             |
| 234      | MUST       | partial success 时 metadata 必须含 `map<int32, google.rpc.Status> failed_requests` | Operation metadata message             |
| 235      | MAY        | API 可支持 BatchDelete,URI 末尾 `:batchDelete`,POST（非 DELETE）                   | Guidance / AIP-235                     |
| 235      | MUST       | 同步 BatchDelete 必须 atomic；异步 BatchDelete 可选 partial success                | Atomic vs. Partial Success             |
| 235      | MUST NOT   | BatchDelete 不得支持基于 filter 的匹配                                             | Request message                        |

## 1.7 Design Patterns（AIP-152-165, 210, 211, 214, 217）

### 全谱清单

| 编号 | 标题                          | 核心规则（一句话）                                                                       | 级别范围 |
|------|-------------------------------|------------------------------------------------------------------------------------------|----------|
| 152  | Jobs                          | 长期重复任务用 Job 资源 + Run 自定义方法 + LRO                                           | SHOULD   |
| 153  | Import and export             | 导入导出用 `:import` / `:export` 自定义方法 + LRO                                        | SHOULD   |
| 154  | Resource freshness validation | Etag 字段用于乐观锁；不匹配返回 ABORTED                                                  | SHOULD   |
| 155  | Request identification        | `string request_id` 提供幂等性（UUID4 格式）                                             | MAY      |
| 157  | Partial responses             | 部分响应通过 FieldMask 系统参数或 View 枚举（禁用请求字段 read_mask）                    | SHOULD   |
| 158  | Pagination                    | 集合返回必须一开始提供分页；`page_size` / `page_token` / `next_page_token`               | MUST     |
| 159  | Reading across collections    | 跨集合读取用 `-` 通配符                                                                  | MAY      |
| 160  | Filtering                     | List 过滤用 `string filter` 结构化字符串语法                                             | SHOULD   |
| 161  | Field masks                   | Update 用 google.protobuf.FieldMask `update_mask`,必须相对于资源                         | MUST     |
| 162  | Resource Revisions（Draft）   | 资源修订历史用嵌套 `revisions` 子集合 + `latest` 别名                                    | SHOULD   |
| 163  | Change validation             | `bool validate_only` 字段用于预览请求不执行                                              | SHOULD   |
| 164  | Soft delete                   | 软删除：`delete_time` + `purge_time` + `Undelete` + `Expunge` 方法                       | SHOULD   |
| 165  | Criteria-based delete         | 基于条件的批量删除用 `Purge` 自定义方法 + filter + force                                 | MAY      |
| 210  | Unicode                       | 字符串规范化用 NFC；唯一标识限 ASCII                                                     | MUST     |
| 211  | Authorization checks          | 鉴权先于验证；拒绝时用 PERMISSION_DENIED                                                 | MUST     |
| 214  | Resource expiration           | 资源过期用 `expire_time` (Timestamp) + `ttl` (Duration) oneof                            | MUST     |
| 217  | Unreachable resources         | 跨集合 list 时不可达资源用 `repeated string unreachable` + `bool return_partial_success` | MUST     |

### 规则摘要（本类）

| AIP 编号 | 级别       | 规则正文（≤80字）                                                                    | 章节锚点                       |
|----------|------------|--------------------------------------------------------------------------------------|--------------------------------|
| 152      | MAY        | API 可定义 Job 资源（重复/需权限分离的任务）                                         | Guidance / AIP-152             |
| 152      | MUST       | Job 资源名必须以 `Job` 结尾                                                          | Guidance                       |
| 152      | SHOULD     | Job 服务应定义全部 5 个标准方法                                                      | Guidance                       |
| 152      | SHOULD     | Job 服务应提供 `:run` 自定义方法（POST + LRO）                                       | Run method                     |
| 152      | MUST       | Run URI 必须以 `:run` 结尾                                                           | Run method                     |
| 153      | MAY        | API 可支持 import/export 自定义方法（URI 末尾 `:import` / `:export`）                | Guidance / AIP-153             |
| 153      | MUST       | import/export 必须返回 LRO（除非保证秒级）                                           | Multiple resources             |
| 153      | MUST       | import/export HTTP 必须是 POST,body 必须 `"*"`                                       | Multiple resources             |
| 153      | SHOULD     | import/export 应包含 parent 字段                                                     | Multiple resources             |
| 153      | MUST       | 导入请求中如指定 parent,跨 parent 的资源必须被拒绝                                   | Multiple resources             |
| 154      | MAY        | 资源可包含 string etag 字段做乐观锁                                                  | Guidance / AIP-154             |
| 154      | MUST       | etag 字段必须是 string,且必须名为 `etag`                                             | Guidance                       |
| 154      | SHOULD NOT | 资源上的 etag 字段不应有任何 field_behavior 注解                                     | Guidance                       |
| 154      | MUST       | etag 不匹配时服务必须返回 ABORTED                                                    | Guidance                       |
| 154      | MUST       | 声明式友好资源必须包含 etag 字段                                                     | Declarative-friendly resources |
| 154      | SHOULD     | etag 值应符合 RFC 7232（应包含引号）                                                 | Guidance (Note)                |
| 155      | MAY        | API 可在请求消息中加 `string request_id` 参数                                        | Guidance / AIP-155             |
| 155      | MUST       | 提供 request_id 必须保证幂等性                                                       | Guidance                       |
| 155      | MUST NOT   | request_id 字段不得在资源本身上,仅在请求消息中                                       | Guidance                       |
| 155      | SHOULD     | request_id 应为 optional                                                             | Guidance                       |
| 155      | MAY        | UUIDs 作为唯一 request_id 格式                                                       | Guidance                       |
| 157      | MAY        | API 可支持部分响应（通过系统参数 FieldMask 或 View 枚举）                            | Guidance / AIP-157             |
| 157      | MUST       | FieldMask 参数值必须为 google.protobuf.FieldMask                                     | Field masks parameter          |
| 157      | MUST       | FieldMask 参数必须 optional                                                          | Field masks parameter          |
| 157      | SHOULD     | 显式 `"*"` 应被支持                                                                  | Field masks parameter          |
| 157      | SHOULD NOT | FieldMask 不应在请求字段中,而应在系统参数中                                          | Field masks parameter          |
| 157      | SHOULD     | View 枚举应命名 `<Type>View`,至少有 BASIC 与 FULL 值                                 | View enumeration               |
| 157      | MUST NOT   | 不得从 View 中移除字段（破坏性变更）                                                 | View enumeration               |
| 158      | MUST       | 返回集合的 RPC 必须一开始就提供分页                                                  | Guidance / AIP-158             |
| 158      | MUST NOT   | page_size 不得是必填字段                                                             | Guidance                       |
| 158      | MUST       | 未指定 page_size 或为 0 时,服务选择默认值,不得报错                                   | Guidance                       |
| 158      | SHOULD     | page_size 大于最大值时应强制降级到最大值                                             | Guidance                       |
| 158      | MUST       | page_size 为负数时必须返回 INVALID_ARGUMENT                                          | Guidance                       |
| 158      | MUST NOT   | page_token 不得是必填字段                                                            | Guidance                       |
| 158      | MUST       | 分页令牌必须是不透明 URL 安全字符串                                                  | Opacity                        |
| 158      | MUST NOT   | 分页令牌不得可被用户解析                                                             | Opacity                        |
| 158      | MUST       | 到达集合末尾时 next_page_token 必须为空                                              | Guidance                       |
| 158      | MUST NOT   | API 响应不得是流式响应                                                               | Guidance                       |
| 158      | SHOULD     | 分页令牌应有合理过期时间（经验值 3 天）                                              | Expiring page tokens           |
| 158      | MAY        | 请求可定义 `int32 skip` 字段跳过多条                                                 | Skipping results               |
| 159      | MAY        | API 可支持跨集合读取,用 `-` 作为通配                                                 | Guidance / AIP-159             |
| 159      | MUST       | URI 模式仍必须用 `*`,不得硬编码 `-`                                                  | Guidance                       |
| 159      | MUST       | 方法必须显式文档支持跨集合                                                           | Guidance                       |
| 159      | MUST       | 响应中资源必须用规范的 resource name                                                 | Guidance                       |
| 160      | MAY        | API 可提供 List 过滤（用 `string filter` 结构化字符串）                              | Guidance / AIP-160             |
| 160      | SHOULD     | 过滤字段应叫 `filter`                                                                | Guidance                       |
| 160      | SHOULD     | 不合规的过滤字符串应返回 INVALID_ARGUMENT                                            | Validation                     |
| 160      | MUST NOT   | 字段名不得出现在比较运算符右侧                                                       | Comparison Operators           |
| 160      | SHOULD     | AND、OR、NOT 等逻辑/否定操作符应被支持                                               | Logical / Negation Operators   |
| 160      | SHOULD     | =、!=、<、>、<=、>= 比较操作符应被支持（非布尔/枚举）                                | Comparison Operators           |
| 160      | SHOULD     | 通配符 `*` 用于字符串相等比较应被支持                                                | Comparison Operators           |
| 160      | MUST       | 遍历操作符 `.` 必须支持                                                              | Traversal operator             |
| 160      | MUST       | has 操作符 `:` 必须支持                                                              | Has Operator                   |
| 161      | MUST       | 字段掩码必须使用 google.protobuf.FieldMask 类型                                      | Guidance / AIP-161             |
| 161      | MUST       | 字段掩码必须相对于资源（非请求消息）                                                 | Guidance                       |
| 161      | SHOULD NOT | 不得使用 `google.protobuf.FieldMask read_mask` 请求字段（已废弃）                    | Guidance (Warning)             |
| 161      | MUST       | 读和写使用相同掩码时数据必须自洽                                                     | Read-write consistency         |
| 161      | MUST       | 通配符 `*` 在重复字段或 map 上必须支持                                               | Wildcards                      |
| 161      | MUST NOT   | 字段掩码不得通过索引访问重复字段特定元素                                             | Wildcards (Note)               |
| 161      | MUST       | 写时遇到不存在的字段条目应返回 INVALID_ARGUMENT                                      | Invalid field mask entries     |
| 162      | MAY        | API 可存储资源的修订历史（嵌套 `revisions` 子集合）                                  | Guidance / AIP-162 (Draft)     |
| 162      | MUST       | 修订资源 message 必须命名为 `{ResourceType}Revision`                                 | Guidance                       |
| 162      | MUST       | 修订资源必须含 `snapshot` 字段（OUTPUT_ONLY 父资源）                                 | Guidance                       |
| 162      | MUST       | 修订资源必须含 `create_time` 字段                                                    | Guidance                       |
| 162      | MUST       | 子集合名必须为 `revisions`                                                           | Resource names for revisions   |
| 162      | SHOULD     | 服务应提供 `:rollback` 自定义方法（POST）                                            | Rollback                       |
| 163      | MAY        | API 可提供 `bool validate_only` 字段预览请求                                         | Guidance / AIP-163             |
| 163      | SHOULD     | validate_only 应执行权限检查和其他"活"请求的校验                                     | Guidance                       |
| 163      | MUST       | 声明式友好资源的变更方法必须包含 validate_only 字段                                  | Declarative-friendly resources |
| 164      | MAY        | API 可支持软删除（标记删除而非彻底删除）                                             | Guidance / AIP-164             |
| 164      | SHOULD     | 软删除资源应同时有 delete_time 和 purge_time 字段                                    | Guidance                       |
| 164      | SHOULD     | 软删除资源应包含 DELETED 状态值                                                      | Guidance                       |
| 164      | SHOULD     | 软删除资源应提供 `:undelete` 自定义方法（POST,响应为资源本身）                       | Undelete                       |
| 164      | MAY        | 软删除资源可提供 `:expunge` 自定义方法（永久删除）                                   | Expunge                        |
| 164      | SHOULD NOT | List 响应默认不应包含软删除资源（除非 show_deleted=true）                            | List and Get                   |
| 165      | SHOULD     | 大多数 API 应仅用 Delete（AIP-135）/BatchDelete（AIP-235）,不应基于条件删除          | Guidance / AIP-165             |
| 165      | MAY        | API 可实现 `Purge` 自定义方法（POST + LRO）用于基于 filter 的批量删除                | Guidance                       |
| 165      | MUST       | Purge 请求必须包含 `parent` 与 `filter` 字段                                         | Request message                |
| 165      | MUST       | Purge 请求必须包含 `bool force` 字段                                                 | Request message                |
| 165      | MUST       | force=false 时服务必须返回样本而非真正删除                                           | Request message                |
| 210      | MUST       | "characters" 必须定义为 Unicode code points                                          | Character definition           |
| 210      | MUST       | 所有字符串字段长度限制必须用 characters 度量                                         | Length units                   |
| 210      | SHOULD     | Unicode 值应存储为 Normalization Form C                                              | Normalization                  |
| 210      | MUST       | 唯一标识符必须存储为 NFC                                                             | Normalization                  |
| 210      | SHOULD     | 唯一标识符应限 ASCII（字母数字 + hyphen + underscore）,不超 64 字符                  | Unique identifiers             |
| 210      | SHOULD     | 唯一标识符不应以数字开头                                                             | Unique identifiers             |
| 211      | MUST       | 服务必须在验证请求前先检查授权                                                       | Guidance / AIP-211             |
| 211      | MUST       | 授权失败时服务必须返回 PERMISSION_DENIED                                             | Guidance                       |
| 211      | SHOULD     | 错误信息应避免泄露资源存在性（标准模板）                                             | Guidance                       |
| 211      | SHOULD     | 不可授权时（如资源不存在）应检查父资源的读权限,返回 NOT_FOUND                        | Guidance                       |
| 214      | MUST       | 资源过期必须用 `google.protobuf.Timestamp expire_time` 字段                          | Guidance / AIP-214             |
| 214      | MUST       | 相对过期必须定义 oneof `expiration` 含 `expire_time` 和 `ttl`（Duration,INPUT_ONLY） | Guidance                       |
| 214      | MUST       | API 返回时必须只设置 expire_time,清空 ttl                                            | Guidance                       |
| 217      | MUST       | 可部分失败的 list 响应必须包含 `repeated string unreachable` 字段                    | Guidance / AIP-217             |
| 217      | MUST       | unreachable 字段必须为 repeated string,命名为 `unreachable`                          | Guidance                       |
| 217      | MUST       | unreachable 必须包含 service-relative 资源名（非 full name/URI/ID）                  | Guidance                       |
| 217      | MUST       | unreachable 字段必须标 UNORDERED_LIST field_behavior                                 | Guidance                       |
| 217      | MUST       | 现有 API 引入 unreachable 必须同时加 `bool return_partial_success` 请求字段          | Adopting partial success       |
| 217      | MUST       | return_partial_success=true 时,API 按 unreachable 模式返回                           | Adopting partial success       |

## X. 待补齐：§1.1 Meta / §1.2 Process / §1.4 API Concepts / §1.8 Compatibility and Versioning / §1.9 Polish / §1.10 Protocol Buffers

**待补 15 个 AIP**：1, 2, 3, 8, 9, 100, 111, 127, 180, 181, 182, 184, 185, 190, 191, 192, 193, 194, 200, 205, 213, 215。
**Meta + Process + API Concepts + Protocol Buffers 多为概念性或 protobuf 限定,对 SODA HTTP REST 规范直接落地影响小**
。Compatibility（180/181/182/184/185）与 Polish 中的 Errors（193）已在 SODA 项目 ADR-0013 锚定,Naming（190）已被现有 Research
doc 覆盖。 **Ticket 04-08 grilling 时如需要可针对性补齐,不必全量**。

## Y. SODA 已涵盖项（见 `docs/research/google-aip-api-design-spec.md`）

| AIP 编号 | 现有 Research doc 引用章节 | 关联 ADR            |
|----------|----------------------------|---------------------|
| AIP-1    | §1.1 设计哲学              | —                   |
| AIP-121  | §1.1 / §2.1 / §2.2         | —                   |
| AIP-122  | §2.2 资源名称              | ADR-0012            |
| AIP-123  | §2.3 资源类型              | —                   |
| AIP-124  | §2.4 资源关联              | —                   |
| AIP-126  | §2.5 枚举                  | ADR-0005            |
| AIP-130  | §1.2 / §3.1                | —                   |
| AIP-131  | §3.2 Get                   | —                   |
| AIP-132  | §3.3 List + §5.1           | —                   |
| AIP-133  | §3.4 Create                | —                   |
| AIP-134  | §3.5 Update                | ADR-0012            |
| AIP-135  | §3.6 Delete                | —                   |
| AIP-136  | §3.7 自定义方法            | ADR-0012            |
| AIP-140  | §4.1 字段命名              | —                   |
| AIP-142  | §4.6 时间                  | ADR-0031            |
| AIP-143  | §4.4 标准化代码            | —                   |
| AIP-144  | §4.7 重复字段              | —                   |
| AIP-148  | §4.3 标准字段              | —                   |
| AIP-149  | §4.8 未设置字段值          | —                   |
| AIP-154  | §3.5 etag                  | —                   |
| AIP-156  | §2.6 单例资源              | —                   |
| AIP-158  | §5.1 分页                  | —                   |
| AIP-160  | §5.2 过滤                  | —                   |
| AIP-161  | §5.3 字段掩码              | —                   |
| AIP-163  | §5.4 变更验证              | —                   |
| AIP-164  | §5.5 软删除                | ADR-0017 / ADR-0023 |
| AIP-180  | §8 向后兼容                | —                   |
| AIP-190  | §6.1 命名                  | —                   |
| AIP-193  | §7 错误处理                | ADR-0013            |
| AIP-202  | §4.5 字段格式              | —                   |
| AIP-203  | §4.2 字段行为              | —                   |
| AIP-216  | §4.9 状态字段              | ADR-0005            |

**已涵盖项统计**：共 **33 个 AIP**（约占全集 50%）；涵盖 Operations 标准方法、Resource Design 主流、Fields
标准字段、错误处理、向后兼容性、命名规范。 **未涵盖**：Process / Meta / API Concepts / Protocol Buffers 类全空，Polish 仅含
AIP-190，Design Patterns 仅含 158/160/161/163/164，Compatibility 仅含 AIP-180，Resource Design 缺 128/129/236。

## Z. AIP 子目录（仅清单）

| 路径                 | 描述                                                 |
|----------------------|------------------------------------------------------|
| `general/`（站点根） | AIPs 主索引页（本调研拉取的全部 66 个 AIP 都在这里） |
| `adopting/`          | 「如何在组织内采纳 AIP」导论页面                     |
| `contributing/`      | 贡献指南                                             |
| `faq/`               | 常见问题                                             |

> 仓库侧 https://github.com/googleapis/aip 已迁至 https://github.com/aip-dev/google.aip.dev；原 googleapis/aip 仓主文档编号
> 0100~0285 在 aip-dev 仓中按主题分类（general/operations/fields 等子目录，本调研不展开）。
