package com.soda.component.domain.types;

import com.soda.component.domain.LongLiteralType;
import com.soda.component.domain.Type;

import java.time.Duration;
import java.time.Instant;

/**
 * 绝对时间点 DP（见 ADR-0031）— UTC 绝对时间点，{@code long} 毫秒存储。
 * <p>
 * 「wire≠semantic」类型：线上标量为 {@code long}（epoch 毫秒，{@link com.fasterxml.jackson.annotation.JsonValue}
 * 继承自 {@link LongLiteralType}），语义值为 {@link Instant}（{@link #instant()} 派生，
 * {@link Instant#ofEpochMilli(long)} 毫秒精度互逆——亚毫秒截断，毫秒单位契约）。单位毫秒——主流
 * （{@code System.currentTimeMillis}/JS {@code Date.now}/Go {@code UnixMilli}）、保亚秒精度；
 * 覆盖 Jackson 3 裸 {@code Instant} 的 ISO-8601 字符串默认（{@code WRITE_DATES_AS_TIMESTAMPS} 默认 false）。
 * <p>
 * record 形态：{@code value()} 为 record 访问器、{@code @JsonValue} 继承、零 Jackson 代码、
 * 从 {@code long} 自动反序列化（Jackson 3.1.4 实证，见 ADR-0028）。无值域约束（任意 long 均合法）。
 * 富血方法（JDK 风格，见 dp-conventions）：{@link #of(Instant)}/{@link #now()} 构造、
 * {@link #plus(Duration)}/{@link #minus(Duration)} 偏移、{@link #isAfter(EpochMilli)}/{@link #isBefore(EpochMilli)} 谓词。
 * <p>
 * 当前为契约就位、暂无生产消费方；现有裸 {@code Instant} 字段的迁移另开一轮（见 ADR-0031 Consequences）。
 *
 * @see LongLiteralType
 * @see Type
 */
public record EpochMilli(long value) implements LongLiteralType, Comparable<EpochMilli> {

    /**
     * 从 {@link Instant} 构造。
     */
    public static EpochMilli of(Instant instant) {
        return new EpochMilli(instant.toEpochMilli());
    }

    /**
     * 当前时刻。
     */
    public static EpochMilli now() {
        return of(Instant.now());
    }

    /**
     * 语义值（派生 {@link Instant}，{@code ofEpochMilli} 毫秒精度互逆——亚毫秒截断）。
     */
    public Instant instant() {
        return Instant.ofEpochMilli(value);
    }

    /**
     * 时间偏移（加）。例如 {@code EpochMilli.now().plus(Duration.ofMinutes(5))}。
     */
    public EpochMilli plus(Duration duration) {
        return of(instant().plus(duration));
    }

    /**
     * 时间偏移（减）。
     */
    public EpochMilli minus(Duration duration) {
        return of(instant().minus(duration));
    }

    /**
     * 是否在指定时刻之后。
     */
    public boolean isAfter(EpochMilli other) {
        return value > other.value;
    }

    /**
     * 是否在指定时刻之前。
     */
    public boolean isBefore(EpochMilli other) {
        return value < other.value;
    }

    @Override
    public int compareTo(EpochMilli other) {
        return Long.compare(value, other.value);
    }
}
