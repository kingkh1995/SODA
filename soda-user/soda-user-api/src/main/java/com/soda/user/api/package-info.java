/**
 * api 模块（OPEN：允许被任何人引用）。
 * <p>
 * 2026-08-16 解除对 {@code domain} 的临时依赖（见 ADR-0026）：{@code RequestCodeCommand}
 * 删除，api 按用例拆分——per-use-case 命令（{@code RequestChangeMobileCodeCommand} 等）回归
 * primitive 形状，scene/channel 由方法语义隐式；0021 修订的「api→domain 正式方案」悬案以
 * 消除需求的方式了结。
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.soda.user.api;

import org.springframework.modulith.ApplicationModule;
