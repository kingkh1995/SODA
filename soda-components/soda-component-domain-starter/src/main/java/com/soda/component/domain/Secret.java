package com.soda.component.domain;

/**
 * 秘密值基类 —— 携带敏感数据的不可变值，{@link Type} 的特殊子类型。
 * <p>
 * 继承 {@link Type} 的不可变 + 自校验契约，但针对敏感数据（密码、API Key、Token 等）扩展特殊规则：
 * <ul>
 *   <li><b>脱敏 toString</b> — 继承 {@link #toString()} 自动输出 {@code Xxx[***]}</li>
 *   <li><b>拒绝序列化</b> — 通过私有构造器 + 非标准访问器命名实现</li>
 *   <li><b>identity-based identity</b> — 默认走引用相等，不暴露值</li>
 * </ul>
 * <p>
 * 子类必须遵守：字段 {@code private final}，构造器私有，对外暴露静态 {@code of(...)} 工厂。
 * 值访问器不命名为 {@code getXxx()} 或直接的 {@code value()}，而是 {@code internalXxx()}，
 * 避免被 Jackson 等序列化框架自动发现。
 *
 * @see Type
 * @see com.soda.component.domain.types.RawCredential
 */
public abstract class Secret implements Type {

    /**
     * 脱敏字符串表示，不暴露实际值。
     *
     * @return {@code Xxx[***]}
     */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[***]";
    }
}
