# ADR-0033: 加密族与哈希族重构 —— 类型擦除、场景端口与域界判据

日期：2026-08-23
状态：已接受
取代：ADR-0030 的加密族与哈希族部分（脱敏/Masked 族见 ADR-0032，不在本次范围）

## Context

ADR-0030 设计的派生类型层（Encrypted{Mobile..RealName}+EncryptedString、HashedPassword/HashedApiKey/CredentialHash）经审查存在结构性缺陷：

1. **名义空壳**：`EncryptedMobile.of(anyBase64)` 对"这是手机号的密文"零验证能力——子类对父类无任何不变量强化。加密/哈希是**类型擦除变换**：原始类型信息在变换时即丢失，值内不存在也不可验证。
2. **自验证公理违背**：一个 DP 的名字承诺了它验证不了的知识。
3. **算法选择误入公共 API**：`Hasher.hash(String algorithm, ...)` 把部署级常量暴露为调用方参数。

调研结论（JWE RFC 7516 / Tink / Vault Transit / MongoDB CSFLE / NIST 800-63B §5.1.1–5.1.2 / OWASP Password Storage / GitHub·Cloudflare 令牌哈希实践 / CipherSweet 盲索引）支撑以下模型。

## Decision

### 1. 解耦律（字面值是否为攻击素材）

| 字面值泄露后的危害 | 归属 |
|---|---|
| 非攻击素材（无钥惰性密文 / 单向指纹 / 掩码产物） | 普通 `StringLiteralType` record，无遮蔽义务 |
| 攻击素材（明文 PII；PHC 串=离线爆破素材） | `extends SensitiveValue`，toString 强制遮蔽 |

### 2. 加密族：唯一代表 `Ciphertext`

- 字面值 = JWE compact（RFC 7516），约束 `alg=dir + enc=A256GCM + kid 非空`；
- 自验证 = 五段式结构 + JOSE 明文头白名单（无需钥匙即可解析）；
- 类型擦除：解密由调用方提供目标类型 `Decryptor.decrypt(ct, Mobile.class)`；
- toString 缺省全量输出（无钥惰性，可辩护）。

### 3. 哈希族：两个代表

| 类型 | 场景 | 自验证 | 生产端口 |
|---|---|---|---|
| `PasswordHash`（extends SensitiveValue，唯一敏感特例） | 口令验证 | PHC 前缀白名单（argon2id/bcrypt/scrypt/pbkdf2）；格式感知遮蔽保留至盐段前 | `PasswordHasher.hash/verify(SecretValue)` |
| `Digest`（plain record） | 高熵令牌查找 + 低熵 PII 盲索引 | 恰好 32 字节（hex/base64） | `Digester.digest(SecretValue)`（无钥）/ `Digester.index(SensitiveValue)`（HMAC+归一化+字段级派生钥） |

- MD5 否决（RFC 6151/OWASP/NIST）；
- `Digest` 无法自证构造方式——算法出处由端口承载，这是自验证公理的准确读法：验证字面值中可知的全部不变量。

### 4. 端口命名：场景×能力混合
`Encryptor`/`Decryptor`（ISP 分离读写侧）、`PasswordHasher`（Tink 同名先例）、`Digester`
（快摘要族统一端口：`digest(SecretValue)` 无钥令牌摘要 + `index(SensitiveValue)` 盲索引——
两意图同产 32 字节摘要，策略差异由方法承载；归一化注册于实现层；
把 Tink 文档"use-case→primitive 选型指南"固化为端口）。
方法名=场景意图，类型名=能力契约，配置=算法参数；算法词不出现在任何公共签名。
**签名纪律**：入参禁止 String/基本类型、必须为 DP（出参允许基本类型）——
`Encryptor.encryptGeneric(String)` 因 String 入参删除，`Decryptor.decryptGeneric(Ciphertext): String`
合规保留，`PasswordHasher.verify` 出参 boolean 合规保留。
**预留拓展项**：`ContentFingerprinter` + `FileFingerprint`（文件去重/秒传/完整性，内容标识域）
随内容存储组件落地时创建，判据见本节域界。

### 5. 盲索引两列模式

登录标识字段：`*_ct`（Ciphertext，还原用）+ `*_bidx`（Digest，唯一索引）。规范化 → 字段级派生钥 HMAC（派生钥由加密基础设施内部管理，与加密钥分离）。泄露面仅等值关系与频次。

### 6. 域界判据（替代品 vs 附件）

摘要**替代**敏感原值参与持久化/查询/验证（删列即丧失能力）→ DP 哈希族；摘要仅**伴随**全量内容作地址/去重（删列无损）→ 内容标识域（`FileFingerprint`/`ContentFingerprinter`）。密钥有无是族内按输入熵选择的参数，不是域界。

## Consequences

