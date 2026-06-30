package com.soda.component.support.types;

import com.soda.component.domain.Type;
import com.soda.component.support.util.TypeConfig;
import com.soda.component.support.util.ValidateUtils;

/**
 * 邮件内容 DP — 主题 + 正文，不可变、自校验、可比较。
 * <p>
 * 校验规则：主题非 blank 且最长 255 字符，正文非 blank。
 *
 * @see Type
 */
public record EmailContent(
        String subject,
        String body
) implements Type {

    private static final int SUBJECT_MAX_LENGTH = Math.max(255, TypeConfig.PROVIDER.emailSubjectMaxLength());

    public EmailContent {
        ValidateUtils.hasText(subject);
        ValidateUtils.maxLength(subject, SUBJECT_MAX_LENGTH);
        ValidateUtils.hasText(body);
    }

}
