package com.soda.user.job.param;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 清理过期 Token 的任务参数。
 *
 * @param daysBefore 清理 N 天前未使用的 Token
 * @param batchSize  每批处理条数
 */
public record CleanTokenParam(
        @JsonProperty("daysBefore") int daysBefore,
        @JsonProperty("batchSize") int batchSize
) {}
