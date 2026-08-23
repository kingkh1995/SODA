package com.soda.component.domain.types;

import com.soda.component.domain.SensitiveValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveValue 家族相等性契约测试。
 * <p>
 * class-aware 相等依赖「基类 {@code @Include} 规范值 + 子类
 * {@code @EqualsAndHashCode(callSuper = true)}」的 canEqual 收窄链。子类漏标注解不报编译错，
 * 而是静默继承基类相等语义——对同值的外来敏感子类型放行 equals。Lombok 注解为 SOURCE 保留级、
 * 运行期反射不可见，故以行为断言拦截。
 *
 * @see SensitiveValue
 */
@DisplayName("SensitiveValue 子类相等性契约")
class SensitiveValueContractTest {

    /**
     * 探针子类 —— 模拟未来新增的敏感 DP。自身不带 callSuper 标注（继承基类 canEqual），
     * 恰好构成「外来敏感子类型」角色：真实子类若漏标 callSuper，对其同值实例的 equals 将误判为真。
     */
    private static final class Probe extends SensitiveValue {
        Probe(String value) {
            super(value);
        }

        @Override
        public String maskedValue() {
            return "probe";
        }
    }

    @Test
    @DisplayName("每个具体子类对外来敏感子类型恒不相等（漏标 callSuper 即失败）")
    void should_neverEqualForeignSensitiveSubtype() {
        assertThat(new Mobile("13800138000")).isNotEqualTo(new Probe("13800138000"));
        assertThat(new Email("user@example.com")).isNotEqualTo(new Probe("user@example.com"));
        assertThat(new IdCard("110101199001011234")).isNotEqualTo(new Probe("110101199001011234"));
        assertThat(new BankCard("6225880137293568")).isNotEqualTo(new Probe("6225880137293568"));
        assertThat(new ChineseName("张三")).isNotEqualTo(new Probe("张三"));
        assertThat(PasswordHash.of("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"))
                .isNotEqualTo(new Probe("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
    }

    @Test
    @DisplayName("同类型同值相等且 hashCode 一致")
    void should_equalWithinType_byCanonicalValue() {
        var left = new IdCard("110101199001011234");
        var right = new IdCard("110101199001011234");
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
    }

    @Test
    @DisplayName("格式相交的不同子类型同值恒不相等")
    void should_neverEqualAcrossConcreteTypes() {
        // 18 位纯数字串同时满足 IdCard（17 位 + 数字校验位）与 BankCard（13-19 位）格式
        var shared = "110101199001011234";
        assertThat(new IdCard(shared)).isNotEqualTo(new BankCard(shared));
        assertThat(new BankCard(shared)).isNotEqualTo(new IdCard(shared));
    }
}