- 删除：5 个 EncryptedXxx 壳、HashedPassword/HashedApiKey/CredentialHash、AbstractEncryptedValue/AbstractHashedValue、EncryptedValue/HashedValue 密封根、Hasher/CredentialHasher/HashAlgorithm（通用算法串分发模型废弃）；
- soda-user 六文件迁移至 PasswordHasher/PasswordHash；infra 新增 PasswordHasherImpl（BCrypt）；
- toString 不变量收缩为：SensitiveValue 子类强制遮蔽（raw 五族+PasswordHash）；Ciphertext/Digest 无遮蔽义务。

## 扩展路线（触发条件驱动，零预建）

| 候选场景 | 触发条件 | 处置 |
|---|---|---|
| 截断盲索引（16 字节桶，AWS Beacon 式） | 出现频次敏感的超大表 | `Digest` 校验分化 → 分裂专用型 |
| 索引钥轮换双写窗口 | 索引钥轮换 | 摘要升级 kid 标签信封 |
| 幂等键（请求体指纹） | 接入支付/开放 API | `Digester` 新增重载；载荷先立 DP（签名纪律） |
| Webhook/报文验签 | 接第三方回调 | `Digester` 新增 verifyMac 型方法 |
| 非对称签名（JWT 签发/文档签署） | 接入认证中心或电子签 | 独立端口（Signer/TokenIssuer）或外部服务适配器；签名产物是证据不是等值字面量，不产 Digest |
| 审计哈希链 | 合规审计需求 | 新端口 anchor()，评估新类型 |
| 限流分桶/缓存指纹/内容寻址 | 基础设施或内容域需求 | 不进 DP 域（域界判据 §6） |

## 修订注记（2026-08-23，代码检视落地）

1. **遮蔽规则修正为通用分段规则**：盐段 = 倒数第二段，`maskedValue()` 保留至其之前（原实现固定保留至第 4 段，scrypt/pbkdf2 五段形态会泄露盐段）；bcrypt 盐与校验和融合的四段形态维持 7 字符截断。
2. **Digest 字母表唯一化**：字面值接受 hex 或标准 base64（RFC 4648 §4 含 padding）；base64url 不接受（原实现正则放行 url-safe 字符但 MIME 解码器静默丢弃，行为自相矛盾）。
3. **长度上限接线 SPI**：`TypeConfigProvider.credentialHashMaxLength()`（指向已删除类型、零调用方）更名为 `passwordHashMaxLength()`，默认 200 对齐 user 表 `password_hash` 列 VARCHAR(200)，下限 128；`PasswordHash` 经 `TypeConfig.PROVIDER` 消费。
4. **`Encryptor.encrypt` 入参收窄为 `SensitiveValue`**（原 `StringLiteralType`）：密文/摘要/掩码等非敏感字面量的"再加密"是无意义调用；五个原始 PII 均为 SensitiveValue 子类，零生产调用方，收窄无迁移成本。
5. **`Digest` 构造期归一化为小写 hex**：hex 大小写与 base64 三种字面值可编码同一摘要，而 Digest 唯一用途是等值列（`uk_bidx`/令牌查找），依赖唯一线上形态；接受字母表不变（注记 2），仅收敛存储形态（Email 小写化先例）。
6. **`Digest` 输入收窄为单一字面值**：`of()` 仅接受小写 hex-64，标准 base64 构建走显式 `fromBase64` 工厂——取代注记 5 的双字母表归一化方案。理由：字面值即规范形，类型不做隐式拼写转换；显式工厂对齐 `fromYuan` 先例。（调研佐证：CipherSweet 盲索引输出仅提供 binary/hex 两形态，Laravel Sanctum 令牌哈希列为 VARCHAR(64) hex——base64 不是等值列的存储形态选项。）
7. **`PasswordHasher` 增加 `needsRehash(PasswordHash): boolean`**（2026-08-23，用户裁定提前落地 rehash 能力）：登录 verify 通过后消费——为真则以候选凭证重哈希并由调用方持久化。算法知识仍锁实现层（BCrypt 强度比较，非本产出形态返回 false），领域零算法词汇纪律不变；实体侧配套 `PasswordAuthAccount.verifyAndRehash(SecretValue, PasswordHasher)`（安全不变量：错误候选不动哈希）。方法面契约测试同步扩为 hash / verify / needsRehash 三方法。
8. **`PasswordHasherImpl.hash()` 增加 BCrypt 72 字节输入守卫**（2026-08-23，用户裁定）：bcrypt 的 Blowfish 密钥调度最多吸收 72 字节、实现普遍静默截断——前 72 字节相同的口令将验证等价。守卫只拦 `hash()`（注册/改密增量 fail-fast），**不拦 `verify()`**：存量截断哈希靠截断对称性照常匹配，verify 侧拒绝只会锁门而无安全增益。上限属算法实现细节，不入端口契约（换 argon2 时守卫随实现消亡）。

## 参考

RFC 7516 · FIPS 180-4/198-1 · RFC 2104 · RFC 6151 · NIST SP 800-63B · OWASP Password Storage CS · CWE-532 · PHC string format · Google Tink wire format · MongoDB CSFLE · CipherSweet · AWS DynamoDB Encryption SDK beacons
