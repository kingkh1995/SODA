# wire≠semantic 字面量扩展：DecimalLiteralType 子契约 + EpochMilli（绝对时间点 DP）

ADR-0028 的五家族有一个隐含不变量：`value()` 既是领域规范值、又是 JSON 线上值（语义值 == 线上原语）。小数（`BigDecimal` 语义、`String` 线上以防精度丢失）与绝对时间点（`Instant` 语义、`long` 毫秒线上）打破这个巧合——**语义值 ≠ 线上值**。本 ADR 决定：这类「wire≠semantic」字面量 DP 仍归其**线上类型**所在家族，`value()` 返回线上类型（承载继承的 `@JsonValue`），语义值作第二访问器 + 派生字段。落两个形态：小数加 `DecimalLiteralType extends StringLiteralType`（+ `BigDecimal decimalValue()`，家族层**首个子契约**），绝对时间点建通用 `EpochMilli` DP 直挂 `LongLiteralType`（`long` 毫秒 + 派生 `Instant instant()`，**不建** `TimestampLiteralType` 子接口）。两者不共享基类。

发起：用户复盘（2026-08-21 grill-with-docs 会话）——「BigDecimal 作 String 线上、Instant 作 long 线上是特殊的 StringLiteralType / LongLiteralType，是否要通用的基类/基接口」。

> 修订（2026-08-23，用户决定）：**撤销 `DecimalLiteralType` 子契约接口、收敛为单类型**——仅有的两个实现者均经抽象基类到达、全仓零多态调用点，「多实现者回本」判据未兑现，按「无场景不预设」合并：删除 domain-starter 接口，domain-types 抽象基类更名为 `DecimalLiteralType`（直接 `implements StringLiteralType`），契约与缓存不变量同置一类。`@JsonValue` `value()` 链路与序列化输出不变；`decimalValue()` 访问器由该类承载。未来出现不经基类的简单小数 DP（record 直挂家族，如 `Fen`/`EpochMilli` 形态）时再评估恢复独立契约。

## 决策

- **wire≠semantic 原则**：线上标量类型 ≠ 语义值时，`value()` 返回**线上类型**（String/long），`@JsonValue` 继承自所属家族接口；语义值（BigDecimal/Instant）为**第二访问器 + 派生字段**，不参与序列化/`equals`/`hashCode`。无干净替代：`@JsonValue` 在 `BigDecimal value()` 上会序列化为 JSON **数字**（击穿防精度目标）；在 `Instant value()` 上得 Jackson 默认 ISO-8601 串（非 long）。家族 = 线上类型（ADR-0028 原则），故小数属 String 家族、时间属 Long 家族——用户的「特殊 StringLiteralType / 特殊 LongLiteralType」提法成立。

- **`DecimalLiteralType extends StringLiteralType`**（接口，domain-starter，+ `BigDecimal decimalValue()`）：小数 DP 的子契约。`value()` 继承 String（`@JsonValue` → 标量双向），`decimalValue()` 返回派生 BigDecimal。**家族层首个子契约**——`WanYuan`/`Percentage` 实现之。
- **`AbstractDecimalLiteralType`**（抽象基类，domain-types，`implements DecimalLiteralType`）：集中「wire≠semantic」缓存不变量——规范值 `String value`（`@JsonValue`，继承自家族）+ 派生 `BigDecimal decimalValue`；构造期规范化（nullCheck→maxScale→setScale(UNNECESSARY)→`validate` 钩子）；`final value()`/`decimalValue()`；`getClass()`-based `final equals`/`hashCode`（类型作用域——`WanYuan("1.00")≠Percentage("1.00")`）；`protected validate(BigDecimal)` 钩子（`Percentage` 覆写加 `[0,100]` range）。`WanYuan`/`Percentage` `extends` 之，剥除字段/Lombok/手写 `value()`/`equals`，仅留 `SCALE` + 构造器(`super`) + `validate` + 工厂 + 单位换算。镜像 `AbstractEncryptedValue` 既有「abstract 基类 implements 家族接口」先例（domain-types 既有惯例，非破例）。`@JsonValue` 跨三层（接口→基类 `final value()`→子类）经 `AbstractDecimalLiteralTypeTest` 实测生效；序列化输出不变（仍是 `toPlainString()`）。

- **`EpochMilli` DP**（通用绝对时间点）：`record EpochMilli(long value) implements LongLiteralType, Comparable<EpochMilli>`，`long value`（epoch 毫秒，规范值，`@JsonValue` 继承）+ 派生 `Instant instant()`（`Instant.ofEpochMilli(value)`，毫秒精度互逆——亚毫秒截断，毫秒单位契约；富血 `of`/`now`/`plus`/`minus`/`isAfter`/`isBefore`）。单位 = 毫秒（主流 `System.currentTimeMillis`/JS `Date.now`/Go `UnixMilli`、保亚秒精度、外部契约多期望毫秒）。直挂 `LongLiteralType`，**不建 `TimestampLiteralType` 子接口**——子契约只在多实现者时回本（小数有 2、时间只 1 个通用 DP）。当前契约就位、暂无生产消费方；现有裸 `Instant` 字段迁移见 Consequences。

- **不共享基类**：小数线上 String、时间线上 long，语义分别为 BigDecimal/Instant；唯一共性「wire≠semantic、两访问器」太薄；无方法根 marker ADR-0028 已否；`RichLiteralType<W,S>` 双值基为 ≤2 成员搭过载抽象。各 `extends` 各自线上家族，语义访问器各归各家。

