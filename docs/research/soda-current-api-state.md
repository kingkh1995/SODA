---
type: Research
title: SODA 现有 API 状态基线（Controller / WebAssembler / 错误结构 / AIP 引用链）
description: 钉 ticket 04-08 逐类 grilling 探讨前的项目基线——Controller 端点、WebAssembler 签名、Result/ErrorInfo 实现、现有 AIP 引用与 ADR 锚定、明显偏差。仅记录现状,不评价、不修复。**改造前快照**（`6bbe98b` 之前的形态；Result/ErrorInfo 已删、两个 `:requestChange*Code` 端点已更名——现行形态见 `docs/conventions/aip-api-conventions.md`）。
tags: [ aip, soda, baseline, api-surface ]
status: stable
sources:
  - resource: file://C:/Users/mm/IdeaProjects/SODA
    id: soda-repo
    title: SODA 仓库源码 + docs
generated:
  by: wayfinder/03
  at: 2026-09-02T00:00:00Z
---

# SODA 现有 API 状态基线

> 范围:仅记录项目现状,不评价、不修复;后续 ticket 04-08 逐类 grilling 时与 AIP 全谱对照。

## 1. Controller 清单

| 文件                                                                              | 类注解                                                             | 基础 URL     |
|-----------------------------------------------------------------------------------|--------------------------------------------------------------------|--------------|
| `soda-user/soda-user-adapter/src/main/java/com/soda/user/web/UserController.java` | `@RestController` + `@RequestMapping("/api/users")` + `@Validated` | `/api/users` |

> 截至 2026-09-02,项目仅有 **1 个 Controller**(UserController)。所有端点集中在 `soda-user/soda-user-adapter` 模块。

## 2. 端点表

| Controller     | HTTP 方法 | URL 模式                                  | Request                          | Response       | 调用 Service                              | 语义                           |
|----------------|-----------|-------------------------------------------|----------------------------------|----------------|-------------------------------------------|--------------------------------|
| UserController | `POST`    | `/api/users`                              | `CreateUserRequest`              | `UserResponse` | `UserService.createUser`                  | AIP-133 Create                 |
| UserController | `PATCH`   | `/api/users/{id}`                         | `UpdateUserRequest`              | `Void`         | `UserService.updateUser`                  | AIP-134 Update(无 update_mask) |
| UserController | `DELETE`  | `/api/users/{id}`                         | —                                | `Void`         | `UserService.deregisterUser`              | AIP-135 Delete(注销=终态)      |
| UserController | `POST`    | `/api/users/{id}:disable`                 | —                                | `Void`         | `UserService.disableUser`                 | AIP-136 自定义方法             |
| UserController | `POST`    | `/api/users/{id}:enable`                  | —                                | `Void`         | `UserService.enableUser`                  | AIP-136 自定义方法             |
| UserController | `POST`    | `/api/users/{id}:changeUsername`          | `ChangeUsernameRequest`          | `Void`         | `UserService.changeUsername`              | AIP-136 自定义方法             |
| UserController | `POST`    | `/api/users/{id}:changePassword`          | `ChangePasswordRequest`          | `Void`         | `UserAuthService.changePassword`          | AIP-136 自定义方法             |
| UserController | `POST`    | `/api/users/{id}:requestChangeMobileCode` | `RequestChangeMobileCodeRequest` | `Void`         | `UserAuthService.requestChangeMobileCode` | AIP-136 自定义方法(UCC 发码)   |
| UserController | `POST`    | `/api/users/{id}:changeMobile`            | `ChangeMobileRequest`            | `Void`         | `UserAuthService.changeMobile`            | AIP-136 自定义方法(UCC 换绑)   |
| UserController | `POST`    | `/api/users/{id}:requestChangeEmailCode`  | `RequestChangeEmailCodeRequest`  | `Void`         | `UserAuthService.requestChangeEmailCode`  | AIP-136 自定义方法(UCC 发码)   |
| UserController | `POST`    | `/api/users/{id}:changeEmail`             | `ChangeEmailRequest`             | `Void`         | `UserAuthService.changeEmail`             | AIP-136 自定义方法(UCC 换绑)   |

