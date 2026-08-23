package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.types.Ciphertext;

/**
 * 解密器 Gateway —— 类型擦除解密（ADR-0033）。
 * <p>
 * 密文不携带原始类型；解密时由调用方提供目标字面量类型，
 * 返回经验证的原始 DP（如 {@code decrypt(ct, Mobile.class)} → {@code Mobile}）。
 *
 * @see Gateway
 * @see Ciphertext
 * @see Encryptor
 */
public interface Decryptor extends Gateway {

    /**
     * 解密为目标字面量 DP 类型。
     *
     * @param ciphertext 密文
     * @param rawType    原始字面量 DP 类型（须有 String 单参构造或工厂约定）
     */
    <T extends StringLiteralType> T decrypt(Ciphertext ciphertext, Class<T> rawType);

    /**
     * 通用字符串解密。
     */
    String decryptGeneric(Ciphertext ciphertext);
}
