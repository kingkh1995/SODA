---
type: Glossary
title: Soda — DDD Scaffold
description: 写代码 / 写文档 / 命名前查术语时读——业务词汇单一来源：定义＋`_Avoid_` 约束；框架层类型归代码 javadoc，不在此。
tags: [ glossary, ddd ]
status: stable
---

# Soda — DDD Scaffold

## Language

按聚合分组：身份 / 凭证 / 验证 / 应用服务入口。词汇项保持「定义 + `_Avoid_`」格式不变；分组标题不挂外链接，与 doc-conventions
§2.5「CONTEXT 互不挂链接」一致。

### §1 身份聚合（User）

写 User 聚合行为（属性修改、状态跃迁、注销）时读这里。

用户身份聚合根，持有一组 AuthAccount 子实体（密码账户为构造期必填字段）。属性修改经意图命名的领域方法表达（见 ADR-0010）。
_Avoid_: 用户管理、系统用户、setXXX、changeStatus、删除（作为领域动词）、remove、物理删除

**Username**:
用户账号，4-30 位字母数字，全局唯一；注销键释放后可被新注册复用。`REMOVED` 是键释放后领域内存中的占位值，不是真实登录名（见
ADR-0023）。
_Avoid_: 账号、账号名、把 REMOVED 当作真实登录名

**Nickname**:
用户昵称。最长 30 字符，禁止空白字符。
_Avoid_: 名称、显示名

**Mobile**:
用户手机号。格式校验 + 归一化，是短信认证账户标识的派生源。

**Sex**:
性别枚举：`M`（Male）、`F`（Female）。

**Avatar**:
用户头像 URL。URL 格式校验。


**UserState**:
用户状态枚举：`E`（Enabled 启用）、`D`（Disabled 禁用）、`R`（Removed 注销，吸收态终态）。状态跃迁经 User 的意图方法表达（disable /
enable / deregister），R 之后无任何操作（见 ADR-0017/0023）。 _Avoid_: 删除作为领域动词、remove

**键释放（Key Release）**:
注销终态后 username / mobile / email 可被新注册占用——领域语义是释放，持久化表示归基础设施决策（见 ADR-0017/0023）。

**SocialType**:
社交平台类型枚举。取值：`GE`（Gitee）、`DT`（DingTalk）、`WENT`（WechatWork）、`WMP`（WechatMp）、`WOPN`（WechatOpen）、`WMIN`（WechatMini）、`ALIP`（AlipayMini）。

**AuthAccount**:
用户认证账户，密封基类，4 个子类对应 4 种认证方式；User 聚合的子实体，生命周期由聚合根管理。
_Avoid_: 认证信息、登录方式、Account

### §2 凭证聚合（AuthAccount）

写认证方式建模（多态子实体、自描述 ID 编码、社交平台映射）时读这里。

**PasswordAuthAccount**:
密码认证账户，持有口令哈希（PHC 格式）。密码校验与修改是聚合内行为——原密码比对不匹配抛 IAE（见 ADR-0027）。

**SmsAuthAccount**:
短信认证账户。验证码不在账户上——码形是通道级规则，验证状态归 Verification 聚合（见 ADR-0011）。

**EmailAuthAccount**:
邮箱认证账户。行为同 SmsAuthAccount。

**SocialAuthAccount**:
社交认证账户。纯标识映射，无密码验证。

**AuthAccountId**:


**AuthAccountType**:
认证方式枚举。取值：`P`（Password）、`S`（Sms）、`E`（Email）、`O`（OAuth）。

### §3 验证聚合（Verification）

写验证码生命周期、发码拓扑、源 / 收件人建模、唯一性机制时读这里。

**VerificationCodePolicy**:
验证码策略值对象：码长 / 有效期 / 字符集的组合（嵌套 DP 组合，见 ADR-0018）。是通道级规则而非账号数据——效果物化进
`VerificationCode`，策略本身不落验证实体（见 ADR-0011/0026）。

**VerificationCode**:
验证码值对象：码值 + 过期时刻。匹配与过期判断均为注入时钟的纯函数；使用状态不在此——生命周期由 `Verification` 的状态表达（见
ADR-0011）。

**Verification**:
验证聚合根，独立于 User 存在——注册场景的验证先于用户诞生；封装一次验证码的生命周期：`I` 已创建待投递 → `P` 待验证 → `V`
已验证 → `U` 已使用，过期是派生判断不落状态（见 ADR-0021/0026）。 _Avoid_: `verifyXxx` 作为发起验证码的动词（发起动词是
`requestChangeMobileCode` / `requestChangeEmailCode`）

**VerificationChannel**:
验证通道枚举：`S`（SMS，短信）、`E`（Email，邮箱）。通道判别栖身于 `VerificationRecipient`，业务逻辑按 recipient 模式匹配分派、不按
channel 判断分支（见 ADR-0016/0026）。

**CredentialChangeDomainService**:
承载跨聚合编排的领域服务：验证通过后对 User 完成换绑（verify → 换绑 → use 一体序列）（见 ADR-0026）。

**UserVerificationScene**:
验证场景枚举——调用方词汇，区分业务用途防止多调用方冲突：`UCC`（换绑凭证）、`UPR`（找回密码）、`ULG`（登录验证）、`URG`（注册验证）。扁平助记码即
`source.scene` 值（ADR-0005）；Verification 对场景语义不感知。

**VerificationState**:
验证状态枚举：`I`（Initialized 已创建待投递）、`P`（Pending 待验证）、`V`（Verified 已验证）、`U`（Used 已使用，终态）。过期是派生判断不落状态（见
ADR-0011）；`V` 只在原子消费流中瞬时存在、永不落库。

**活跃验证（active verification）**:
存在性谓词：`state ∈ {I, P}` 且未过期——同一 `source` 至多一条活跃验证，唯一性机制见 ADR-0025。活跃不是状态枚举的一个值。

**VerificationSource**:
请求源标识：`{scene, subject}` 两段非空字符串——scene 是场景助记码，subject 是该场景内的裸业务键（无类型前缀，通道语义归
`VerificationRecipient`），两者复合成唯一槽位键（见 ADR-0026）。URG 场景 subject 即投递端点，以盲索引形态进入槽位键（见
ADR-0025 腾槽）。

**VerificationRecipient**:
投递端点多态值对象：`SmsRecipient(Mobile)` / `EmailRecipient(Email)`，携带通道判别与类型化目标地址——通道语义只在此，不在
source（见 ADR-0026/0028）。

### §4 应用服务入口

用户认证 / 凭证用例的应用编排入口（DomainService `CredentialChangeDomainService` 见 §3）。

**UserAuthService**:
用户认证/凭证用例的应用服务：换绑发码与验证消费的唯一编排入口；方法即场景、物理发送归事务提交后的投递监听——裁定细节见
ADR-0021/0026。 _Avoid_: 把物理发送写进服务编排；在 api 命令中携带 scene/channel
