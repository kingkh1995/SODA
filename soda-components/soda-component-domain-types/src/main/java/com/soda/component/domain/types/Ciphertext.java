package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 可解密信封字面量 —— 加密族唯一代表（ADR-0033）。
 * <p>
 * 字面值为 JWE compact 序列化（RFC 7516）：{@code BASE64URL(header).BASE64URL(cek).BASE64URL(iv).BASE64URL(ct).BASE64URL(tag)}。
 * 内置信封约束：{@code alg=dir}（直接对称密钥）、{@code enc=A256GCM}、{@code kid} 非空——
 * 算法与密钥标识随值携带，解密侧无需带外约定即可选钥（密钥轮换期逐行不同，故必须带内）。
 * <p>
 * <b>类型擦除模型</b>：本类型不携带原始类型信息（加密不可逆地抹去了它）；
 * 解密时由调用方显式提供目标类型：{@code Decryptor.decrypt(ct, Mobile.class)}。
 * <p>
 * 字面值本身非攻击素材（无钥惰性），因此<b>不继承 SensitiveValue</b>、无遮蔽义务；
 * toString 缺省输出完整密文为可接受行为（域界判据见 ADR-0033）。
 *
 * @see StringLiteralType
 * @see com.soda.component.domain.gateway.Encryptor
 * @see com.soda.component.domain.gateway.Decryptor
 */
public record Ciphertext(String value) implements StringLiteralType {

    /**
     * JWE compact 五段式：header.cek.iv.ct.tag，全部 base64url 字符集。
     */
    private static final Pattern FIVE_SEGMENTS =
            Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    /**
     * JOSE 明文头白名单（顺序无关）：alg=dir、enc=A256GCM、kid 非空三者必须同时存在。
     * 结构校验仅此——真实接受由解密侧 JOSE 解析器裁决（重复成员等边缘不在此设防）。
     */
    private static final Pattern JOSE_HEADER = Pattern.compile(
            "(?s)^(?=.*\"alg\"\\s*:\\s*\"dir\")(?=.*\"enc\"\\s*:\\s*\"A256GCM\")(?=.*\"kid\"\\s*:\\s*\"[^\"]+\").*$");

    public Ciphertext {
        ValidateUtils.hasText(value);
        ValidateUtils.matches(value, FIVE_SEGMENTS);
        ValidateUtils.matches(decodeHeader(value), JOSE_HEADER);
    }

    private static String decodeHeader(String jwe) {
        var headerSegment = jwe.substring(0, jwe.indexOf('.'));
        return new String(ParseUtils.parseBase64Url(headerSegment), StandardCharsets.UTF_8);
    }
}
