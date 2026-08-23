# 0026 — Verification 双概念重构：不透明 VerificationSource + 多态 VerificationRecipient（主体裁定 + 拓扑收编）

**Status**: accepted（2026-08-16，grill-with-docs 会话两轮，用户逐项确认）

> 取代范围：ADR-0025（2026-08-15）的通道判别栖身（`Subject` 密封层级）、subject 的类型化表达、`VerificationScene` 归属、发码拓扑。唯一性机制（`active_key`/惰性 DELETE/终态不占唯一键/无 E 态）**全部保留**。
> 会话内修订（同日期）：主体裁定（User 非 Verification）、`VerificationServiceImpl` 作废、api 按用例拆分、policy 必传、ULG/UPR subject=userId、URG 集合级端点。
> 会话内修订二（同日期，用户逐项确认）：① `VerificationRecipient` 通用属性 `target()`（地址字符串，AuthAccountId 根级字符串访问器同构），`value()` 上提为根默认方法（`channel + ":" + target`，JSON 字面值同此）；② 持久化改 `channel` + `target` 两列（替代 recipient 自描述单列——「无独立 channel 列」撤销，restore 经双参工厂 `of(VerificationChannel, String)`）；③ `active_key` 由 convertor 恒设，终态（V/U）清 NULL 收敛进 `gateway.save`。
> 会话内修订三（同日期，用户决策）：**多属性输出**——VerificationRecipient 按多属性 DP 处理，不再输出单属性规范字符串：删除 `value()`（`{channel}:{target}` 前缀串）与 `of(String)` 前缀路由（修订二① 的 value() 根默认方法撤销）；JSON 对象输出 `{"channel":"S","target":"13800138000"}`（根访问器 `@JsonProperty`、类型化组件 `@JsonIgnore`），反序列化经双参 `@JsonCreator`；持久化双列 restore 不变。
> 会话内修订四（同日期，用户决策）：**泛型多态**——`VerificationRecipient<T extends Type>`，`target()` 返回类型化地址 DP（`SmsRecipient.target()` = `Mobile`、`EmailRecipient.target()` = `Email`；record 组件即 target，隐式访问器满足接口方法，投递侧直接 `sms.target()` 取类型化地址，不再依赖 record pattern）；JSON 输出不变（`{"channel","target"}`——组件 target 经地址 DP `@JsonValue` 序列化为裸串，修订三的 `@JsonIgnore` 随组件改名撤销）；持久化侧经密封 switch 落裸值（泛型捕获 → 具体类型，收敛进 convertor）；修订三的 `String target()` 撤销。
> 会话内修订五（同日期）：双参工厂参数改双字符串——`of(String channel, String target)`（channel 枚举解析收敛进工厂内部 `VerificationChannel.of` 单点，IAE 快速失败保留；与 `VerificationSource.of(scene, subject)` 双字符串同构，convertor 免预转换）。
> 会话内修订六（同日期，用户决策）：**LiteralType 契约**——组件层新增 `LiteralType extends Type`（`String value()`，与 `EnumType` 平行），单属性字面量 DP（`Mobile`/`Email`）实现之；`VerificationRecipient<T extends LiteralType>` 泛型边界收紧——基础设施直接 `target().value()` 落裸串，convertor 密封 switch 删除（泛型捕获自带 value()，不再判别具体类型）。
> 会话内修订七（同日期，用户决策，**LiteralType 契约重构，见 ADR-0028**）：`LiteralType` 单接口（`String value()`）不足以覆盖原语字面量——重构为五家族契约 `StringLiteralType`/`LongLiteralType`/`IntLiteralType`/`BooleanLiteralType`/`DoubleLiteralType`（IntSupplier 式，互不关联、无共享根，`value()` 返回原语免装箱），`@JsonValue` 声明于家族接口、实现类继承即获标量 JSON 双向（Jackson 3.1.4 实证）；`EnumType` 收敛为 `StringLiteralType` 封闭子契约（`value()` 默认 = `name()`）；`VerificationRecipient<T extends StringLiteralType>` 边界随迁（String 家族），`target().value()` 落裸串不变、convertor 零改动；单属性 DP 全量收编（组件层 15 + soda-user 5，`AuthAccountId` 自描述编码与字面量契约正交一并收编，编码理由见 ADR-0007）；record/单 public 构造器 class 零 Jackson 代码，private 构造器 + 工厂（缓存/单例/解析）保留 `@JsonCreator`（构造入口不可继承，结构性必要）。
>
> 检视修订（2026-08-16，code review 后用户逐项确认）：① **V 终态标记撤回**——`VerificationState.V` 非终态（`terminal()=false`）：V 内存瞬态（verify→use 同事务）不落库、不涉槽位释放，`active_key` 清 NULL 仅 U 终态（本文正文「V/U 清 active_key」表述随之下调为「U 终态清」）；② **§7 userId 来源改为路径 `{id}` 注入**（资源级端点，与 §11 及 ADR-0021 §6 一致，非认证会话）；③ 工厂策略选择措辞修正——`UCC_CODE_POLICY` 为**场景方法内私有常量**，非「内联 switch」；④ §5 访问器措辞 `code()` → `name()`（框架约定序列化用枚举 name() 短名，ADR-0005）；⑤ §6 工厂标注 `@Component`（与实现一致，注册语义同 `@Service`）、source 映射方法名 `newCredentialChangeSource`（与实现一致）；⑥ **`VerificationCode.code` 存储形态 String 化确认**（用户裁决，接受现状不回退）——`code` 组件为 `String`：`from`/`matches` 入口签名仍收 `RandomString`（防伪造在边界成立；DB restore 非输入路径，构造器仅 `hasText` 无风险），DP 不持有生成包装（「不为包装而包装」，见 ADR-0018 再修订注记）；偏离 ADR-0018 应用案例与本文「code/state 不变」表述，正式记录于此。