- **Jackson 3 Instant 默认订正**：`dp-conventions §5.1` 原记「Instant 输出数字时间戳（epoch seconds）」**有误**——Jackson 3 把 `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` 默认改为 `false`（Jackson 2 为 `true`），裸 `Instant` 默认序列化为 **ISO-8601 字符串**（`"2026-08-21T12:00:00Z"`），非数字；即便启用 timestamps，默认也是 epoch 秒 + 小数纳秒，非毫秒。`@JsonValue long value()`（EpochMilli）覆盖此默认，且不受全局 feature flag 影响（经 Jackson 3 源码实证：`JsonValueSerializer` 只序列化 `@JsonValue` 访问器返回值、忽略其他字段——`Instant` 派生字段不泄漏）。`JavaTimeModule` 已内建自动注册（原文档对）。来源：[`DateTimeFeature.java:210`](https://github.com/FasterXML/jackson-databind/blob/3.x/src/main/java/tools/jackson/databind/cfg/DateTimeFeature.java#L210)、[`InstantSerializerBase.java:95-104`](https://github.com/FasterXML/jackson-databind/blob/3.x/src/main/java/tools/jackson/databind/ext/javatime/ser/InstantSerializerBase.java#L95-L104)、[`JsonValueSerializer.java:305-320`](https://github.com/FasterXML/jackson-databind/blob/3.x/src/main/java/tools/jackson/databind/ser/jackson/JsonValueSerializer.java#L305-L320)。

## Considered Options

| 方案 | 否定原因 |
|---|---|
| `value()` 返回语义值（BigDecimal/Instant）+ 另设线上 `@JsonValue` 访问器 | 家族接口无法在 `value()` 上声明 `@JsonValue`（BigDecimal→JSON 数字击穿防精度；Instant→ISO 串非 long）；破坏「`@JsonValue` 在家族 `value()` 上」契约 |
| 小数独立家族 `DecimalLiteralType extends Type`（非子契约） | 重复 `StringLiteralType` 的 `@JsonValue String value()` 契约；破坏「家族=线上类型」原则；`VerificationRecipient<T extends StringLiteralType>` 收不到独立家族成员 |
| 时间也建 `TimestampLiteralType extends LongLiteralType` 子接口 | 仅 1 个实现者（通用 `EpochMilli`），YAGNI；与子契约回本判据（多实现者）相悖 |
| 小数 + 时间共享基类（marker 或 `RichLiteralType<W,S>`） | 线上类型不同（String/long）、语义类型不同；共性太薄；marker 根 ADR-0028 已否；双值基为 ≤2 成员搭过载抽象 |
| 时间用裸 `Instant` + 全局 Jackson 配置（启用 `WRITE_DATES_AS_TIMESTAMPS` + 关 nanos）出毫秒 | 全局 feature 影响所有 Instant（含审计列）；非类型安全；单位不受 DP 边界保护；与「字段为 DP 或基本类型 + DP 映射」相悖 |
| 时间单位 = 秒 | 丢亚秒精度、与主流（毫秒）相悖 |

## Consequences

- `DecimalLiteralType` 是 ADR-0028 家族层**首个子契约**——引入层级。**不违反 0028「无共享根」**：0028 否的是五**兄弟**家族共享根（`LiteralType` 单接口 / 泛型 / 密封根 / marker），子契约是在**单一**家族内特化（线上类型仍是 String，仅加语义访问器），与「家族=线上类型」原则一致。`EnumType extends Type`（2026-08-16 收窄、非 StringLiteralType）保持平行。

- `WanYuan`/`Percentage` `extends AbstractDecimalLiteralType`（`bigDecimalValue`→`decimalValue`、剥除字段/Lombok/手写 `value()`/`equals`，零外部 callsite，2 文件内部）。序列化输出不变（仍是 `toPlainString()`）；`WanYuanTest`/`PercentageTest`/`AbstractDecimalLiteralTypeTest` 全绿验收（含跨类不相等、缓存不变量、`@JsonValue` 跨三层标量序列化）。

- `dp-conventions §5.1` Instant 行订正（ISO-8601 串默认，非 epoch 秒）；§5.2 BigDecimal 模式补注「实现 `DecimalLiteralType`」。

- **迁移（wire-breaking，另开一轮，未在本 ADR 决定）**：现有裸 `Instant` 字段（`DomainEvent.occurredAt`、`VerificationCode.expireAt`、`Verification.verify/isExpiredAt` 入参）若改用 `EpochMilli`，线上格式从 ISO-8601 字符串翻成 long 毫秒——对既有 JSON 契约/测试是 breaking change。**审计列**（`AbstractAuditable.createdDate/lastModifiedDate`）属基础设施，保持裸 `Instant`（非领域 DP，与 `VerificationPO.expireAt` 同——列存 UTC 字面值）。2026-08-21 用户决定暂不迁移现有 `Instant`（`EpochMilli` 当前无生产消费方），范围与是否全量翻新留待后续轮。

- `EpochMilli` 的 `long` 规范值 + 派生 `Instant` 形态经 Jackson 3 源码实证安全（`@JsonValue` 访问器完全控制序列化、忽略其他字段）。`EpochMilliTest` 实测：裸 long 标量序列化、round-trip、拒对象形式、毫秒精度互逆/亚毫秒截断均通过。
