package io.mango.job.starter.resource;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations.MessageTemplateSpec;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Job module message template resources.
 */
@Component
public class JobMessageTemplateResourceProvider implements ResourceProvider {

    @Override
    public List<String> moduleCodes() {
        return List.of("job");
    }

    @Override
    public List<ResourceDeclaration> provide() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(new MessageTemplateSpec(
                "job",
                "定时任务",
                2026061800700014001L,
                2060000000000014001L,
                2060000000000014002L,
                2060000000000014003L,
                1,
                "job.instance.failed",
                "定时任务执行失败",
                "JOB",
                "JOB",
                "定时任务实例执行失败后发送给任务负责人或配置接收人。",
                "{\"type\":\"object\",\"properties\":{\"appCode\":{\"type\":\"string\",\"title\":\"应用编码\"},\"jobCode\":{\"type\":\"string\",\"title\":\"任务编码\"},\"jobName\":{\"type\":\"string\",\"title\":\"任务名称\"},\"handlerName\":{\"type\":\"string\",\"title\":\"处理器\"},\"instanceId\":{\"type\":\"string\",\"title\":\"执行实例ID\"},\"triggerType\":{\"type\":\"string\",\"title\":\"触发类型\"},\"triggerBatchNo\":{\"type\":\"string\",\"title\":\"触发批次\"},\"traceId\":{\"type\":\"string\",\"title\":\"链路ID\"},\"scheduledFireTime\":{\"type\":\"string\",\"title\":\"计划触发时间\"},\"startTime\":{\"type\":\"string\",\"title\":\"开始时间\"},\"endTime\":{\"type\":\"string\",\"title\":\"结束时间\"},\"durationMillis\":{\"type\":\"number\",\"title\":\"执行耗时毫秒\"},\"errorSummary\":{\"type\":\"string\",\"title\":\"失败原因\"},\"ruleName\":{\"type\":\"string\",\"title\":\"告警规则\"}},\"required\":[\"jobCode\",\"jobName\",\"instanceId\",\"errorSummary\"]}",
                NoticePriority.HIGH,
                "BIZ_ID",
                true,
                "定时任务执行失败：{{jobName}}",
                "定时任务 {{jobName}}（{{jobCode}}）执行失败。实例：{{instanceId}}；处理器：{{handlerName}}；触发批次：{{triggerBatchNo}}；失败原因：{{errorSummary}}。请进入平台能力/任务管理/执行实例查看日志。",
                "【Mango】定时任务执行失败：{{jobName}}",
                "定时任务 {{jobName}}（{{jobCode}}）执行失败。实例：{{instanceId}}；处理器：{{handlerName}}；触发批次：{{triggerBatchNo}}；失败原因：{{errorSummary}}。请进入平台能力/任务管理/执行实例查看日志。",
                "定时任务执行失败：{{jobName}}",
                "定时任务 {{jobName}}（{{jobCode}}）执行失败，实例：{{instanceId}}，原因：{{errorSummary}}。",
                "定时任务执行失败：{{jobName}}",
                "定时任务 {{jobName}} 执行失败，请查看任务日志。"
        )));
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(new MessageTemplateSpec(
                "job",
                "定时任务",
                2026061900500010000L,
                2060000000001050000L,
                2060000000001050001L,
                2060000000001050002L,
                1,
                "job.worker.offline",
                "Worker 离线",
                "JOB",
                "JOB",
                "Job Worker 节点离线或心跳过期后通知任务管理员。",
                "{\"type\":\"object\",\"properties\":{\"appCode\":{\"type\":\"string\",\"title\":\"应用编码\"},\"serviceCode\":{\"type\":\"string\",\"title\":\"服务编码\"},\"workerGroup\":{\"type\":\"string\",\"title\":\"Worker分组\"},\"workerAddress\":{\"type\":\"string\",\"title\":\"Worker地址\"},\"lastHeartbeatAt\":{\"type\":\"string\",\"title\":\"最后心跳时间\"}},\"required\":[\"serviceCode\",\"workerAddress\"]}",
                NoticePriority.HIGH,
                "BIZ_ID",
                true,
                "Job Worker 离线：{{serviceCode}}",
                "Job Worker {{serviceCode}} / {{workerGroup}} 已离线或心跳过期，地址：{{workerAddress}}，最后心跳：{{lastHeartbeatAt}}。",
                "【Mango】Job Worker 离线：{{serviceCode}}",
                "Job Worker {{serviceCode}} / {{workerGroup}} 已离线或心跳过期，地址：{{workerAddress}}，最后心跳：{{lastHeartbeatAt}}。请检查服务状态。",
                "Job Worker 离线：{{serviceCode}}",
                "Job Worker {{serviceCode}} / {{workerGroup}} 已离线，地址：{{workerAddress}}。",
                "Job Worker 离线：{{serviceCode}}",
                "Job Worker {{serviceCode}} 已离线。"
        )));
        return declarations;
    }
}
