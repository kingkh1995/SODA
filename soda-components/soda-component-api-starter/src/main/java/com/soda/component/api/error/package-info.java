/**
 * 需要被 HTTP 状态码细分的失败信号（404 不存在 / 409 冲突 / 412 条件未满足）—— 由应用层守卫与用例代码抛出，
 * starter-web 的 {@code ProblemDetailAdvice} 按类型自带的状态码翻译。
 * <p>
 * 其余拒绝不以本包类型表达：格式 / 值不可表示的校验仍是 {@link java.lang.IllegalArgumentException}
 * （译 400，见 ADR-0015）。见 ADR-0013。
 */
@NullMarked
package com.soda.component.api.error;

import org.jspecify.annotations.NullMarked;
