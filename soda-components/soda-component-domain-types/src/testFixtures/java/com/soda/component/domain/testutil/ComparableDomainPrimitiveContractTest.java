package com.soda.component.domain.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code Comparable} DP 的契约基类 —— 在 {@link DomainPrimitiveContractTest} 之上追加「比较」组。
 * <p>
 * 未实现 {@link Comparable} 的 DP 不得继承本类（建组需适用形态命中，见 dp-test-conventions §2 表规则）。
 * <p>
 * <b>样例次序约定</b>：{@code contract().other()} 须严格大于 {@code contract().sample()}；
 * 「比较」组据此钉住自然序方向，异值序实现（如字典序 vs 数值序）漂移即时暴露。
 *
 * @param <T> 被测 DP 类型
 */
public abstract class ComparableDomainPrimitiveContractTest<T extends Comparable<? super T>>
        extends DomainPrimitiveContractTest<T> {

    @Nested
    @DisplayName("比较")
    class ComparableTest {

        @Test
        @DisplayName("compareTo 与 equals 一致：同值零序")
        void should_beConsistentWithEquals() {
            var left = sample();
            assertThat(left.compareTo(sample())).isZero();
            assertThat(left).isEqualTo(sample());
        }

        @Test
        @DisplayName("异值 compareTo 非零且方向为 sample < other")
        void should_orderDeclaredSamples() {
            assertThat(sample().compareTo(other())).isNegative();
            assertThat(other().compareTo(sample())).isPositive();
        }
    }
}
