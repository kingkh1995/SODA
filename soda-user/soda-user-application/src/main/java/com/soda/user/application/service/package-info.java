/**
 * ApplicationService 实现 — 每个聚合根一个 ServiceImpl，编排 domain gateway + event bus。
 * <p>
 * 负责 Command → Domain 的编排，不包含业务逻辑。
 */
@NullMarked
package com.soda.user.application.service;

import org.jspecify.annotations.NullMarked;
