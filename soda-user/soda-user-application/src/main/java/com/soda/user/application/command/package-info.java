/**
 * Command 处理器 — 写操作执行器。
 * <p>
 * 输入来自 {@code api/command/} 的 Command record，编排 domain gateway + event bus。
 * 当 ServiceImpl 方法超过 10 个或出现复杂编排时，按 Command 拆分为独立的 Processor。
 */
@NullMarked
package com.soda.user.application.command;

import org.jspecify.annotations.NullMarked;
