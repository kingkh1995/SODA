package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨类型不等式契约（清单法）——同模块非枚举 record/class DP 两两恒不等。
 * <p>
 * 相等语义由 Lombok canEqual / record 构造性保证；本套件防「自定义 equals
 * 遗漏 class 检查」的回归：任何 DP 若按裸值比较，将在此对同值异型实例失败
 * （如 {@code WanYuan("1")} vs {@code Percentage("1.00")}）。新增 DP 只需在
 * {@link #inventory()} 登记一行规范实例，配对自动全量。
 * <p>
 * 范围：排除枚举（equals 不可覆写，跨类不等由 JVM 保证）与 SecretValue
 * （identity 相等，专测承载）；SensitiveValue 子类的 callSuper 泄漏守卫归
 * {@link SensitiveValueContractTest}（探针契约），此处仅承载具体类型互不相等。
 */
@DisplayName("跨类型不等式（DP 清单全对）")
class CrossTypeEqualityTest {

    /**
     * 单向断言即可：equals 对称性由 final 类 + Lombok canEqual 构造保证，
     * 反向断言只是复制同一保证。
     */
    private static List<Object> inventory() {
        return List.of(
                new Alphabet("XYZ789"),
                Active.FALSE,
                BankCard.of("6225880137293568"),
                ChineseName.of("张三"),
                new Ciphertext(jweSample()),
                new Digest("0".repeat(64)),
                Email.of("user@example.com"),
                new EmailContent("Welcome", "Thank you"),
                new EpochMilli(42L),
                new Fen(1500),
                IdCard.of("110101199001011234"),
                new LongId(42),
                new MaskedBankCard("622588******6789"),
                new MaskedChineseName("张*"),
                new MaskedEmail("t***@example.com"),
                new MaskedIdCard("110101********1234"),
                new MaskedMobile("138****8000"),
                Mobile.of("13800138000"),
                PasswordHash.of("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                Percentage.of("33.33"),
                PositiveInt.of(3),
                new RandomString("Abc123"),
                new SmsContent("hello"),
                new Uuid("550e8400-e29b-41d4-a716-446655440000"),
                new UpdateMask(Set.of("nickname")),
                ConcurrencyVersion.of(3),
                WanYuan.of("1"));
    }

    /**
     * 最小合法 JWE compact 样例（五段结构；头声明 alg=dir + enc=A256GCM + kid，
     * 与 CiphertextTest 探针同构）。
     */
    private static String jweSample() {
        var header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"phone-2024\"}"
                        .getBytes(StandardCharsets.UTF_8));
        return String.join(".", header, "cek", "iv", "ct", "tag");
    }

    @Test
    @DisplayName("清单内任意两类实例 equals 恒不相等")
    void should_neverEqualAcrossInventoryTypes() {
        var dps = inventory();
        // 清单自检：同一类不得重复登记（登记笔误即时暴露）
        assertThat(dps.stream().map(Object::getClass).distinct()).hasSize(dps.size());

        for (int i = 0; i < dps.size(); i++) {
            for (int j = i + 1; j < dps.size(); j++) {
                var left = dps.get(i);
                var right = dps.get(j);
                if (left.getClass() == right.getClass()) {
                    continue;
                }
                assertThat(left.equals(right))
                        .as("%s vs %s", left.getClass().getSimpleName(), right.getClass().getSimpleName())
                        .isFalse();
            }
        }
    }
}
