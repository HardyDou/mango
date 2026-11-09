package io.mango.authorization.core.support.integration;

import lombok.Builder;
import lombok.Getter;

/**
 * 出站连接结果。
 *
 * <p>结果不包含凭证、请求头原文或响应正文。</p>
 */
@Getter
@Builder
public class OutboundResult {

    private final boolean success;
    private final Integer statusCode;
    private final long elapsedMs;
    private final String message;
}
