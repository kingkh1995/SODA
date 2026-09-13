package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ValidateUtils;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 更新掩码 DP — AIP-134 / AIP-161 的 {@code update_mask} 归一化形态，规范值为字段名集合。
 * <p>
 * 三语态（{@link #covers} 是唯一判定入口），与 AIP-134 逐条对应：
 * <ul>
 *   <li><b>空集</b>（省略 / 空白 / 纯分隔符）—— 「隐含掩码 = 全部已填充字段」：写不写由<b>值</b>决定，
 *       值为 null 的字段不写；</li>
 *   <li><b>字段集</b> —— 掩码是唯一标准：命中字段无条件写入，值为 null 即清空；</li>
 *   <li><b>{@code *}</b> —— 全量替换：{@link #parse} 展开为白名单全集后与上一条同构。</li>
 * </ul>
 * {@code *} 是<b>请求级指令</b>而非可存储的掩码值：解析期需白名单才能确定「全部字段」的外延，
 * 故典范构造器拒绝未展开的 {@code *}（{@code ["*"]} 无法离线反序列化，见 ADR-0038）。
 * <p>
 * 字符串入口 {@link #parse} 同时施加 <b>资源级</b>白名单（白名单外字段名 → {@link IllegalArgumentException}
 * → 400 {@code INVALID_ARGUMENT}，AIP-161 §5.3 强制）。白名单属资源上下文而非掩码自身的不变量，
 * 故不进构造器——同一字面量对不同资源的合法性不同，本类型只承诺「形态合法」。
 *
 * @see Type
 */
public record UpdateMask(Set<String> fields) implements Type {

    private static final String WILDCARD = "*";

    /**
     * 紧凑构造器即唯一校验点 —— 形态非法（null / 空白字段名 / 未展开的 {@code *}）不可表示。
     * <p>
     * 防御性拷贝用 {@code unmodifiableSet(new LinkedHashSet<>(…))} 而非 {@code Set.copyOf}：
     * 后者迭代序未定义（实测会打乱声明序），而掩码的 {@code toString} / JSON 字面量要求确定性。
     */
    public UpdateMask {
        ValidateUtils.notNull(fields);
        fields.forEach(ValidateUtils::hasText);
        if (fields.contains(WILDCARD)) {
            throw new IllegalArgumentException(
                    "wildcard is request-level and must be resolved to concrete field names: " + fields);
        }
        fields = Collections.unmodifiableSet(new LinkedHashSet<>(fields));
    }

    /**
     * 解析 update_mask 原始字符串（逗号分隔字段名，AIP-161）。
     *
     * @param raw           请求体中的 {@code updateMask} 字段值，可为 null（= 省略）
     * @param allowedFields 该资源的可更新字段白名单；含白名单外字段名 → IAE（→ 400）
     * @return 掩码；{@code null} / 空白 / 纯分隔符 → 空集；含 {@code *} → 白名单全集（全量替换）
     * @throws IllegalArgumentException 含白名单外字段名
     */
    public static UpdateMask parse(@Nullable String raw, Set<String> allowedFields) {
        ValidateUtils.notNull(allowedFields);
        if (raw == null || raw.isBlank()) {
            return new UpdateMask(Set.of());
        }
        var tokens = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (tokens.contains(WILDCARD)) {
            // 全量替换：`*` 已覆盖全部字段，与其余字段名并列属冗余，按全集收敛（AIP-134 未禁止混用）
            return new UpdateMask(allowedFields);
        }
        var unknown = new LinkedHashSet<>(tokens);
        unknown.removeAll(allowedFields);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "unknown update_mask fields: " + unknown + "; allowed: " + allowedFields);
        }
        return new UpdateMask(tokens);
    }

    /**
     * 本字段在本次更新中是否写入 —— 掩码判定的唯一入口。
     *
     * @param field 字段名（资源的可更新字段之一）
     * @param value 请求体中该字段的值，可为 null；仅用于「省略掩码」语态下的填充判定
     * @return 空掩码时值非 null 才写（隐含掩码 = 已填充字段）；非空掩码时命中即写
     */
    public boolean covers(String field, @Nullable Object value) {
        return fields.isEmpty() ? value != null : fields.contains(field);
    }

    /**
     * 规范值访问器 —— 字段名集合；空集 = 省略。
     * <p>
     * 显式覆写以承载 {@code @JsonValue}（Jackson 3 不识别 record component 上的注解）。
     */
    @Override
    @JsonValue
    public Set<String> fields() {
        return fields;
    }
}
