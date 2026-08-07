package com.soda.user.application.convertor;

import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.Sex;
import com.soda.user.api.dto.UserDTO;
import com.soda.user.domain.User;
import com.soda.user.domain.types.Avatar;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/**
 * User 聚合根 → {@link UserDTO} 转换器。
 * <p>
 * 出站 DTO 组装：领域值对象 unwrap 为基础类型。
 * 枚举输出 {@code name()}，与 {@link com.soda.component.domain.EnumType} 的 Jackson 序列化约定一致。
 */
@NullMarked
@Component
public class UserDTOConvertor {

    public UserDTO convert(User user) {
        return new UserDTO(
                user.getId().value(),
                user.getUsername().value(),
                user.getNickname().value(),
                user.getMobile().map(Mobile::value).orElse(null),
                user.getEmail().map(Email::value).orElse(null),
                user.getSex().map(Sex::name).orElse(null),
                user.getAvatar().map(Avatar::value).orElse(null),
                user.getState().name());
    }
}
