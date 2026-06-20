package com.soda.user.domain.enums;

import com.soda.component.domain.EnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 社交平台类型枚举。
 * <p>
 * DTO/VO 不直接引用枚举类型，通过 {@link #name()} 字符串传递。
 * 数据库存储 {@link #name()} 值（如 {@code "GE"} / {@code "DT"} / …）。
 * <p>
 * 枚举值对齐 Yudao {@code SocialTypeEnum} 的 {@code source} 映射：
 * <pre>
 *   GITEE(10)  → GE      DINGTALK(20)    → DT
 *   WECHAT_ENTERPRISE(30) → WENT  WECHAT_MP(31) → WMP
 *   WECHAT_OPEN(32)       → WOPN  WECHAT_MINI_PROGRAM(34) → WMIN
 *   ALIPAY_MINI_PROGRAM(40) → ALIP
 * </pre>
 *
 * @see Sex
 * @see UserStatus
 * @see AuthAccountType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SocialType implements EnumType {

    GE("Gitee"),
    DT("DingTalk"),
    WENT("WechatWork"),
    WMP("WechatMp"),
    WOPN("WechatOpen"),
    WMIN("WechatMini"),
    ALIP("AlipayMini");

    private final String desc;
}
