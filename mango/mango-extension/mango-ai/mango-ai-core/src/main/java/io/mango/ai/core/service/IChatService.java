package io.mango.ai.core.service;

import io.mango.ai.api.command.ChatCommand;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务。
 */
public interface IChatService {

    /**
     * 发起流式对话。
     *
     * @param command 对话命令
     * @return 标准 JSON 事件流
     */
    Flux<String> chat(ChatCommand command);
}
