# Masked Value DPs 与命名：Redacted→Masked、SensitiveLiteral→SensitiveValue

本 ADR 取代 ADR-0030 §4（Redacted DP 层级）。背景来自对 DP 防护家族的 usage-map 审查（2026-08-23）。

## Context

ADR-0030 为四类防护模式（Raw+Maskable / Encrypted / Hashed / Redacted）各建统一层级。审查发现 Redacted 家族存在仪式性冗余与自相矛盾：

- **仪式层零消费者**：`RedactedValue` 密封接口与 `AbstractRedactedValue` 抽象类在整个 `soda-components` + `soda-user` 中无跨模块生产消费者——仅自身模块 `main` + `test` 引用，无任何 `soda-user` 生产代码使用。
- **`redacted()` 零调用**：原始值 DP（`Mobile`/`Email`/`IdCard`/`BankCard`/`RealName`）持有的 `redacted()` 方法在整个代码树中调用次数为 0（生产、测试均无）。
- **接口 default `maskedValue()` 不可达**：`RedactedValue` 提供的 `maskedValue()` 默认方法因 JLS class-wins 规则被 `AbstractRedactedValue` 强制覆写，永远不可达——接口本身纯属仪式。
- **急切缓存与同 ADR「缓存纯函数是纯开销」自相矛盾**：原始值 DP 为每个实例急切缓存一个 `RedactedXxx` 脱敏实例（`private final RedactedXxx masked`），违背 ADR-0030 自身 §Removed「缓存纯函数是纯开销」的结论。

新需求事实：**已脱敏值（masked）会落库用于展示场景**，因此脱敏串的格式正则校验在 `of()` 读回时是承重的（必须从持久化值重建并校验，而非仅构造期一次）。

## 决策

### 1. Redacted 家族重构为 Masked 记录
- 具体 DP 由 `class RedactedXxx` 改为 `record MaskedXxx implements StringLiteralType`（`MaskedMobile`/`MaskedEmail`/`MaskedIdCard`/`MaskedBankCard`/`MaskedRealName`）。
- 删除密封接口 `RedactedValue` 与抽象类 `AbstractRedactedValue`。
- 原始值 DP（`Mobile` 等）**移除缓存的脱敏实例与 `redacted()` 方法**；`maskedValue()` 改为**委托**到对应 `MaskedXxx.maskOf(String)`。
- 掩码算法保持单一事实源：作为 package-private `static String maskOf(String)`，与格式正则**同址**于各 `MaskedXxx` 记录内；`MaskedXxx.from(RawXxx)` 与 `RawXxx.maskedValue()` 都经它派生。

### 2. 重命名
- `Redacted*` → `Masked*`（对齐「脱敏 / maskedValue」词汇，消除 redact/mask 混用）。
- `SensitiveLiteral` → `SensitiveValue`（去掉实现味的 "Literal"，避免与 `StringLiteralType` 家族命名碰撞；语义/契约不变）。
- `SecretValue` 不变。

### 3. 需求落地：脱敏值落库、格式校验承重
`MaskedXxx` 是持久化存储形态（展示场景落库），构造器/`of()` 用格式正则校验脱敏串；读回路径 `of(String)` 重建即校验，保证落库值不可篡改/非法。

### 4. 取舍与理由
- 脱敏值**不敏感**：不含原始 PII 明文，无需 `SensitiveValue` 继承（不继承即不被 `toString` 脱敏包裹，也不强制 final 基类）；`record` 即满足 dp-conventions「简单 DP 用 record」(§2.2) 与 STYLEGUIDE record-first。
- **9 类型 → 5 记录**：原 Redacted 家族（`RedactedValue` 接口 + `AbstractRedactedValue` 抽象类 + 5 个 `RedactedXxx` 类）及关联仪式一并移除，由 5 个 `MaskedXxx` 记录取代。
- **死代码消除**：不可达的接口默认 `maskedValue()`、零调用 `redacted()` 一并删除。
- 解决 ADR-0030 自相矛盾：原始值 DP 不再急切缓存脱敏实例（缓存纯函数=纯开销），`maskedValue()` 走无状态 `maskOf`。

调用链（单一掩码事实源）：
- **写侧**：`MaskedMobile.from(Mobile)` 经 `maskOf` 派生并构造，构造器用格式正则校验脱敏串。
- **日志**：原始值 DP `toString()` → `SensitiveValue.toString()` 调用 `maskedValue()` → 委托 `maskOf`。
- **读侧**：`MaskedXxx.of(persistedMasked)` 从持久化脱敏串正则重建（承重校验）。
- **预留缝**：未来若出现跨 `MaskedXxx` 的泛型消费（如统一脱敏展示 DTO），再补共享接口/基类——当前刻意不引入，避免重蹈 §4 仪式层覆辙。

