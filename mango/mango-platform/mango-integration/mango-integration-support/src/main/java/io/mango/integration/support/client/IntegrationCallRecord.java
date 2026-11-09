package io.mango.integration.support.client;

import lombok.Builder;
import lombok.Getter;

/**
 * 集成调用记录。
 *
 * <p>只记录调用元数据，不记录请求体和凭证。</p>
 */
@Getter
@Builder
public class IntegrationCallRecord {

    private final String instanceCode;
    private final String connectorCode;
    private final String method;
    private final String path;
    private final int statusCode;
    private final long elapsedMs;
    private final boolean success;
    private final String message;
}
