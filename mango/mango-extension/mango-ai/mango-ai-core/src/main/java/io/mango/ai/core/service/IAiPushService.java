package io.mango.ai.core.service;

/**
 * AI 模块通用实时推送服务。
 */
public interface IAiPushService {
    /**
     * 通过 Mango Realtime 广播通知。
     *
     * @param content 通知内容
     */
    void broadcastNotification(String content);

    /**
     * 通过 Mango Realtime 广播告警。
     *
     * @param content 告警内容
     */
    void broadcastAlert(String content);
}