## Considered Options

| 方案 | 否定原因 |
|---|---|
| 保留 `RedactedValue` 接口 + `AbstractRedactedValue`，仅把具体类改 record | 接口默认 `maskedValue()` 仍不可达（JLS class-wins 必须覆写）、`redacted()` 仍零调用；仪式层零消费者问题未解 |
| 保留抽象基类 `AbstractRedactedValue`、删接口 | 抽象基类仍需继承约束，与「脱敏值不敏感、用 record」相悖；原始 DP 缓存矛盾仍在 |
| 保留原始 DP 急切缓存脱敏实例 | 直接违背 ADR-0030 §Removed「缓存纯函数是纯开销」；无生产消费者证明缓存无收益 |
| 当前方案（删接口+抽象类、5 记录、原始 DP 委托 maskOf） | 单一事实源、record 免费语义、消除死代码与自相矛盾；唯一代价：跨类型泛型消费需未来补接口（YAGNI 预留缝） |

## Consequences

- **9 → 5 类型**：Redacted 家族 7 个类型（接口+抽象类+5 具体类）与关联仪式一并移除，由 5 个 `MaskedXxx` 记录取代；与 `SensitiveLiteral`→`SensitiveValue` 重命名合并计入，敏感/脱敏相关类型净减（原 9 → 5 记录）。
- **record 免费语义与 dp-conventions §2.2 对齐**：不可变、`value()`/`equals`/`hashCode` 由 record 免费提供，脱敏串即 `value()`，无需 Lombok/手写；满足 STYLEGUIDE record-first（消除 ADR-0030 对「简单 DP 用 record」的敏感 DP 豁免）。
- **三层词汇定界**：`SecretValue`（瞬态凭证载体，永不展示）/ `SensitiveValue`（长期 PII 基类，脱敏后展示）/ `Masked*`（已脱敏存储形态，落库展示）——同一防御栈的三层边界清晰，互不混淆。
- **ADR-0030 §4 被本 ADR 取代**：`SensitiveLiteral`→`SensitiveValue` 重命名同步生效；ADR-0030 其余（Encrypted / Hashed 层级、Gateway 契约）不变。
- **矛盾消解**：原始值 DP 不再缓存脱敏实例，缓存纯函数开销消除；`maskedValue()` 走无状态 `maskOf`，与 ADR-0030 §Removed 自洽。
- **读回承重**：`MaskedXxx.of()` 格式正则校验保证落库脱敏值合法，支持展示场景往返。

## 修订注记（2026-08-23，代码检视落地）

1. **`maskOf(String)` 收敛为 `private`**——仅服务本类 `from(RawXxx)`；原始值 DP（同包）不再直接调用掩码算法。
2. **派生通道唯一化**：Raw DP 的 `maskedValue()`（`SensitiveValue.computeMasked()` 实现）改为 `MaskedXxx.from(this).value()`；`from` 是原始值 → 脱敏值的唯一公开入口。
3. 本 ADR 正文 §Decision/§Consequences 中「package-private」「maskedValue() 委托 maskOf」的表述以本注记为准。
4. **`RealName`→`ChineseName`、`MaskedRealName`→`MaskedChineseName`**：校验契约本就仅接受汉字（`^[\u4e00-\u9fa5]{2,20}$`），"RealName" 命名过度承诺；对齐主流词汇（hutool `chineseName`、yudao `@ChineseNameDesensitize`）。
5. **姓名掩码统一为"保留首字、其余打码"，删除复姓表**：与主流实现完全一致；复姓表收录不全导致复姓之间行为不一致（欧阳→`欧阳*`，上官→`上**`），而两种行为安全性等价，取无特例的均匀规则。`MaskedChineseName` 格式正则相应收紧为单字前缀（`^[\u4e00-\u9fa5]\*+$`）；零持久化数据，收紧无迁移成本。
6. **成对引入原则**：每个 `MaskedXxx` 必须有对应 raw DP 承载校验与原始语义（`from(RawXxx)` 为唯一派生入口），不接受无原始类型的孤儿掩码类。
7. **`SensitiveValue` 收敛为单抽象方法**：`public final maskedValue()` + `protected abstract computeMasked()` 的双方法结构随懒缓存的移除失去载体（包装层不再承载任何策略），合并为单个 `public abstract String maskedValue()`；子类直接实现公开钩子，`toString()` 的 final 约束与输出格式不变。
