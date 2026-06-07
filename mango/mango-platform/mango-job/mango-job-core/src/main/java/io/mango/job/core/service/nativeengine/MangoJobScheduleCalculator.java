package io.mango.job.core.service.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Mango 原生 Job 下一次触发时间计算器。
 */
@Component
public class MangoJobScheduleCalculator {

    public LocalDateTime nextFireTime(MangoJobDefinitionEntity definition, LocalDateTime baseTime) {
        Require.notNull(definition, "任务定义不能为空");
        JobScheduleType scheduleType = JobScheduleType.valueOf(definition.getScheduleType());
        LocalDateTime base = baseTime == null ? LocalDateTime.now() : baseTime;
        return switch (scheduleType) {
            case CRON -> nextCron(definition.getScheduleExpression(), base);
            case FIXED_RATE -> base.plusNanos(fixedRateMillis(definition.getScheduleExpression()) * 1_000_000L);
            case ONE_TIME -> oneTime(definition.getScheduleExpression());
            case MANUAL -> null;
        };
    }

    private LocalDateTime nextCron(String expression, LocalDateTime base) {
        Require.notBlank(expression, "Cron 表达式不能为空");
        CronExpression cronExpression = CronExpression.parse(expression.trim());
        return cronExpression.next(base);
    }

    private long fixedRateMillis(String expression) {
        Require.notBlank(expression, "固定频率表达式不能为空");
        try {
            long intervalMillis = Long.parseLong(expression.trim());
            Require.isTrue(intervalMillis > 0, "固定频率必须大于 0 毫秒");
            return intervalMillis;
        } catch (NumberFormatException ex) {
            return Require.fail(400, "固定频率表达式必须为毫秒数");
        }
    }

    private LocalDateTime oneTime(String expression) {
        Require.notBlank(expression, "一次性调度时间不能为空");
        String value = expression.trim();
        if (StringUtils.hasText(value)) {
            return LocalDateTime.parse(value);
        }
        return null;
    }
}
