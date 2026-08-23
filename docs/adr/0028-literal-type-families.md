# 字面量家族契约：五家族 LiteralType 重构（@JsonValue 继承 + EnumType 收编）

将单接口 `LiteralType`（`String value()`，ADR-0026 修订六）重构为互不关联的五家族契约 `StringLiteralType`/`LongLiteralType`/`IntLiteralType`/`BooleanLiteralType`/`DoubleLiteralType`（IntSupplier 式，无共享根），`@JsonValue` 声明于家族接口的 `value()` 上、实现类继承即获标量 JSON 序列化与反序列化（Jackson 3.1.4 实证双向）——record 实现类与单 public 构造器 class 零 Jackson 代码；`EnumType` 收敛为 `StringLiteralType` 封闭子契约（`value()` 默认 = `name()`）；单属性 DP 全量收编，`VerificationRecipient<T extends StringLiteralType>` 边界随迁，持久化 `target().value()` 落裸串不变。发起：用户复盘（2026-08-16 grill-with-docs 会话）——「当前 LiteralType 只支持 String，字面值 JSON 都是基本类型 + String，需要覆盖原语且免装箱/拆箱，重复的 `@JsonValue` 访问器样板应省略」。

> 修订（2026-08-16，用户复盘）：**`EnumType extends StringLiteralType` 收窄为 `EnumType extends Type`，`value()` 默认实现删除**——三条支撑论据经检验均不成立：① `@JsonValue` 继承对枚举是装饰性的（Jackson 原生枚举序列化即 `name()`，输出不变；反序列化走各枚举显式 `@JsonCreator`）；② 统一 `value()` 访问器全仓零调用点（业务层一律 `.name()`：`UserDTOConvertor`/`UserVerificationFactory`/`VerificationConvertor`）；③ 泛型边界参与（`VerificationRecipient<T extends StringLiteralType>`）无枚举 target——正文「YAGNI」已预留放宽路径。收窄代价为零（无调用方、序列化行为不变），换取去掉 `((Enum<?>) this)` 强转与家族耦合；枚举与五家族恢复纯平行（与「互不关联、无共享根」原则一致）。未来枚举 target 出现时放宽边界，届时改动仍是一行 `extends`。

## 决策

- **五家族、互不关联、无共享根**（IntSupplier 式）：每个家族接口 `extends Type`、声明 `@JsonValue X value()`（`value()` 返回原语，免装箱/拆箱）；泛型 `LiteralType<T> { T value(); }` 被否（装箱）。`LiteralType` 单接口删除，名称「字面量」保留于家族名与文档。
- **`@JsonValue` 继承即标量双向**：Jackson 3.1.4 实证——注解声明在接口方法上，实现类（含 record 隐式访问器、Lombok 生成访问器、class 手写访问器）继承后，标量序列化与反序列化同时生效（record 单组件、class 单参构造器均可反序列化，`{}`/对象形式仍被拒）。依赖版本行为（3.1.4 实证），作为框架契约固定于 family 接口 javadoc。
- **`EnumType extends StringLiteralType`**：枚举 = 封闭的单属性字符串字面量 DP（字面值 = 短名 `name()`）；`default value()` 经 `((Enum<?>) this).name()`（框架内部 1 行，EnumMap/EnumSet 同款惯用法，契约限定实现类为枚举）；`@JsonValue` 继承使序列化仍为 `name()`（与 Jackson 枚举默认输出一致，行为不变）；`StateEnumType extends EnumType` 传递继承。7 个业务枚举零改动。
- **creator 收编边界**：record 与单 public 构造器 class 零 Jackson 代码（继承 `@JsonValue` 覆盖双向）；private 构造器 + 工厂（缓存/单例/解析）的 class 保留 `@JsonCreator`（构造入口不可继承、private 构造器 Jackson 不可见——结构性必要，注解挂在本来就存在的 `of()` 上）。显式声明哲学维持（dp-conventions「不依赖推断」修订为「record/单构造器 class 依赖继承 + 推断，带构造逻辑工厂保留显式 creator」）。
- **全量收编（20 个 DP）**：组件层 `StringLiteralType`（Mobile、Email、Alphabet、RandomString、SmsContent、CredentialHash、UUId、Percentage、WanYuan、SoftwareVersion）、`LongLiteralType`（LongId）、`IntLiteralType`（Fen、Version、PositiveInt）、`BooleanLiteralType`（Active）；soda-user `StringLiteralType`（Username、Nickname、Avatar、AuthAccountId 密封基类）、`LongLiteralType`（UserId）。`DoubleLiteralType` 契约就位无实现。
- **排除**：`Secret`/`RawCredential`（故意不可序列化，`rawValue()` 反序列化访问器名）；多属性 DP（VerificationCodePolicy、VerificationCode、VerificationSource、EmailContent 等）。`AuthAccountId` 的自描述编码（`P:42`）与字面量契约正交——单属性 String、标量 JSON、delegating creator 三条件满足，一并收编（字段级 `@JsonValue` 删除，编码理由仍见 ADR-0007）。
- **`VerificationRecipient<T extends StringLiteralType>`**：仅 String 型 target 存在（YAGNI，未来原语/枚举 target 出现时放宽边界）；convertor `target().value()` 零改动（编译路径不变）。

## Considered Options

| 方案 | 否定原因 |
|---|---|
| 泛型 `LiteralType<T> { T value(); }` | `T` 对原语装箱/拆箱，直接违反免装箱需求 |
| 密封根 + 家族 + 根单点 switch `rawValue()` | 用户否：家族互不关联（IntSupplier 式）；rawValue 访问器多余（Secret 是唯一特例）；密封锁定家族集 |
| 根 marker `LiteralType`（无方法）作泛型锚点 | 用户否：无关联家族各自 `extends Type`；Q6-B 边界已用 StringLiteralType |
| 单一接口 + 每 DP 手写 `@JsonValue`（现状） | 样板重复；String-only 无法表达原语字面量 |
| `StringType`/`LongType` 命名（去 Literal） | 用户否：保留 Literal 词（EnumType 平行语义）；`StringType extends Type` 的「Type-Type」读感也差 |
| 每个枚举手写 `value()` | 样板；`((Enum<?>) this).name()` 1 行收敛进框架 |
| 全删 class `@JsonCreator` 依赖推断 | private 构造器 Jackson 不可见（实证），缓存/单例/解析路径会被绕过；显式声明哲学维持 |

## Consequences

- 14 个 DP 变零 Jackson 代码（9 record + Email + Username/Nickname/Avatar/UserId/AuthAccountId 基类），6 个 class 保留 1 行 `@JsonCreator`；序列化输出全不变（行为中立，现有测试即验收）。
- `@JsonValue` 继承行为依赖 Jackson 3.1.4+（3.1.x 实证）；升级 Jackson 时需回归标量双向契约（family javadoc 已固化）。
- dp-conventions §5 模式表重写（「record 单字段必须 `@JsonCreator(DELEGATING)`」删除——实证过时）；framework-conventions 补字面量家族条目；CONTEXT.md VerificationRecipient 词条随迁；ADR-0026 修订六的 LiteralType 契约被本 ADR 取代（修订七注记）。
- 枚举获得统一 `value()` 访问器（替代散落的 `name()`/`code()` 混用）；`VerificationRecipient` 边界收窄至 String 家族——原语 target 出现时放宽（届时 convertor 需落串转换，属正常演进）。
