# RandomStringGenerator 字符集参数化 — Alphabet DP

`RandomStringGenerator` 契约从 `generate(PositiveInt)` 改为 `generate(PositiveInt, Alphabet)`：字符集从"基础设施层实现细节"升格为领域概念（`com.soda.component.domain.types.Alphabet` DP，开集字符串，非枚举）。原契约显式声明"字符集和随机源由基础设施层决定，领域层只关心长度"（CONTEXT.md / framework-conventions.md / 两侧 Javadoc）——该断言被反转：验证码需求（短信纯数字、邮箱字母数字）要求领域层表达字符集意图。`VerificationCodePolicy` 相应重构为**嵌套 DP 值对象**——`codeLength`（`PositiveInt`）、`expiry`（`Duration`）、`codeAlphabet`（`Alphabet`）三要素一体，`DEFAULT_SMS`=6位/5分钟/DIGITS、`DEFAULT_EMAIL`=8位/30分钟/UNAMBIGUOUS_ALPHANUMERIC。

> 再修订（2026-08-11）：`ALPHANUMERIC`、`LETTERS` 便捷常量移除，新增 `UNAMBIGUOUS_ALPHANUMERIC`（`"23456789ABCDEFGHJKLMNPQRSTUVWXYZ"`——数字 2-9 + 大写字母去 I/O，剔除 0/1/I/O 形近字符，仅大写，跟随兑换码/恢复码主流样式）；`DEFAULT_EMAIL` 改用之（码长/有效期不变）。

## 决策

- **Alphabet 是 DP（开集）而非枚举**：字符集必须是任意字符串（`"0123456789"`、自定义去易混淆集），闭集枚举无法表达"任意字符集"；仓库的短名枚举惯例（ADR-0005）针对类别型概念，Alphabet 是值不是类别。`DIGITS`/`UNAMBIGUOUS_ALPHANUMERIC` 常量只是便捷默认，不构成封闭集合。
- **DP 只做纯索引映射**：严格 `charAt(int)`（0 ≤ index < size，越界 IAE，失败即暴露生成器 bug）；随机源留在生成器（`SecureRandom.nextInt(size)`，拒绝采样无偏）。否定"hashcode + 取模"方案——`x % size` 在非 2 的幂长度上有模偏差，`%` 保留符号产生负索引，`Math.abs(Integer.MIN_VALUE)` 仍为负。
- **不变量**：非空、字符唯一（字符集语义是集合，重复=隐式加权，几乎必是 bug）、size ≥ 2（熵下限）。
- **允许嵌套 DP 属性**：DP 字段可为其他 DP（值对象组合——Evans 蓝皮书 / Vernon IDDD `Address` / ddd-by-examples `Money` 模式）。前提是组合消除重复校验与基本类型↔DP 转换，不为包装而包装。`framework-conventions` 字面值语义条款原"不允许嵌套持有 DP"与此冲突，同日修订。嵌套 DP 在 JSON 中经各自 `@JsonValue`/creator 表现为基本类型值（如 `codeAlphabet` 序列化为 `"0123456789"`），round-trip 无需额外配置。
- **应用案例**：`VerificationCode.code: String → RandomString`——消除 `from` 拆箱与 `matches` 再拆箱两处转换、`hasText` 校验由 RandomString 自校验承接、类型安全（code 只能是随机字符串，杜绝任意字符串伪造验证码）；JSON 形状不变（RandomString `@JsonValue` 裸字符串）。`SmsAuthAccountId`/`EmailAuthAccountId`/`PasswordAuthAccountId` 嵌套 `Mobile`/`Email`/`UserId` 是既有先例。
- **通用能力**：生成器不绑定任何业务概念（Verification 等），调用方自选字符集。
- **码形是通道级规则，非账号数据**：`SmsAuthAccount`/`EmailAuthAccount` 移除 `verificationCodePolicy` 字段（死字段——生产代码零读取、恒等于通道默认、override 链未实现，见 ADR-0011 修订注记）；`DEFAULT_POLICY` 常量保留为 per-account 覆盖预留契约（create 可空参数一并删除——零生产传参，契约删除，见 ADR-0011 修订注记）。

## Considered Options

| 方案 | 否定原因 |
|---|---|
| 枚举（DIGITS/UNAMBIGUOUS_ALPHANUMERIC，闭集） | 无法表达任意字符集；类别型惯例（ADR-0005）不适用于值型概念 |
| 基础设施拥有（多 bean/配置区分字符集） | 领域层无法表达意图；同一调用方多场景切换需持多个 bean；字符集漏进装配层 |
| `charAt` 取模环绕（任意 int → 字符） | 模偏差 + 负索引陷阱 + 掩盖生成器越界 bug；`SecureRandom.nextInt(size)` 已无偏解决 |
| policy 作 per-account 账号配置字段 | 死字段：零读取、恒为通道默认、override 链（SPI）未实现；码形是通道级规则（YAGNI） |

## Consequences

- `VerificationCodePolicy` 重构为嵌套 DP 值对象（`PositiveInt codeLength`、`Alphabet codeAlphabet`）；账户实体删除 policy 字段——JSON 持久化形状变化（脚手架阶段无存量数据，按破坏性变更处理）。
- 生成器实现类（尚不存在）按新签名实现；测试 lambda 桩与 Mockito 桩（`generate(any(PositiveInt.class), any(Alphabet.class))`）同步更新。
- "领域层只关心长度"断言在 CONTEXT.md / framework-conventions.md / 两侧 Javadoc 中撤销。
