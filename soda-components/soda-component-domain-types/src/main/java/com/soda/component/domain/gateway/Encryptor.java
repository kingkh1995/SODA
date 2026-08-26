package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.Ciphertext;
import com.soda.component.domain.types.SensitiveValue;

/**
 * 加密器 Gateway —— PII 字段可还原加密场景（ADR-0033）。
 * <p>
 * 输入敏感值 DP（{@link SensitiveValue} 具体子类），输出类型擦除的 {@link Ciphertext}（JWE compact，
 * alg=dir + enc=A256GCM + kid）。密钥按字段类型于实现层内部派生，
 * 算法与密钥管理由基础设施实现承担。
 * <p>
 * 与 {@link Decryptor} 分离遵循 ISP：查询侧（只读）仅依赖解密能力。
 *
 * @see Gateway
 * @see Ciphertext
 * @see Decryptor
 */
public interface Encryptor extends Gateway {

    /**
     * 加密敏感值 DP（Mobile/Email/IdCard/BankCard/ChineseName 等）。
     * 入参收窄至 SensitiveValue——密文/摘要/掩码的再加密属无意义调用（ADR-0033 修订注记 4）。
     */
    Ciphertext encrypt(SensitiveValue pii);
}
