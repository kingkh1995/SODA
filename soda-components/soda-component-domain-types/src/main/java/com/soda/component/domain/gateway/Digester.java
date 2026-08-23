package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.SensitiveValue;
import com.soda.component.domain.types.Digest;
import com.soda.component.domain.types.SecretValue;

/**
 * 等值摘要器 Gateway —— 快摘要族的统一生产端口（ADR-0033）。
 * <p>
 * 两类意图，同一产出（32 字节 {@link Digest}），策略差异由方法承载、算法由基础设施配置：
 * <ul>
 *   <li>{@link #digest} —— 高熵令牌（API Key/Bearer）：无钥快哈希，输入熵 ≥112bit 自防
 *       （NIST 800-63B §5.1.2 look-up secrets）；明文永不落库，摘要列唯一索引，验证即查库；</li>
 *   <li>{@link #index} —— 低熵 PII 盲索引：按具体类型归一化（E.164/小写）后以字段级派生钥
 *       做 HMAC-SHA256（派生钥于实现层内部管理，与加密钥分离），配密文列组成两列模式。</li>
 * </ul>
 * 轮换语义差异：令牌重发即可；盲索引需全表重算。
 *
 * @see Gateway
 * @see Digest
 * @see SecretValue
 */
public interface Digester extends Gateway {

    /**
     * 高熵令牌摘要（无钥快哈希；同一令牌重复调用结果确定）。
     */
    Digest digest(SecretValue token);

    /**
     * 敏感字面量盲索引（归一化 + 字段级派生钥 HMAC；写侧随密文同步落库，读侧先算后查）。
     */
    Digest index(SensitiveValue pii);
}
