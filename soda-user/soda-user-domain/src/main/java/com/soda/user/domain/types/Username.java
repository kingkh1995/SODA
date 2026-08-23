package com.soda.user.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.gateway.UserGateway;

import java.util.regex.Pattern;

/**
 * 用户名 DP — 4-30 位字母数字，不可变、自校验、可比较。
 * <p>
 * 唯一性由 {@link UserGateway#existsByUsername(Username)} 和 DB 唯一索引双重保证。
 *
 * @see StringLiteralType
 */
public record Username(String value) implements StringLiteralType, Comparable<Username> {

    /**
     * 注销终态默认值（ADR-0023）— R 行键释放后领域恢复的占位用户名。
     * <p>
     * 合法 {@code Username}（4-30 位字母数字）；只存在于领域内存，**从不落库**
     * （DB 中 R 行 {@code username} 为 NULL，见 convertor 空值映射）。
     * "removed" 仍是可注册用户名（DB 无此值），无命名空间浪费。
     * 契约：消费方不得将 {@link #REMOVED} 当作真实登录名处理（R 为吸收态，无登录业务）。
     */
    // 静态字段顺序：USERNAME_PATTERN 必须先于 REMOVED 初始化——REMOVED 构造
    // （new Username(...)）在校验中引用该 Pattern，倒序触发 ExceptionInInitializerError
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,30}$");

    /**
     * 注销终态默认值（ADR-0023）— R 行键释放后领域恢复的占位用户名。
     * <p>
     * 合法 {@code Username}（4-30 位字母数字）；只存在于领域内存，**从不落库**
     * （DB 中 R 行 {@code username} 为 NULL，见 convertor 空值映射）。
     * "removed" 仍是可注册用户名（DB 无此值），无命名空间浪费。
     * 契约：消费方不得将 {@link #REMOVED} 当作真实登录名处理（R 为吸收态，无登录业务）。
     */
    public static final Username REMOVED = new Username("removed");

    public Username {
        ValidateUtils.hasText(value);
        ValidateUtils.matches(value, USERNAME_PATTERN);
    }

    @Override
    public int compareTo(Username other) {
        return value.compareTo(other.value);
    }
}