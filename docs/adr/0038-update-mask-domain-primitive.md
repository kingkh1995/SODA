---
type: Decision Record
title: update_mask 语义与归一化：UpdateMask DP 单源（省略 = 已填充字段掩码，`*` = 全量替换）
description: 设计或评审 Update 部分更新、改动 update_mask 解析时读——掩码归一化与覆盖判定收敛在 UpdateMask DP，应用层不感知原始字符串；省略语义与 `*` 通配符对齐 AIP-134。
tags: [ update-mask, aip-134, aip-161, domain-primitive, api ]
status: stable
---

# 0038 — update_mask 语义与归一化：UpdateMask DP 单源

`update_mask` 的三种语态（省略 / 字段集 / `*`）此前被劈成两处：解析器只做字符串 → 集合，而「空集退化为何种语义」写在应用层的一个布尔谓词里，同一句话在解析器
javadoc、调用点注释、私有方法 javadoc 各写一遍。现把归一化与覆盖判定收敛为 domain-types 的 `UpdateMask`
DP（规范值 = 字段名集合，`covers(field, value)` 是唯一判定入口），ApplicationService 只调用它、不感知原始字符串（见
ADR-0037 同类收敛：比对收敛在基类守卫 `requireIfMatch`）。

## 语义（对齐 AIP-134 / AIP-161）

| 语态                   | 字面量           | 规范值                       | 写不写由谁决定                                                           |
|------------------------|------------------|------------------------------|--------------------------------------------------------------------------|
| 省略 / 空白 / 纯分隔符 | 空集             | 空集                         | **值**（AIP-134「隐含掩码 = 全部**已填充**字段」）：仅写值非 null 的字段 |
| 字段集                 | `"nickname,sex"` | `{nickname, sex}`            | **掩码**（唯一标准）：命中字段无条件写入，值为 null 即清空               |
| 全量替换               | `"*"`            | **白名单全集**（解析期展开） | **掩码**：白名单内所有字段无条件写入，null 即清空                        |

白名单外字段名 → `IllegalArgumentException` → 400 `INVALID_ARGUMENT`（AIP-161 §5.3 强制）。

**`*` 在 `parse` 期展开为白名单全集**，而非以字面 `*` 存储：AIP-134 的 `*` 是请求级指令（「全量替换」），其外延
「全部字段」只有结合资源白名单才能确定；展开后 `covers` 退化为「空集判值 / 非空判命中」两分支且 **有界**（`covers` 对白名单外
字段名恒 false，不再对任意字段名返回 true），与字段集语态同构。副产物：`*,nickname` 这类混用自然收敛为全集（`*` 已覆盖全部，
冗余项无害；AIP 未禁止混用），无需自造拒绝规则；`["*"]` 不可离线反序列化（`*` 不是可存储的掩码值）。

## Consequences

- 掩码是 **线协议概念**，但类型落在 domain-types：应用层与适配层共用同一判定，避免在每个资源重复实现；代价是领域类型模块承载一个非领域来源的词汇（与
  `Digest`/`Ciphertext` 同属"承载可证明契约"的既有先例）。
- **资源级白名单经 `parse(raw, allowedFields)` 进入静态工厂**，与
  dp-conventions「静态工厂只做类型转换/预处理」的字面表述存在张力：白名单属资源上下文而非掩码自身不变量（同一字面量对不同资源合法性不同），构造器只能承诺形态合法。取舍记录于此，避免被当作疏漏"修复"。
- 判定方法命名 `covers`（而非 `isXxx`/`hasXxx`）：与 DP 自身领域动词先例一致（`VerificationCode.matches`、`expiredAt`），且
  `writes` 一类效果动词会误导读者以为方法有写行为。
- **`covers(field, value)` 的第二参是值本身而非 `boolean valuePresent`**：它虽只用于判空，却是 AIP-134「隐含掩码 =
  已填充字段」这条规范的 **输入**。传布尔会把「何为已填充」的定义推给每个调用点（可能被写成 `hasText` 等不同判定而静默改变语义），并让调用点重新出现
  `command.sex() != null` 这种裸布尔实参——写侧业务代码全仓仅有的布尔参数是先例所禁（`ValidateUtils.minValue` 的尾置
  `inclusive` 属 STYLEGUIDE §2.2 许可的"辅助参数"）。判定权留在一处。
- **显式空值仍走 DP 校验**：AIP-134 的 populated 措辞是「non-empty value」，严格读法下 `{"nickname": ""}`（省略掩码）应为
  no-op；本项目按「显式提供的非法值必须报错」处理 → `new Nickname("")` 抛 IAE → 400。这是对客户端 **显式输入**
  的严格化，不改变规则本体（值为 null 的字段仍不写）。
- 适配层与 api 模块的形状不变（Command 仍携带原始 `updateMask` 字符串），ADR-0026 关闭的 api→domain 依赖问题不被重开。