## Context

- ADR-0025（2026-08-15）多态塌缩：`Verification` 单类、通道判别栖身密封 `Subject`、subject 必填、`active_key` = `scene:subject`、发码拓扑「发送主体是 Verification，User 是协助方」（ADR-0021）。
- 用户复盘（2026-08-16）三轮：
  1. Verification 被绑定它不需要的语义——subject 密封层级与 scene 枚举都是调用方词汇；Verification 行为太薄（策略选择移出后领域内零 channel 行为；send 是投递侧职责）。
  2. **主体裁定**：「发送验证码」的主体是**用户**（UCC/ULG/UPR 全为 user 场景；URG 唯一例外无用户）——「发送主体是 Verification」是拟人化措辞，撤销。Verification 是**协助方聚合**（码生命周期载体），「UserVerification」是用例概念（无类，由应用服务编排实现）。
  3. **subject 从唯一索引意图设计**：subject = 槽位占用者——UCC/ULG/UPR = userId（会话注入 / 端点反查），URG = 端点值（唯一例外）。

## Decision

### 0. 双概念总览 + 主体裁定

- **source**（`VerificationSource`，DP）：`{scene: String, subject: String}`，两属性均非空。**非空是 Verification 对 source 的唯一要求**——不感知语义、零行为耦合；聚合持有并暴露访问器（唯一键、消费反查、防御守卫使用）。
- **recipient**（`VerificationRecipient`，多态密封 DP）：`SmsRecipient(Mobile)` / `EmailRecipient(Email)`，`channel()` 派生。**channel 只在 VerificationRecipient**——subject 不携带；持久化 `channel` + `target` 双列显式承载（见 §2/§10）。
- **主体** = 用户（UCC/ULG/UPR：请求方 + 前置规则归属者；URG 无用户例外）；**Verification = 协助方聚合**（生命周期载体）；「UserVerification」= 用例概念（非类，应用服务编排实现）。
- Verification 感知边界：不解析 source（纯字符串）；channel 仅作 recipient 数据（投递分派在 infra、策略选择在工厂）——**领域内零 channel 行为**。

### 1. VerificationSource

- 属性均为 string：`scene` = 扁平助记码（`UCC`/`UPR`/`ULG`/`URG`，ADR-0005）；`subject` = **裸业务键**（无类型前缀）。
- **subject = 槽位占用者（唯一索引的身份维度）**：UCC/ULG/UPR = userId 裸键串（UCC 路径 `{id}` 注入、ULG/UPR 端点反查推导）；URG = 端点值串（无用户）。支撑不变量：同一 scene 内 subject 语义类型恒定（跨场景由 scene 消歧、同场景内类型由场景语义固定）。
- 校验：非空（非 blank）。`compositeKey()` = `scene + ":" + subject`——`active_key` 单一事实源（取代 ADR-0025 的 infra `compose` 单点函数；复合键推导非序列化——串从不解析回 DP，是单向派生的槽位唯一键）。

### 2. VerificationRecipient（多态 DP）

