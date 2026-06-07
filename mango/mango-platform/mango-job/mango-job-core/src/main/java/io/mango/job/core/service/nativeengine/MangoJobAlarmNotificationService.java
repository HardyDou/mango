package io.mango.job.core.service.nativeengine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.job.core.entity.MangoJobAlarmRuleEntity;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.mapper.MangoJobAlarmRuleMapper;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.vo.NoticeSendResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends Mango Job alarm events through mango-notice.
 */
@Service
public class MangoJobAlarmNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobAlarmNotificationService.class);

    private static final String ALARM_TYPE_INSTANCE_FAILED = "INSTANCE_FAILED";

    private final MangoJobAlarmRuleMapper alarmRuleMapper;

    private final ObjectProvider<NoticeApi> noticeApiProvider;

    private final ObjectMapper objectMapper;

    public MangoJobAlarmNotificationService(MangoJobAlarmRuleMapper alarmRuleMapper,
                                            ObjectProvider<NoticeApi> noticeApiProvider,
                                            ObjectMapper objectMapper) {
        this.alarmRuleMapper = alarmRuleMapper;
        this.noticeApiProvider = noticeApiProvider;
        this.objectMapper = objectMapper;
    }

    public String notifyInstanceFailed(MangoJobDefinitionEntity definition,
                                       MangoJobInstanceEntity instance,
                                       String errorSummary) {
        NoticeApi noticeApi = noticeApiProvider.getIfAvailable();
        if (noticeApi == null) {
            return "mango-notice 未启用，跳过 Job 失败告警发送";
        }
        List<MangoJobAlarmRuleEntity> rules = enabledRules(definition);
        if (rules.isEmpty()) {
            return "未配置启用的 Job 失败告警规则";
        }
        String lastResult = null;
        for (MangoJobAlarmRuleEntity rule : rules) {
            lastResult = sendRule(noticeApi, rule, definition, instance, errorSummary);
        }
        return lastResult;
    }

    private List<MangoJobAlarmRuleEntity> enabledRules(MangoJobDefinitionEntity definition) {
        return alarmRuleMapper.selectList(new LambdaQueryWrapper<MangoJobAlarmRuleEntity>()
                .eq(MangoJobAlarmRuleEntity::getTenantId, definition.getTenantId())
                .eq(MangoJobAlarmRuleEntity::getAppCode, definition.getAppCode())
                .eq(MangoJobAlarmRuleEntity::getEnabled, 1)
                .eq(MangoJobAlarmRuleEntity::getAlarmType, ALARM_TYPE_INSTANCE_FAILED)
                .and(wrapper -> wrapper
                        .eq(MangoJobAlarmRuleEntity::getJobId, definition.getId())
                        .or()
                        .isNull(MangoJobAlarmRuleEntity::getJobId)));
    }

    private String sendRule(NoticeApi noticeApi,
                            MangoJobAlarmRuleEntity rule,
                            MangoJobDefinitionEntity definition,
                            MangoJobInstanceEntity instance,
                            String errorSummary) {
        SendNoticeCommand command = new SendNoticeCommand();
        command.setBizType(rule.getNoticeSceneCode());
        command.setBizId(String.valueOf(instance.getId()));
        command.setTitle("Mango Job 任务执行失败：" + definition.getJobName());
        command.setContent(errorSummary);
        command.setParams(noticeParams(rule, definition, instance, errorSummary));
        command.setPriority(NoticePriority.HIGH);
        command.setUserId(instance.getTriggerUserId());
        applyRecipientRule(rule, command);
        command.setIdempotentKey("mango-job:alarm:" + rule.getId() + ":" + instance.getId());
        try {
            R<NoticeSendResultVO> response = noticeApi.send(command);
            if (response == null || !response.isSuccess()) {
                String message = response == null ? "通知接口无响应" : response.getMsg();
                LOGGER.warn("Mango Job alarm notice failed, ruleId={}, instanceId={}, message={}",
                        rule.getId(), instance.getId(), message);
                return "Job 失败告警发送失败：" + message;
            }
            return "Job 失败告警已提交到 mango-notice，ruleId=" + rule.getId();
        } catch (RuntimeException ex) {
            LOGGER.warn("Mango Job alarm notice error, ruleId={}, instanceId={}",
                    rule.getId(), instance.getId(), ex);
            return "Job 失败告警发送异常：" + ex.getMessage();
        }
    }

    private Map<String, Object> noticeParams(MangoJobAlarmRuleEntity rule,
                                             MangoJobDefinitionEntity definition,
                                             MangoJobInstanceEntity instance,
                                             String errorSummary) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ruleName", rule.getRuleName());
        params.put("noticeTemplateCode", rule.getNoticeTemplateCode());
        params.put("appCode", definition.getAppCode());
        params.put("jobId", definition.getId());
        params.put("jobCode", definition.getJobCode());
        params.put("jobName", definition.getJobName());
        params.put("handlerName", definition.getHandlerName());
        params.put("instanceId", instance.getId());
        params.put("triggerType", instance.getTriggerType());
        params.put("triggerBatchNo", instance.getTriggerBatchNo());
        params.put("traceId", instance.getTraceId());
        params.put("scheduledFireTime", instance.getScheduledFireTime());
        params.put("startTime", instance.getStartTime());
        params.put("endTime", instance.getEndTime());
        params.put("durationMillis", instance.getDurationMillis());
        params.put("errorSummary", StringUtils.hasText(errorSummary) ? errorSummary : instance.getErrorSummary());
        return params;
    }

    @SuppressWarnings("unchecked")
    private void applyRecipientRule(MangoJobAlarmRuleEntity rule, SendNoticeCommand command) {
        if (!StringUtils.hasText(rule.getNoticeParams())) {
            return;
        }
        try {
            Object value = objectMapper.readValue(rule.getNoticeParams(), Object.class);
            if (!(value instanceof Map<?, ?> params)) {
                return;
            }
            Object userId = params.get("userId");
            if (userId instanceof Number number) {
                command.setUserId(number.longValue());
            }
            Object userIds = params.get("userIds");
            if (userIds instanceof List<?> values) {
                List<Long> ids = new ArrayList<>();
                for (Object item : values) {
                    if (item instanceof Number number) {
                        ids.add(number.longValue());
                    }
                }
                command.setUserIds(ids);
            }
            Object recipientRuleCode = params.get("recipientRuleCode");
            if (recipientRuleCode instanceof String code && StringUtils.hasText(code)) {
                command.setRecipientRuleCode(code);
            }
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Mango Job alarm notice params parse failed, ruleId={}", rule.getId(), ex);
        }
    }
}
