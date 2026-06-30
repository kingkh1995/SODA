package com.soda.user.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.support.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

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

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SocialType of(String name) {
        return ParseUtils.parseEnum(SocialType.class, name);
    }
}
