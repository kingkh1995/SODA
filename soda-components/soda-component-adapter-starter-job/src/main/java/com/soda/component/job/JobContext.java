package com.soda.component.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * 定时任务上下文。
 * <p>
 * 包含调度信息 {@code (jobName, shardIndex, shardTotal)} 和泛型业务参数 {@code T}，
 * 不依赖具体调度框架，由适配器构造后传入 Job 方法。
 * <pre>
 * // Quartz 适配
 * var ctx = new JobContext&lt;&gt;("cleanTokens", 0, 1, new CleanTokenParam(30, 1000));
 *
 * // XXL‑JOB 适配
 * var ctx = new JobContext&lt;&gt;("cleanTokens",
 *     XxlJobHelper.getShardIndex(), XxlJobHelper.getShardTotal(),
 *     JobParamCodec.decode(XxlJobHelper.getJobParam(), CleanTokenParam.class));
 * </pre>
 *
 * @param <T> 业务参数类型
 */
public record JobContext<T>(
        @JsonProperty("jobName") String jobName,
        @JsonProperty("shardIndex") int shardIndex,
        @JsonProperty("shardTotal") int shardTotal,
        @Nullable @JsonProperty("param") T param
) {
}