**端点统计**: 共 11 个。CRUD 占 3 (Create / Update / Delete),自定义动词占 8 (状态/凭证/UCC 发码与消费)。 **未实现**:
`GET /api/users/{id}`(Get)、`GET /api/users`(List)。

## 3. WebAssembler 清单

| 接口               | 包                            | 方法签名                                                                                                           |
|--------------------|-------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `UserWebAssembler` | `com.soda.user.web.assembler` | `toCreateCommand(CreateUserRequest)` → `CreateUserCommand`                                                         |
|                    |                               | `toUpdateCommand(Long userId, UpdateUserRequest)` → `UpdateUserCommand`                                            |
|                    |                               | `toDeregisterCommand(Long userId)` → `DeregisterUserCommand`                                                       |
|                    |                               | `toDisableCommand(Long userId)` → `DisableUserCommand`                                                             |
|                    |                               | `toEnableCommand(Long userId)` → `EnableUserCommand`                                                               |
|                    |                               | `toChangeUsernameCommand(Long userId, ChangeUsernameRequest)` → `ChangeUsernameCommand`                            |
|                    |                               | `toChangePasswordCommand(Long userId, ChangePasswordRequest)` → `ChangePasswordCommand`                            |
|                    |                               | `toRequestChangeMobileCodeCommand(Long userId, RequestChangeMobileCodeRequest)` → `RequestChangeMobileCodeCommand` |
|                    |                               | `toChangeMobileCommand(Long userId, ChangeMobileRequest)` → `ChangeMobileCommand`                                  |
|                    |                               | `toRequestChangeEmailCodeCommand(Long userId, RequestChangeEmailCodeRequest)` → `RequestChangeEmailCodeCommand`    |
|                    |                               | `toChangeEmailCommand(Long userId, ChangeEmailRequest)` → `ChangeEmailCommand`                                     |
|                    |                               | `toResponse(UserDTO)` → `UserResponse`                                                                             |
|                    |                               | `toResponseList(List<UserDTO>)` → `List<UserResponse>`                                                             |

> MapStruct 接口,`componentModel = "spring"`,`unmappedTargetPolicy = ERROR`。共 **13 个方法**(11 个 Request→Command + 2 个
> DTO→Response)。

## 4. 错误结构实现

### 4.1 Result

源:`soda-components/soda-component-adapter-starter-web/src/main/java/com/soda/component/web/Result.java`

| 字段    | 类型                  | 说明                                            |
|---------|-----------------------|-------------------------------------------------|
| `code`  | `int`                 | 业务码(0 = 成功)                                |
| `msg`   | `String`              | 消息                                            |
| `data`  | `@Nullable T`         | 数据段,成功时存在;`@JsonInclude(NON_NULL)` 省略 |
| `error` | `@Nullable ErrorInfo` | 错误详情(AIP-193);成功时省略                    |

工厂方法:`success()` / `success(T data)` / `error(int code, String msg)` / `error(int code, String msg, ErrorInfo)`。

### 4.2 ErrorInfo

源:`soda-components/soda-component-adapter-starter-web/src/main/java/com/soda/component/web/ErrorInfo.java`

| 字段       | 类型                           | 说明                                                             |
|------------|--------------------------------|------------------------------------------------------------------|
| `reason`   | `String`                       | UPPER_SNAKE_CASE 语义码(如 `ALREADY_EXISTS`、`INVALID_ARGUMENT`) |
| `domain`   | `String`                       | 服务域(如 `soda-user.example.com`)                               |
| `metadata` | `@Nullable Map<String,String>` | 上下文键值对;`@JsonInclude(NON_NULL)` 省略                       |

实现形式:`record`(Java 17+);统一信封与错误结构决策见 ADR-0009/0013。

## 5. 现有 AIP 引用链 (来自 `docs/research/google-aip-api-design-spec.md`)

