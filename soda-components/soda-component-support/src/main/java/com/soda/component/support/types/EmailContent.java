package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty("subject") String subject,
        @JsonProperty("body") String body
) implements Type {

    private static final int SUBJECT_MAX_LENGTH = Math.max(255, TypeConfig.PROVIDER.emailSubjectMaxLength());

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EmailContent {
        ValidateUtils.nonBlank(subject);
        ValidateUtils.maxLength(SUBJECT_MAX_LENGTH, subject);
        ValidateUtils.nonBlank(body);
    }

}
