package io.mango.integration.support.client;

import lombok.Builder;
import lombok.Getter;

/**
 * 集成调用结果。
 */
@Getter
@Builder
public class IntegrationCallResult {

    private final boolean success;
    private final int statusCode;
    private final long elapsedMs;
    private final String body;
    private final String message;
}