| AIP 编号 | 引用章节                                                                                                                                    | 关联 ADR                                            |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| AIP-1    | §1.1 设计哲学                                                                                                                               | —                                                   |
| AIP-121  | §1.1 / §2.1 资源导向设计 / §2.2 资源名称                                                                                                    | —                                                   |
| AIP-122  | §2.2 资源名称(`camelCase`、RFC-1123)                                                                                                        | ADR-0012(URL 命名)                                  |
| AIP-123  | §2.3 资源类型(`{Service}/{Type}`、PascalCase)                                                                                               | —                                                   |
| AIP-124  | §2.4 资源关联                                                                                                                               | —                                                   |
| AIP-126  | §2.5 枚举(UPPER_SNAKE_CASE、UNSPECIFIED)                                                                                                    | ADR-0005(短名)                                      |
| AIP-130  | §1.2 / §3.1 方法选择优先级                                                                                                                  | —                                                   |
| AIP-131  | §3.2 Get 方法                                                                                                                               | —                                                   |
| AIP-132  | §3.3 List 方法 + §5.1 分页基础                                                                                                              | —                                                   |
| AIP-133  | §3.4 Create 方法                                                                                                                            | —                                                   |
| AIP-134  | §3.5 Update 方法(update_mask、etag)                                                                                                         | ADR-0012(禁 PUT)                                    |
| AIP-135  | §3.6 Delete 方法                                                                                                                            | —                                                   |
| AIP-136  | §3.7 自定义方法(`:verb`、camelCase)                                                                                                         | ADR-0012(:verb)                                     |
| AIP-140  | §4.1 字段命名                                                                                                                               | —                                                   |
| AIP-142  | §4.6 时间与持续时间                                                                                                                         | ADR-0031(DecimalEpochMilli)                         |
| AIP-143  | §4.4 标准化代码                                                                                                                             | —                                                   |
| AIP-144  | §4.7 重复字段                                                                                                                               | —                                                   |
| AIP-148  | §4.3 标准字段(name / parent / display_name / create_time / update_time / delete_time / purge_time / expire_time / uid / etag / annotations) | —                                                   |
| AIP-149  | §4.8 未设置字段值(包装类型)                                                                                                                 | —                                                   |
| AIP-154  | §3.5 etag 乐观锁                                                                                                                            | —                                                   |
| AIP-156  | §2.6 单例资源                                                                                                                               | —                                                   |
| AIP-158  | §5.1 分页(`page_size` / `page_token` / `next_page_token`)                                                                                   | —                                                   |
| AIP-160  | §5.2 过滤(结构化字符串)                                                                                                                     | —                                                   |
| AIP-161  | §5.3 字段掩码(FieldMask)                                                                                                                    | —                                                   |
| AIP-163  | §5.4 变更验证(`validate_only`)                                                                                                              | —                                                   |
| AIP-164  | §5.5 软删除(delete_time / undelete / purge)                                                                                                 | ADR-0017 / ADR-0023                                 |
| AIP-180  | §8 向后兼容性                                                                                                                               | —                                                   |
| AIP-190  | §6.1 命名                                                                                                                                   | —                                                   |
| AIP-193  | §7 错误处理(ErrorInfo / reason / domain / metadata)                                                                                         | ADR-0013                                            |
| AIP-202  | §4.5 字段格式(UUID4 / IPV4 / IPV6)                                                                                                          | —                                                   |
| AIP-203  | §4.2 字段行为(REQUIRED / OPTIONAL / OUTPUT_ONLY / IMMUTABLE / INPUT_ONLY)                                                                   | —                                                   |
| AIP-216  | §4.9 状态字段                                                                                                                               | ADR-0005                                            |
| —        | §9 SODA 项目适配说明                                                                                                                        | ADR-0021(Verification 拓扑)、ADR-0026(发码两域模型) |

