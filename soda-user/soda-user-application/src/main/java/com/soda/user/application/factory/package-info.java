/**
 * DomainFactory — 复杂聚合根创建器。
 * <p>
 * 输入为 {@code api/command/} 的 Command，内部调用 Entity 的 Builder 完成创建。
 * 仅在创建逻辑涉及 DI 或跨聚合引用时才引入，一般场景用 Entity.createBuilder() 即可。
 */
@NullMarked
package com.soda.user.application.factory;

import org.jspecify.annotations.NullMarked;
