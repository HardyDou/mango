package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 从 PowerJob 数据库读取实例记录。
 */
public class PowerJobDatabaseInstanceReader implements IPowerJobInstanceReader {

    private final PowerJobInstanceInfoMapper instanceInfoMapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final PowerJobProperties properties;

    public PowerJobDatabaseInstanceReader(PowerJobInstanceInfoMapper instanceInfoMapper,
                                          MangoJobDataSourceRouter dataSourceRouter,
                                          PowerJobProperties properties) {
        this.instanceInfoMapper = instanceInfoMapper;
        this.dataSourceRouter = dataSourceRouter;
        this.properties = properties;
    }

    @Override
    public List<PowerJobInstanceInfoEntity> readRecentInstances(Long jobId,
                                                               LocalDateTime triggerTimeStart,
                                                               LocalDateTime triggerTimeEnd,
                                                               int limit) {
        if (jobId == null) {
            return List.of();
        }
        int resolvedLimit = Math.max(1, Math.min(limit, 100));
        return dataSourceRouter.route(properties.getNativeLog().getDatasource(), () -> instanceInfoMapper.selectList(
                new LambdaQueryWrapper<PowerJobInstanceInfoEntity>()
                        .eq(PowerJobInstanceInfoEntity::getJobId, jobId)
                        .ge(triggerTimeStart != null, PowerJobInstanceInfoEntity::getExpectedTriggerTime,
                                toMillis(triggerTimeStart))
                        .le(triggerTimeEnd != null, PowerJobInstanceInfoEntity::getExpectedTriggerTime,
                                toMillis(triggerTimeEnd))
                        .orderByDesc(PowerJobInstanceInfoEntity::getExpectedTriggerTime)
                        .last("limit " + resolvedLimit)));
    }

    private Long toMillis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