**已涵盖 AIP 编号**:共 **33 个**(AIP-1 / 121 / 122 / 123 / 124 / 126 / 130-136 / 140 / 142 / 143 / 144 / 148 / 149 /
154 / 156 / 158 / 160 / 161 / 163 / 164 / 180 / 190 / 193 / 202 / 203 / 216)。 **锚定 ADR**:ADR-0005 / 0009 / 0012 /
0013 / 0017 / 0021 / 0023 / 0026 / 0031。

## 6. 明显偏差 (暂不修复,仅记录)

| 偏差描述                                                                 | file:line                                                                                    | 冲突的 AIP                                         | 备注                                                                                                                                                                                               |
|--------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 缺失 `GET /api/users/{id}`(Get)与 `GET /api/users`(List)                 | `UserController.java`(全文无 @GetMapping)                                                    | AIP-121(资源必须支持 Get/List) / AIP-131 / AIP-132 | 现状:11 个端点无任何 Get/List                                                                                                                                                                      |
| Update 请求体无 `update_mask` 字段                                       | `UpdateUserRequest.java`(全文无 `update_mask` 字段)                                          | AIP-134(MUST)                                      | 现状:全字段 PATCH,无 FieldMask                                                                                                                                                                     |
| Update 无 etag 乐观锁                                                    | `UpdateUserRequest.java`(无 `etag` 字段)                                                     | AIP-154(MAY)/ AIP-134                              | 现状:无乐观锁,后续编辑直接覆盖                                                                                                                                                                     |
| 无 `validate_only` 字段                                                  | 全 Controller / Request 类                                                                   | AIP-163(MAY→SHOULD)                                | 现状:无 dry-run 机制                                                                                                                                                                               |
| 无 `permission_denied` 前置检查                                          | 全 Controller(无 `@PreAuthorize` 等)                                                         | AIP-193(权限检查先于存在性)                        | 现状:权限检查放应用层或缺失                                                                                                                                                                        |
| 资源 ID 用 `Long` 而非 AIP 字符串 `name`                                 | `UserController.java:59, 70, 80, 90, 100, 111, 122, 133, 144, 155` (`@PathVariable Long id`) | AIP-122(资源名称格式 `collection/{id}`)            | DDD 决策:服务端生成 Long ID,见 ADR-0017 + Research §9 SODA 适配段                                                                                                                                  |
| 无分页字段(`page_size` / `page_token` / `next_page_token`)               | 全 Controller / Request 类                                                                   | AIP-158 / AIP-132                                  | 现状:无 List 端点,无分页需求                                                                                                                                                                       |
| 枚举值用单字母短名(`E` / `D`)而非 UPPER_SNAKE_CASE                       | 见 ADR-0005(`enum-short-name`)                                                               | AIP-126(MUST)                                      | 已记录为有意偏离,见 ADR-0005                                                                                                                                                                       |
| 标准字段未全量暴露(`name` / `parent` / `create_time` / `update_time` 等) | `UserResponse.java`(22 行,字段有限)                                                          | AIP-148                                            | DDD 适配:Resource 内 ID 即 name;时间字段未暴露给前端                                                                                                                                               |
| 自定义动词 `requestChangeMobileCode` 含隐含介词短语                      | `UserController.java:121`(`:requestChangeMobileCode`)                                        | AIP-136(不得含介词)                                | 命名争议:`requestChangeMobileCode` 拆为 `requestChangeMobile` + `Code` 三段复合;严格按 AIP 应叫 `requestMobileChangeCode` 或 `sendMobileChangeCode`。**潜在偏离,需在 ticket 05 grilling 时确认**。 |

> **有意偏离已文档化**:ADR-0005 (枚举短名)、ADR-0012 (禁 PUT / URL 命名)、ADR-0013 (错误结构)、ADR-0017 (聚合生命周期)
> 、ADR-0023 (终态键释放)、ADR-0026 (Verification 双概念模型)、ADR-0031 (时间格式 DecimalEpochMilli)。 **潜在偏离未文档化**:
> `requestChangeMobileCode` 命名争议。