- 密封 `VerificationRecipient`，`SmsRecipient(Mobile)` / `EmailRecipient(Email)`；`channel()` 派生（`S`/`E`），构造无冗余参数。
- 多属性输出（2026-08-16 修订三/四）：两属性 `channel()` + 类型化 `target()`（修订四泛型多态：`T extends Type`，`SmsRecipient.target()` = `Mobile`、`EmailRecipient.target()` = `Email`）；JSON 对象形式（`{"channel":"S","target":"13800138000"}`——`channel()` 根访问器 `@JsonProperty`、组件 target 经地址 DP `@JsonValue` 序列化为裸串），反序列化经双参 `@JsonCreator`；**无规范字符串、无前缀路由**（`value()`/`of(String)` 删除）。
- 泛型多态 `target()`：类型参数 `T extends StringLiteralType`（组件层字符串字面量契约——修订六的 `LiteralType` 经 ADR-0028 重构为五家族，边界随迁 String 家族）由子类型以各自地址 DP 特化（`SmsRecipient`→`Mobile`、`EmailRecipient`→`Email`，record 组件即 target，隐式访问器满足接口方法）；投递侧直接取类型化地址，持久化侧 `target().value()` 落裸串（修订四/六/七——泛型捕获自带 value()，免判别）。
- 持久化：`channel` + `target` 两列（2026-08-16 修订：替代 recipient 自描述单列——「无独立 channel 列」撤销），restore 经 `VerificationRecipient.of(String, String)` 双参工厂按枚举分派（channel 枚举解析在工厂内单点，修订五）。
- `VerificationChannel` 枚举保留（普通枚举，不挂 Class 引用，ADR-0016）。角色：① 命令输入（预认证场景命令）；② VerificationRecipient 判别属性（码形策略按场景选择——UCC 专属策略双通道统一，非按通道默认，见 §6）。

### 3. Verification（单类，载体化）

- 字段 `scene/subject/target` → **`source: VerificationSource` + `recipient: VerificationRecipient`**；`code`/`state` 字段不变（`code` 存储形态 String 化确认——入口签名仍收 `RandomString`，见检视修订⑥）。
- 创建：`create(source, recipient, generator, VerificationCodePolicy policy)`——**policy 必传**（无 @Nullable、无内部 switch——Verification 不做通道→策略映射，策略决策属调用方）；码生成、状态 `I`、`VerificationCreatedEvent` 注册留在领域静态工厂（单构造路径，所有场景共用）。
- 行为 `verify/use/markSent/isExpiredAt` 与状态机 `I→P→V→U` 不变；过期派生判断不变；「PENDING 蕴含已送达」不变量不变。**领域内无任何 channel 行为**（verify/use/markSent 全通道同构）。
- 投递：监听器（AFTER_COMMIT）按 **recipient 类型**匹配 `SmsSender`/`EmailSender`（判别源从 subject 迁至 recipient）。

### 4. Subject 层级删除

- `Subject`/`UserSubject`/`MobileSubject`/`EmailSubject` 四个类型删除——通道判别迁至 VerificationRecipient；subject 降为 source 内裸字符串。
- 连带：按 subject 键控的契约（`findFirstBySubjectAndScene` 等）改 source 键控（见 Decision 8）。

### 5. VerificationScene → UserVerificationScene（更名，调用方词汇）

- 值域不变：`UCC`（User Credential Change）/`UPR`/`ULG`/`URG`，扁平助记码（ADR-0005）；位置不变（`soda-user-domain.types`）；`name()` 返回助记码串（= `VerificationSource.scene` 值；框架约定序列化用枚举 name() 短名，见 ADR-0005——2026-08-16 检视修订：原措辞 `code()` 与实现不符）。
- 语义变更：**调用方词汇**——Verification 不再引用（只存 scene 字符串）；工厂按它构造 source；**scene 不进 api 命令**（方法/端点即场景，客户端不可伪造）。
- 跨领域：未来 sys_user 场景用各自枚举 + `S*` 助记码（场景字符串隔离不变，ADR-0025 原则）。

### 6. 调用方构造：UserVerificationFactory

