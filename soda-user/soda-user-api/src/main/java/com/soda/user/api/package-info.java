/**
 * api 模块（OPEN：允许被任何人引用）——用例命令与出站 DTO 的公共契约。
 * <p>
 * 命令按用例拆分（per-use-case）：scene/channel 等场景语义由方法名隐式，
 * 客户端不可伪造（见 ADR-0026）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.soda.user.api;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
