/** 读侧启动入口模块 —— 查询服务（query-server）的 Spring Boot 启动类所在包。 */
@NullMarked
@ApplicationModule(
        type = ApplicationModule.Type.CLOSED,
        allowedDependencies = {"com.soda.user.api"})
package com.soda.user.queryserver;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