- 位置：application 层 `com.soda.user.application.factory`（已有空包），`@Component`（注册语义同 `@Service`，与实现一致，2026-08-16 检视修订），注入 `RandomStringGenerator`。
- **按场景的构造方法**（方法名即场景，scene 方法内写死，2026-08-16 会话修订）：`Verification newCredentialChangeVerification(UserId userId, VerificationRecipient recipient)`——**返回完整 Verification**（码生成 + 事件注册经领域静态工厂单构造路径）；source 映射点同为场景方法 `VerificationSource newCredentialChangeSource(UserId userId)`（scene=UCC 写死，槽位预检/消费反查与构造共用，杜绝服务侧两处映射漂移；方法名与实现一致，2026-08-16 检视修订）。
- **策略按场景而非通道选择**：UCC 专属码形策略（`UCC_CODE_POLICY`，工厂内私有常量）——换绑场景**无论通道（手机/邮箱）统一纯数字、短时效**（6 位数字 / 5 分钟过期），**不引用通道默认**（`DEFAULT_SMS`/`DEFAULT_EMAIL`——通道默认仍是通道级规则常量，但 UCC 用例不采用）。不抽共享 `forChannel` 方法（YAGNI；URG 落地出现第二构造方时再评估抽取）。策略决策属调用方场景，Verification 不参与。
- 边界：纯构造——业务前置（target ≠ 当前值、目标全局唯一、无活跃验证，跨实例查询）与持久化/事件发布留在 AppService。
- 范围：当前仅 UCC（subject ≠ recipient，subject 来自路径 `{id}`，见 §7）；端点场景（未来票）内联构造（URG: `VerificationSource.of(scene.name(), recipient 值)`；ULG/UPR: `VerificationSource.of(scene.name(), userId 串)`——userId 由端点反查）。

### 7. 服务拓扑：VerificationService 作废，发码归用户侧（修订 ADR-0021）

- **`VerificationService`（api 接口 + application 实现）删除**。理由：其存在前提「发送主体是 Verification」撤销；UCC 前置本就是 User 领域规则（启用态/target≠当前/全局唯一），跑在 Verification 服务里是主体错位；「统一入口」是为未来 URG/ULG/UPR 准备的投机设计（YAGNI）——真实场景（UCC/ULG/UPR）全是用户主体，归属用户侧服务；URG 唯一例外届时归注册服务。
- **UCC 发码迁入 `UserAuthService`**（发码 + 消费完整归位，内聚）：
  - `requestChangeMobileCode(RequestChangeMobileCodeCommand{userId, newMobile})` / `requestChangeEmailCode(RequestChangeEmailCodeCommand{userId, newEmail})`——userId 由 adapter 从路径 `{id}` 注入（资源级端点，与 §11 及 ADR-0021 §6 一致；**必填**，UCC 恒认证态——2026-08-16 检视修订，原「认证会话注入」与实现不符）；scene/channel 由方法隐式（UCC/S、UCC/E）。
  - 编排：前置（加载用户启用态 + 非终态 + target ≠ 当前值 + 目标全局唯一）→ `UserVerificationFactory.newCredentialChangeVerification(userId, recipient)`（**先构造**——source 取自已构造实体，服务层零并行推导）→ `existsBySource` 无活跃验证（**Assert 守卫**）→ save（convertor 设 `active_key`；**惰性 DELETE 过期 I/P 行收敛在 gateway 实现内**、同事务腾槽——基础设施实现细节，非领域契约）→ 发布 `VerificationCreatedEvent`；并发撞 `uk_active_key` → `DataIntegrityViolationException` **原样上抛**（预检 + DB 兜底，无翻译——2026-08-16 会话修订）。
- **api 按用例拆分**（Q5）：`RequestCodeCommand` 删除；per-use-case 命令（primitive 形状）；scene/channel 枚举不出 api——**api→domain 临时依赖解除**（0021 修订引入的悬案以消除需求的方式了结）。
- 消费流不变：`changePassword`/`changeMobile`/`changeEmail` 仍在 UserAuthService，经 `CredentialChangeDomainService`（verify → change → use）编排，同事务双 save。
- 未来票：ULG/UPR → 用户侧服务（前置：目标=已注册联系方式——**存在性校验即 subject 推导**，`findByMobile`/`findByEmail` 反查 userId）；URG → 注册服务（前置：目标未注册；subject=端点值，无反查）。

### 8. 唯一性与 Gateway 契约（机制不变，键源迁移）

