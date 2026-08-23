package com.soda.user.domain.types;

import com.soda.component.domain.types.Email;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 邮箱投递端点 — {@code channel() = E}，泛型地址 {@link #target()} = {@link Email}（record 组件即 target）。
 * <p>
 * 多属性 DP（channel + target，JSON 对象输出，见 {@link VerificationRecipient}）——投递侧监听器按
 * recipient 类型匹配 {@code EmailSender}（见 ADR-0026）。
 *
 * @see VerificationRecipient
 */
public record EmailRecipient(Email target) implements VerificationRecipient<Email> {

    public EmailRecipient {
        ValidateUtils.notNull(target);
    }

    public static EmailRecipient of(String target) {
        return new EmailRecipient(new Email(target));
    }

    @Override
    public VerificationChannel channel() {
        return VerificationChannel.E;
    }
}
