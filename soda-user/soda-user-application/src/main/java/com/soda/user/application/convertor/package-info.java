/**
 * DTO 转换器 — 双向转换 domain Entity ↔ api DTO。
 * <p>
 * 简单字段映射在 ServiceImpl 内联即可，映射逻辑复杂时抽出为独立的 Convertor。
 */
@NullMarked
package com.soda.user.application.convertor;

import org.jspecify.annotations.NullMarked;
