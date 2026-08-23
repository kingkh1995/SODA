package com.soda.user.domain.types;

import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 短信投递端点 — {@code channel() = S}，泛型地址 {@link #target()} = {@link Mobile}（record 组件即 target）。
 * <p>
 * 多属性 DP（channel + target，JSON 对象输出，见 {@link VerificationRecipient}）——投递侧监听器按
 * recipient 类型匹配 {@code SmsSender}（见 ADR-0026）。
 *
 * @see VerificationRecipient
 */
public record SmsRecipient(Mobile target) implements VerificationRecipient<Mobile> {

    public SmsRecipient {
        ValidateUtils.notNull(target);
    }

    public static SmsRecipient of(String target) {
        return new SmsRecipient(new Mobile(target));
    }

    @Override
    public VerificationChannel channel() {
        return VerificationChannel.S;
    }
}
