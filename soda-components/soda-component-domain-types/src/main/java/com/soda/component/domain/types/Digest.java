package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;

import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 等值摘要 —— 哈希族普通代表（ADR-0033）。
 * <p>
 * 恰好 32 字节（SHA-256/HMAC-SHA256 输出）的等值比较指纹，服务两类场景：
 * <ul>
 *   <li>高熵令牌查找：{@code Digester.digest(SecretValue)} —— 无钥快摘要，唯一索引列；</li>
 *   <li>低熵 PII 盲索引：{@code Digester.index(SensitiveValue)} —— 归一化 + 字段级派生钥 HMAC。</li>
 * </ul>
 * 字面值无法自证构造方式（裸 SHA-256 与 HMAC-SHA256 输出不可分辨），故类型只承诺
 * 可证明的契约（定长摘要 + 小写 hex 唯一线上形态）；算法出处由生产端口决定并记录于调用点。
 * 删除该列即丧失查询/验证能力——它是敏感原值的替代品，与内容寻址指纹（附件角色）分属两域。
 *
 * @see StringLiteralType
 * @see com.soda.component.domain.gateway.Digester
 */
public record Digest(String value) implements StringLiteralType {

    /**
     * 规范字面值 —— 小写 hex 编码的 32 字节摘要（SHA-256 的标准线上形态）。
     */
    private static final Pattern HEX_64 = Pattern.compile("^[0-9a-f]{64}$");

    public Digest {
        ValidateUtils.matches(value, HEX_64);
    }

    /**
     * 显式转换工厂 —— 从标准 base64 编码的 32 字节摘要构建，归一化为规范小写 hex。
     * <p>
     * 线形态守在其转换入口：只接受<b>规范形态</b>——解码后回编码须与输入全等（含 padding），
     * 故 base64url 与省略 padding 的变体一律拒绝；非 32 字节由构造器 {@link #HEX_64} 拒绝。
     */
    public static Digest fromBase64(String digest) {
        ValidateUtils.hasText(digest);
        var bytes = ParseUtils.parseBase64(digest);
        if (!Base64.getEncoder().encodeToString(bytes).equals(digest)) {
            throw new IllegalArgumentException("invalid format: '" + digest + "'");
        }
        return new Digest(HexFormat.of().formatHex(bytes));
    }
}
