package com.soda.user.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.types.Sex;
import com.soda.component.web.EnumName;

import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

/**
 * 更新用户资料请求体 —— nickname/sex/avatar 全部可选，缺省即不修改对应字段。
 *
 * <p>协议字段：
 *
 * <ul>
 *   <li>{@code updateMask}：逗号分隔的字段名列表（AIP-134 §3.5 / AIP-161）。省略 / 空白视同 AIP-134 「隐含掩码 =
 *       全部已填充字段」——仅写值非 null 的字段；`*` 表示全量替换（所有字段无条件写入， 传 null 即清空）；含未知字段名 → 400
 *       INVALID_ARGUMENT（AIP-161 §5.3 表强制，见 ADR-0038）。
 * </ul>
 *
 * <p>{@code version} 字段不存在：乐观锁令牌由 {@link com.soda.user.web.response.UserResponse#version()}
 * 承担（AIP-154 etag 角色，opaque concurrency token），更新路径不新增 {@code etag} 字段——见 ADR-0039。
 */
public record UpdateUserRequest(
        @JsonProperty("updateMask") @Nullable String updateMask,
        @JsonProperty("nickname") @Nullable String nickname,
        @EnumName(Sex.class) @JsonProperty("sex") @Nullable String sex,
        @URL @JsonProperty("avatar") @Nullable String avatar) {}
