package com.soda.user.job;

import com.soda.component.job.JobContext;
import com.soda.user.job.param.CleanTokenParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户模块定时任务。
 * <p>
 * Job 方法签名统一为 {@code method(JobContext&lt;ParamType&gt; ctx)}，
 * 不依赖具体调度框架，由适配器构造 {@code JobContext} 传入。
 */
@Slf4j
@Component
public class UserJob {

    public void cleanExpiredTokens(JobContext<CleanTokenParam> ctx) {
        // 目前仅输出日志，后续接入调度框架后实现业务逻辑
        var p = ctx.param();
        log.info("CleanExpiredTokens job executed: daysBefore={}, batchSize={}, shard={}",
                p.daysBefore(), p.batchSize(), ctx.shardIndex());
    }
}