- 唯一性不变量：同一 source 至多一条活跃验证——**终态不参与唯一**（与 User 同原则；机制不同：User R 键置 NULL，Verification U 终态清 `active_key`——V 内存瞬态不落库、不涉槽位，2026-08-16 检视修订）。**槽位语义 per-scene**：`(UCC, userId)` 防同用户对不同 recipient 发码；`(ULG/UPR, userId)` 防同用户手机+邮箱双通道并发；`(URG, phone)` 防同端点重复。
- `active_key` = `source.compositeKey()`；`uk_active_key` 唯一索引；I/P 占槽、U 终态迁移清 NULL（V 内存瞬态不落库不涉槽位）、过期 I/P 行惰性 DELETE 释放（**收敛进 gateway `save`**——INSERT 前同事务腾槽，基础设施实现细节，非领域契约；2026-08-16 修订：`active_key` 由 convertor 恒设，U 终态清 NULL 在 save 显式执行——检视修订：V 不再清键）。
- 契约 source 型：`existsBySource(source)` / `findLatestBySourceAndStateIn(source, states)`（`existsBy*` 命名规范不变：键存在性 + 唯一性约定，活跃判定为基础设施实现细节；惰性删除不设契约——2026-08-16 会话修订。消费反查命名<b>三维齐备</b>——latest（只返回最新一条，expire_at 倒序限 1）+ source + stateIn（候选状态集合）；前身命名不完整：`findFirstBySource` 缺 stateIn、`findBySource` 缺 latest；`uk_active_key` 保证每 source 至多一条 I/P 行，latest 语义在单行场景退化为取该行、仍显式表达防退化，见 framework-conventions §查询契约命名）。

### 9. 消费守卫

- `User.changeMobile(Verification)` / `changeEmail(Verification)` 守卫：`source.subject()` 与 userId 裸键串相等（+ scene 为 UCC 助记码）——防御纵深保留（find-by-source 已按构造精确，守卫是廉价兜底，ADR-0025 意图延续）。

### 10. 持久化

- 列：`subject`（裸键，无前缀）、`scene`（助记码）、`channel` + `target`（2026-08-16 修订：替代 recipient 自描述单列——「无独立 channel 列」撤销，判别源从 subject 前缀迁至 recipient，双列显式承载）、`code`/`state`/`expire_at`/`active_key`/时间列不变。
- V1 直改（ADR-0022，开发库），无数据迁移。

### 11. 端点（预认证/无资源场景 = 集合级自定义方法）

- UCC（认证态）：现状资源级不变——`POST /api/users/{id}:requestChangeMobileCode` / `:requestChangeEmailCode`（ADR-0009 规则 3：`{id}` 在路径）。
- ULG/UPR/URG（预认证/无用户）：**集合级自定义方法**——`POST /api/users:requestLoginCode` / `:requestPasswordResetCode` / `:requestRegisterCode`，body `{channel, contact}`（无 `{id}`——资源尚不存在，contact 是操作输入非资源 id；「资源标识符在路径上」规则因无资源而不适用）。
- 合规依据（AIP-136 原文，非推测）：① 总则「Custom methods can be associated with **resources, collections, or services**」；② Collection-based 专节「some custom methods **may** operate on a collection instead」（示例 `{parent=publishers/*}/books:sort`）；③ parent 规则**条件性**——「**If** the collection's resource has a parent, that resource must be called `parent`」——顶层集合（无父）无此变量，集合键字面量。规范边界：AIP-136 示例均带变量（name/parent/scope），无 parent 顶层集合形态正文未直接示例——由条件规则 + Google 生产先例支撑。
- **Google 生产先例**（已查证）：Firebase Identity Toolkit `POST /v1/accounts:signUp`、`POST /v1/accounts:sendOobCode`（发验证码：PASSWORD_RESET/VERIFY_EMAIL/EMAIL_SIGNIN/VERIFY_AND_CHANGE_EMAIL）、`POST /v1/accounts:resetPassword`——无 parent 顶层集合级自定义方法，正是本形态。注：Firebase 用单端点 + `requestType`（scene 进 body）的通用形态；本项目选 per-use-case 动词（Q5），两者都合规，纯产品取舍。

## Considered Options

