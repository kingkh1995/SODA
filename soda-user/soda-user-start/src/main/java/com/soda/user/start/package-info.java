/** 写侧启动入口模块 —— 用户写侧应用的 Spring Boot 启动类所在包。 */
@NullMarked
@ApplicationModule(
        type = ApplicationModule.Type.CLOSED,
        allowedDependencies = {})
package com.soda.user.start;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
