package io.mango.ai.core.provider;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI 模型流式调用端口。
 */
public interface IAiProvider {

    /**
     * 发送上下文消息并返回标准 JSON 事件流。
     *
     * @param messages 对话上下文
     * @param enableThinking 是否启用思考模式
     * @return JSON 事件流
     */
    Flux<String> chat(List<Map<String, String>> messages, boolean enableThinking);
}
