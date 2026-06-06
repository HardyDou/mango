package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 读取 PowerJob MySQL DFS 中的实例日志。
 */
public class PowerJobDatabaseNativeLogReader implements IPowerJobNativeLogReader {

    private static final String LOG_BUCKET = "log";
    private static final String LOG_NAME_PREFIX = "oms-";
    private static final String LOG_NAME_SUFFIX = ".log";

    private final PowerJobFileMapper fileMapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final PowerJobProperties properties;

    public PowerJobDatabaseNativeLogReader(PowerJobFileMapper fileMapper,
                                           MangoJobDataSourceRouter dataSourceRouter,
                                           PowerJobProperties properties) {
        this.fileMapper = fileMapper;
        this.dataSourceRouter = dataSourceRouter;
        this.properties = properties;
    }

    @Override
    public PowerJobNativeLog readInstanceLog(Long instanceId) {
        if (instanceId == null) {
            return PowerJobNativeLog.unavailable("缺少 PowerJob 实例 ID");
        }
        try {
            return dataSourceRouter.route(properties.getNativeLog().getDatasource(), () -> {
                PowerJobFileEntity file = fileMapper.selectOne(new LambdaQueryWrapper<PowerJobFileEntity>()
                        .eq(PowerJobFileEntity::getBucket, LOG_BUCKET)
                        .eq(PowerJobFileEntity::getName, logName(instanceId))
                        .last("limit 1"));
                if (file == null || file.getData() == null || file.getData().length == 0) {
                    return PowerJobNativeLog.unavailable("执行日志尚未归档或当前数据源不可读");
                }
                String content = new String(file.getData(), StandardCharsets.UTF_8);
                if (!StringUtils.hasText(content)) {
                    return PowerJobNativeLog.unavailable("执行日志为空");
                }
                return PowerJobNativeLog.available(content);
            });
        } catch (RuntimeException ex) {
            return PowerJobNativeLog.unavailable("执行日志读取失败：" + ex.getMessage());
        }
    }

    private String logName(Long instanceId) {
        return LOG_NAME_PREFIX + instanceId + LOG_NAME_SUFFIX;
    }
}
