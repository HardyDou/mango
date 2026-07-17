package io.mango.ai.core.service;

import reactor.core.publisher.Flux;

/**
 * AI 模块通用推送服务。
 */
public interface IAiPushService {

    /**
     * 建立推送事件流。
     *
     * @return 标准 JSON 事件流
     */
    Flux<String> connect();

    /**
     * 广播通知。
     *
     * @param content 通知内容
     */
    void broadcastNotification(String content);

    /**
     * 广播告警。
     *
     * @param content 告警内容
     */
    void broadcastAlert(String content);
}
