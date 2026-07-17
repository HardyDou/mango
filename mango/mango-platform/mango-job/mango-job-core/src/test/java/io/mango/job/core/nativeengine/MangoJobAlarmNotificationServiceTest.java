package io.mango.job.core.nativeengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.job.core.entity.MangoJobAlarmRuleEntity;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.mapper.MangoJobAlarmRuleMapper;
import io.mango.job.core.service.nativeengine.MangoJobAlarmNotificationService;
import io.mango.job.core.service.nativeengine.MangoJobAlarmContext;
import io.mango.job.core.service.nativeengine.MangoJobNoticeGateway;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.vo.NoticeSendResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MangoJobAlarmNotificationServiceTest {

    @Test
    void notifyInstanceFailedShouldPreserveStructuredNoticePayload() {
        MangoJobAlarmRuleMapper mapper = mock(MangoJobAlarmRuleMapper.class);
        NoticeApi noticeApi = mock(NoticeApi.class);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("noticeApi", noticeApi);
        when(noticeApi.send(any())).thenReturn(R.ok(new NoticeSendResultVO(1, 0)));

        MangoJobAlarmRuleEntity rule = new MangoJobAlarmRuleEntity();
        rule.setId(31L);
        rule.setRuleName("失败告警");
        rule.setNoticeTemplateCode("JOB_FAILED");
        when(mapper.selectList(any())).thenReturn(List.of(rule));

        MangoJobDefinitionEntity definition = new MangoJobDefinitionEntity();
        definition.setId(41L);
        definition.setTenantId("1");
        definition.setAppCode("mango-app");
        definition.setJobCode("daily-job");
        definition.setJobName("每日任务");
        definition.setHandlerName("dailyJobHandler");

        MangoJobInstanceEntity instance = new MangoJobInstanceEntity();
        instance.setId(51L);
        instance.setTriggerUserId(1L);
        instance.setTriggerType("MANUAL");

        MangoJobAlarmNotificationService service = new MangoJobAlarmNotificationService(
                mapper, new MangoJobNoticeGateway(beanFactory.getBeanProvider(NoticeApi.class)), new ObjectMapper());

        assertThat(service.notifyInstanceFailed(new MangoJobAlarmContext(definition, instance, "boom")))
                .isEqualTo("Job 失败告警已提交到 mango-notice，ruleId=31");

        var commandCaptor = org.mockito.ArgumentCaptor.forClass(SendNoticeCommand.class);
        verify(noticeApi).send(commandCaptor.capture());
        SendNoticeCommand command = commandCaptor.getValue();
        assertThat(command.getParams().toMap())
                .containsEntry("jobCode", "daily-job")
                .containsEntry("errorSummary", "boom");
        assertThat((Number) command.getParams().toMap().get("instanceId"))
                .extracting(Number::longValue)
                .isEqualTo(51L);
        assertThat(command.getMessageData().toMap()).isEqualTo(command.getParams().toMap());
        assertThat(command.getMessageTarget().getParams().toMap()).isEqualTo(command.getParams().toMap());
    }
}
