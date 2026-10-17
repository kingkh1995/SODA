package com.soda.user.web;

import com.soda.component.web.error.ProblemDetailAdvice;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * MVC 切片测试引导配置 —— 本模块测试切片（{@code @WebMvcTest}）向上搜索 {@code @SpringBootConfiguration} 的锚点。
 *
 * <p>窄扫描 {@code com.soda.user.web}（{@code WebMvcTypeExcludeFilter} 进一步收敛到 controller）； 公共 {@code
 * com.soda} advice 不在扫描范围内，显式引入与生产装配同源的 {@link ProblemDetailAdvice} （生产经 starter 自动配置注册，本类不复制其行为）。
 *
 * <p>属共享测试基础设施（配置锚点，非数据 fixture/基类，见 test-conventions §3）。
 */
@SpringBootConfiguration
@ComponentScan(basePackageClasses = UserController.class)
@Import(ProblemDetailAdvice.class)
class UserWebTestApp {}
