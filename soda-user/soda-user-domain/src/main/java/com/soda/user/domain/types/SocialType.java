package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum SocialType implements EnumType {

    GE("gitee"),
    DT("ding-talk"),
    WENT("wechat-work"),
    WMP("wechat-mp"),
    WOPN("wechat-open"),
    WMIN("wechat-mini"),
    ALIP("alipay-mini");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SocialType of(String name) {
        return ParseUtils.parseEnum(SocialType.class, name);
    }
}
