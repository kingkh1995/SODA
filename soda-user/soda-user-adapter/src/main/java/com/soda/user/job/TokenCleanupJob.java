package com.soda.user.job;

import com.soda.component.job.JobContext;
import com.soda.user.job.param.TokenCleanupParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 清理过期 Token 的定时任务。
 * <p>
 * Job 方法签名统一为 {@code method(JobContext&lt;ParamType&gt; ctx)}，
 * 不依赖具体调度框架，由适配器构造 {@code JobContext} 传入。
 */
@Slf4j
@Component
public class TokenCleanupJob {

    public void cleanExpiredTokens(JobContext<TokenCleanupParam> ctx) {
        // 占位实现：仅记录参数，接入调度框架后补业务逻辑
        var param = ctx.param();
        log.info("cleanExpiredTokens: daysBefore={}, batchSize={}, shard={}",
                param.daysBefore(), param.batchSize(), ctx.shardIndex());
    }
}
