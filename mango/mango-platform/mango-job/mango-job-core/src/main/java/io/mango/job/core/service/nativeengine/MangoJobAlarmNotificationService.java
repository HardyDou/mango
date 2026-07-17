package io.mango.job.core.service.nativeengine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.job.api.enums.JobCode;
import io.mango.job.core.constant.MangoJobNoticeBizTypes;
import io.mango.job.core.entity.MangoJobAlarmRuleEntity;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.mapper.MangoJobAlarmRuleMapper;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class MangoJobAlarmNotificationService implements IMangoJobAlarmNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobAlarmNotificationService.class);

    private static final String ALARM_TYPE_INSTANCE_FAILED = "INSTANCE_FAILED";

    private final MangoJobAlarmRuleMapper alarmRuleMapper;

    private final MangoJobNoticeGateway noticeGateway;

    private final ObjectMapper objectMapper;

    public MangoJobAlarmNotificationService(MangoJobAlarmRuleMapper alarmRuleMapper,
                                            MangoJobNoticeGateway noticeGateway,
                                            ObjectMapper objectMapper) {
        this.alarmRuleMapper = alarmRuleMapper;
        this.noticeGateway = noticeGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public String notifyInstanceFailed(MangoJobAlarmContext context) {
        Require.notNull(context, JobCode.JOB_INVALID, "Job 失败告警上下文不能为空");
        Require.notNull(context.definition(), JobCode.JOB_INVALID, "任务定义不能为空");
        Require.notNull(context.instance(), JobCode.JOB_INVALID, "任务实例不能为空");
        MangoJobDefinitionEntity definition = context.definition();
        MangoJobInstanceEntity instance = context.instance();
        if (!noticeGateway.isAvailable()) {
            return "mango-notice 未启用，跳过 Job 失败告警发送";
        }
        List<MangoJobAlarmRuleEntity> rules = enabledRules(definition);
        if (rules.isEmpty()) {
            return "未配置启用的 Job 失败告警规则";
        }
        String lastResult = null;
        for (MangoJobAlarmRuleEntity rule : rules) {
            lastResult = sendRule(rule, definition, instance, context.errorSummary());
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

    private String sendRule(MangoJobAlarmRuleEntity rule,
                            MangoJobDefinitionEntity definition,
                            MangoJobInstanceEntity instance,
                            String errorSummary) {
        SendNoticeCommand command = new SendNoticeCommand();
        command.setBizType(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED);
        command.setBizId(String.valueOf(instance.getId()));
        command.setTitle("Mango Job 任务执行失败：" + definition.getJobName());
        command.setContent(errorSummary);
        Map<String, Object> params = noticeParams(rule, definition, instance, errorSummary);
        NoticeSiteMessageTargetCommand target = routeTarget("job:instance", params);
        command.setParams(NoticeJsonRequest.of(params));
        command.setMessageScene(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED);
        command.setMessageSubject(subject("JOB_INSTANCE", String.valueOf(instance.getId()), definition.getJobName()));
        command.setMessageTarget(target);
        command.setMessageData(NoticeJsonRequest.of(params));
        command.setMessageActions(List.of(routeAction("VIEW_INSTANCE", "查看实例", target)));
        command.setPriority(NoticePriority.HIGH);
        command.setUserId(instance.getTriggerUserId());
        applyRecipientRule(rule, command);
        command.setIdempotentKey("mango-job:alarm:" + rule.getId() + ":" + instance.getId());
        try {
            MangoJobNoticeGateway.MangoJobNoticeDelivery delivery = noticeGateway.send(command);
            if (!delivery.success()) {
                String message = delivery.message();
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

    private NoticeSiteMessageSubjectCommand subject(String subjectType, String subjectId, String subjectName) {
        NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
        subject.setSubjectType(subjectType);
        subject.setSubjectId(subjectId);
        subject.setSubjectName(subjectName);
        return subject;
    }

    private NoticeSiteMessageTargetCommand routeTarget(String targetKey, Map<String, Object> params) {
        NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
        target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
        target.setTargetKey(targetKey);
        target.setParams(NoticeJsonRequest.of(params));
        return target;
    }

    private NoticeSiteMessageActionCommand routeAction(String actionCode, String actionLabel, NoticeSiteMessageTargetCommand target) {
        NoticeSiteMessageActionCommand action = new NoticeSiteMessageActionCommand();
        action.setActionCode(actionCode);
        action.setActionLabel(actionLabel);
        action.setInteractionType(NoticeSiteMessageActionInteractionType.ROUTE);
        action.setTarget(target);
        return action;
    }

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
