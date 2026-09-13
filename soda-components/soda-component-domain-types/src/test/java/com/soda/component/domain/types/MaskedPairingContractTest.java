package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * 原值 ↔ 掩码配对契约 —— 敏感数据 DP 与 {@code Masked*} 的家族一阶派生一致。
 * <p>
 * 验证四类契约：（1）派生一致 — {@code original.maskedValue()} 与
 * {@code MaskedXxx.from(original).value()} 同源同值；（2）脱敏不可逆推 —— 掩码串不含原值、
 * 仅保留文档约定的前缀/后缀形状；（3）toString 脱敏 — {@code SensitiveValue} 统一渲染
 * {@code ClassName[masked=<maskedValue>]}；（4）配对完整性 — 五对原值/掩码显式登记在
 * {@link #pairingCases()}，新增/删除 DP 必须同步改动此表，禁反射/扫描自检
 * （见 dp-test-conventions §1.6 「强制力 ＝ 检视项，不引入反射自检」）。
 *
 * @see SensitiveValueContractTest  equality 轴（不叠加）
 * @see MaskedMobile
 * @see MaskedEmail
 * @see MaskedIdCard
 * @see MaskedBankCard
 * @see MaskedChineseName
 */
@DisplayName("原值 ↔ 掩码配对契约")
class MaskedPairingContractTest {

    /**
     * 五对原值 DP ↔ 掩码 DP 显式登记清单 —— 配对完整性（契约 4）的载体。
     * 每行：原值 DP、其配对 Masked*、文档约定的掩码串。
     */
    static Stream<Arguments> pairingCases() {
        return Stream.of(
                arguments(
                        Mobile.of("13800138000"),
                        MaskedMobile.from(Mobile.of("13800138000")),
                        "138****8000"),
                arguments(
                        Email.of("user@example.com"),
                        MaskedEmail.from(Email.of("user@example.com")),
                        "u***@example.com"),
                arguments(
                        IdCard.of("110101199001011234"),
                        MaskedIdCard.from(IdCard.of("110101199001011234")),
                        "110101********1234"),
                arguments(
                        BankCard.of("6225880137293568"),
                        MaskedBankCard.from(BankCard.of("6225880137293568")),
                        "622588******3568"),
                arguments(
                        ChineseName.of("张三"),
                        MaskedChineseName.from(ChineseName.of("张三")),
                        "张*")
        );
    }

    @Nested
    @DisplayName("脱敏")
    class Masking {

        @ParameterizedTest(name = "[{index}] {0} → {2}")
        @MethodSource("com.soda.component.domain.types.MaskedPairingContractTest#pairingCases")
        @DisplayName("原值 ↔ 掩码配对契约（派生一致 / 不可逆推 / toString / 配对完整性）")
        void should_satisfyPairingContract(
                SensitiveValue original, StringLiteralType masked, String maskedValue) {
            // 契约 1 — 派生一致：两条派生路径产出一致掩码串
            assertThat(original.maskedValue()).isEqualTo(masked.value());
            // 契约 2 — 脱敏不可逆推：掩码串不含原值、仅保留前缀/后缀形状
            assertThat(masked.value()).isNotEqualTo(original.value());
            assertThat(masked.value()).doesNotContain(original.value());
            assertThat(original.maskedValue()).isNotEqualTo(original.value());
            // 契约 3 — toString 脱敏：基类统一渲染 ClassName[masked=<maskedValue>]
            assertThat(original).hasToString(
                    original.getClass().getSimpleName() + "[masked=" + maskedValue + "]");
            // 契约 4 — 配对完整性：Masked* 自身 value() 亦等于掩码串
            assertThat(masked.value()).isEqualTo(maskedValue);
        }
    }
}
