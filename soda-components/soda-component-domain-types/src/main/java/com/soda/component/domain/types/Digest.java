package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;

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

    /**
     * 标准 base64（RFC 4648 §4，含 padding）编码的 32 字节摘要：44 字符、末尾恰一个 '='。
     */
    private static final Pattern BASE64_STD_32B = Pattern.compile("^[A-Za-z0-9+/]{43}=$");

    /**
     * 唯一规范入口 —— 仅接受小写 hex-64 字面值；大写与 base64 拼写走显式转换工厂。
     */
    public Digest {
        ValidateUtils.matches(value, HEX_64);
    }

    /**
     * 工厂 —— 委托紧凑构造器（小写 hex-64 是唯一合法字面值）。
     */
    public static Digest of(String digest) {
        return new Digest(digest);
    }

    /**
     * 显式转换工厂 —— 从标准 base64 编码的 32 字节摘要构建，归一化为规范小写 hex。
     * base64url 不接受（ADR-0033 修订注记 2）。
     */
    public static Digest fromBase64(String digest) {
        ValidateUtils.hasText(digest);
        ValidateUtils.matches(digest, BASE64_STD_32B);
        var bytes = ParseUtils.parseBase64(digest);
        ValidateUtils.equals(bytes.length, 32);
        return new Digest(HexFormat.of().formatHex(bytes));
    }
}
