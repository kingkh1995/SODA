# User Entity — Intention-Revealing Domain Methods + AIP-grounded REST API

User 聚合根的方法按业务意图命名（`changeXxx` / `disable` / `enable`），不暴露贫血 setter 或泛化的 `changeStatus`。REST API 遵循 Google AIP 标准：个人资料用 PATCH 标准方法，状态跃迁用 `:disable`/`:enable` 自定义方法。

## 问题

改造前 `User` 实体混用了两种风格：`changeUsername`/`changeStatus`（领域行为命名）和 `setNickname`/`setMobile`/`setEmail`/`setSex`/`setAvatar`（贫血 setter）。API 层面 `updateUser` 接收 5 个 nullable 字段，`updateStatus` 接收 `String status`，两者都迫使调用方处理不应知晓的内部状态枚举。

- setter 无法附加业务逻辑——未来换手机号需要重新验证时，无处拦截
- `changeStatus(UserState)` 接收任意 `UserState` 值，不限制合法跃迁路径，允许从任何状态到任何状态的非法调用
- API 暴露了泛化的状态字段（"E"/"D"），而不是按意图表达（"disable this user"）

## 决策

### Domain 实体方法

`User` 的 6 个属性修改方法统一为 `changeXxx` 模式，外加两个状态跃迁方法：

```java
// 属性修改（每个字段独立，因为不同字段有不同业务语义）
void changeNickname(Nickname nickname)
void changeMobile(Mobile mobile)      // 配合验证流程修改，见 mobile/email 验证 API
void changeEmail(Email email)         // 配合验证流程修改，见 mobile/email 验证 API
void changeSex(Sex sex)
void changeAvatar(Avatar avatar)
void changePassword(CredentialHash)
void changeUsername(Username username) // 已有，保持

// 状态跃迁（用具体意图取代泛化状态机）
void disable()    // E→D: UserStateChangedEvent; 已是D: no-op
void enable()     // D→E: UserStateChangedEvent; 已是E: no-op
```

每个方法的参数都是 Domain Primitive（`Nickname`、`Mobile`、`Email` 等），由调用方在 ApplicationService 中从 Command 转换，实体不接触应用层类型。

### REST API 端点（Google AIP 标准）

REST API 遵循 Google API Improvement Proposals 标准：

| AIP | 规则 | 应用 |
|---|---|---|
| [AIP-122](https://google.aip.dev/122) | 集合标识符用复数 | `users`（不是 `user`） |
| [AIP-134](https://google.aip.dev/134) | Update 用 PATCH，State 字段不可直接写入 | `PATCH /users/{id}` body 不含 state |
| [AIP-136](https://google.aip.dev/136) | 自定义方法 POST + 冒号语法 | `POST /users/{id}:disable` |
| [AIP-216](https://google.aip.dev/216) | 状态转换用自定义方法，State OUTPUT_ONLY | `:disable`/`:enable` 是唯一途径 |

| 方法 | 端点 | 说明 |
|---|---|---|
| `PATCH` | `/users/{id}` | 更新个人资料（body: nickname, sex, avatar）<br>mobile 和 email 不走此端点，走独立验证流程 |
| `POST` | `/users/{id}:disable` | 禁用用户 — AIP-136 自定义方法 |
| `POST` | `/users/{id}:enable` | 启用用户 — AIP-136 自定义方法 |
| `POST` | `/users/{id}:verifyMobile` | 发送手机验证码 — body: { newMobile } |
| `POST` | `/users/{id}:changeMobile` | 验证手机并修改 — body: { code } |
| `POST` | `/users/{id}:changePassword` | 修改密码 — body: { newPassword }，委托到 PasswordAuthAccount.changePassword()（旧密码校验待实现，见待办） |
| `POST` | `/users/{id}:verifyEmail` | 发送邮箱验证码 — body: { newEmail } |
| `POST` | `/users/{id}:changeEmail` | 验证邮箱并修改 — body: { code } |

验证流程（以手机为例）——验证码发起（单聚合创建）在 `UserAuthServiceImpl` 内展开，消费流程集中于 `CredentialChangeDomainService`（跨聚合编排）：
```
# Step 1 — 发送验证码
POST /users/{id}:verifyMobile  { "newMobile": "13800138000" }
AppService 加载 User（存在性校验），执行验证码发起（生成码、构造 SmsVerification 聚合
（scene=CC, target=newMobile, INITIALIZED）、经 `verification.send(smsSender)` 发送转 PENDING）；验证聚合落库

# Step 2 — 验证并修改
POST /users/{id}:changeMobile  { "code": "123456" }
AppService 加载 User + 待验证聚合，经 CredentialChangeDomainService 执行
verify → user.changeMobile(verification) → use，先存 user 再存 verification（USED）
失败路径（错码/过期）不落库——实体无变更（attempts 已随 ADR-0011 修订移除）
```

邮箱验证流程同理，更新 EmailAuthAccount。
注意：mobile/email 因涉及登录凭证修改，**不放在 PATCH 中**，必须走独立验证流程（参考 yudao-cloud 的设计模式）。

| 方案 | 否定原因 |
|---|---|
| Profile VO 作为参数 | `updateProfile(Profile)` — Profile 本质是 DTO/Command，实体不应接受应用层类型作参数 |
| `changeStatus(E/D)` | 不限制跃迁路径，允许 `E→E`、`D→D` 等无意义调用，意图不明确 |
| mobile/email 放在 PATCH 中 | 涉及登录凭证修改，需要验证流程，不能与纯 profile 字段混在一起 |
| 全量 PUT | 无法表达部分更新，前端被迫提交所有字段 |

## 参考

- **kk-ddd**: 无 public setter，方法按领域动词命名（`invalidate()`、`changePassword()`），状态跃迁显式表达
- **COLA 示例**: 领域方法动词命名（`charge(ctx)`、`checkRemaining()`），@Data setter 限于 ORM 层
- **Google AIP**:
  - [AIP-121](https://google.aip.dev/121) Resource-oriented design — API 设计哲学
  - [AIP-122](https://google.aip.dev/122) Resource names — 复数集合标识符、name 字段
  - [AIP-134](https://google.aip.dev/134) Standard methods: Update — PATCH 语义、State 不可写入
  - [AIP-136](https://google.aip.dev/136) Custom methods — `:action` 冒号语法
  - [AIP-216](https://google.aip.dev/216) States — 状态枚举设计、状态转换方法
- **Eric Evans DDD**: 方法名应在通用语言中表达业务意图，而非数据操作
