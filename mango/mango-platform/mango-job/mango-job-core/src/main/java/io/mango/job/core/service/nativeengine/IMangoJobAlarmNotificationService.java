package io.mango.job.core.service.nativeengine;

/**
 * Mango Job 失败告警服务。
 */
public interface IMangoJobAlarmNotificationService {

    /**
     * 发送实例失败告警。
     *
     * @param context 告警上下文
     * @return 可写入执行日志的发送摘要
     */
    String notifyInstanceFailed(MangoJobAlarmContext context);
}