| 方案 | 结论 |
|---|---|
| recipient 单属性字符串输出（自描述 `S:13800138000`，`@JsonValue` + 前缀路由） | **修订三（2026-08-16 用户决策）**——多属性对象输出（channel + target 双属性），弃规范字符串/前缀路由；AuthAccountId 同构仅保留判别分派与通道语义 |
| subject 带类型前缀（`U:42`）承载通道判别 | 否决（用户决策）——通道语义归 VerificationRecipient，subject 纯裸键 |
| recipient 判别走独立 channel 列（0025 删除列回归） | **修订（2026-08-16 用户决策）**——channel + target 双列持久化（弃 recipient 自描述单列）；restore 由领域双参工厂 `of(String, String)` 按枚举分派（修订五：双字符串，枚举解析收敛进工厂），跨列不变量收敛进 convertor 双列映射 |
| ULG/UPR subject=端点值（0025 原「端点场景 subject 即 target」） | **修订**——subject=userId（槽位占用者：防同用户手机+邮箱双通道并发）；仅 URG 保留端点值 |
| 保留 VerificationService（统一入口 / 收窄为 issuer） | 否决（用户决策）——主体=用户，发码按场景归属用户侧服务；统一入口是投机设计（YAGNI） |
| api 统一命令（RequestCodeCommand 保留全参，scene 进命令） | **修订**——api 按用例拆分，per-use-case 命令，scene/channel 隐式（方法即场景），客户端不可伪造 |
| policy 由 Verification.create 缺省解析（Q8 初版） | **修订**——policy 必传，默认 UCC_CODE_POLICY 场景方法内私有常量（策略决策属调用方）；不抽 forChannel（YAGNI，唯一构造方） |
| 集合级端点挂 parent（`{parent=...}` 形态） | 不适用——users 是顶层集合无父；parentless 形态由 AIP-136 条件规则 + Firebase 先例支撑 |

## Consequences

- **代码删除**：`Subject`/`UserSubject`/`MobileSubject`/`EmailSubject`/`VerificationScene`/`VerificationService`（api 接口 + application 实现）/`RequestCodeCommand`。
- **代码新增**：`VerificationSource`/`VerificationRecipient`/`SmsRecipient`/`EmailRecipient`/`UserVerificationScene`/`UserVerificationFactory`（application.factory，policy 场景方法内私有常量——非「内联 switch」，2026-08-16 检视修订）；`UserAuthService` 增 `requestChangeMobileCode`/`requestChangeEmailCode` + `RequestChangeMobileCodeCommand`/`RequestChangeEmailCodeCommand`。
- **代码修改**：`Verification`（字段 source+recipient、create 必传 policy）；`VerificationGateway`/`VerificationGatewayImpl`（契约 source 型、save 终态（U）清 active_key——V 内存瞬态不落库不涉槽位，2026-08-16 检视修订）；`VerificationConvertor`/`VerificationPO`（channel+target 双列、subject 裸键）；`UserController`/`UserWebAssembler`（改调 UserAuthService）；`VerificationCreatedEventHandler`（按 VerificationRecipient 类型分派）。
- **构建**：api→domain 临时依赖解除（0021 悬案了结；实施时确认无其他引用）。
- **表**：`subject` 裸键、`recipient` → `channel` + `target` 双列；`active_key`/`uk_active_key`/查询索引不变。
- **文档**：ADR-0021/0025 取代注记、ADR-0012 集合级自定义方法注记、research doc §3.7 补一行、CONTEXT.md 词条重写。
- **接缝（未来票）**：ULG/UPR（`findByMobile`/`findByEmail` 反查 userId、集合级端点、policy 第二构造方出现时评估抽取 forChannel）、URG（注册服务、subject=端点值）、sys_user 场景（`S*` 场景串 + 各自枚举）、跨领域抽取——Verification 现已零用户概念类型依赖（source 纯字符串、recipient 包 component 层 `Mobile`/`Email`）。

## 依据

- AIP-136 原文（google.aip.dev/136，实抓）：三类挂载点声明、Collection-based 专节、条件性 parent 规则、动词命名规则（POST 副作用 / camelCase / 无介词 / 无标准动词）。
- Firebase Identity Toolkit 文档（docs.cloud.google.com/identity-platform/docs/reference/rest）：`POST /v1/accounts:signUp`、`accounts:sendOobCode`、`accounts:resetPassword` 生产先例。
- ADR-0007（AuthAccountId 集中 `@JsonCreator` 前缀路由）、ADR-0016（class↔channel 映射归基础设施）、ADR-0021（聚合根判据与消费机制保留部分）、ADR-0025（机制遗产全部保留）。
- 用户逐项决策（2026-08-16 grill-with-docs 会话两轮）：Q5 裸键、Q9 应用层工厂、Q10 工厂返回完整 Verification、Q12→Q5 命令全参→按用例拆分、Q15 channel 只在 VerificationRecipient、主体裁定（User）、ULG/UPR subject=userId、policy 必传 + UCC_CODE_POLICY 场景方法内私有常量（forChannel 否决）、URG 集合级端点。
