package io.mango.authorization.core.support.integration;

import lombok.Builder;
import lombok.Getter;

/**
 * 出站探测调用描述。
 *
 * <p>只承载可达性探测需要的运行时参数，不含凭证与协议语义。</p>
 */
@Getter
@Builder
public class OutboundCall {

    private final String baseUrl;
    private final String path;
    private final int timeoutMs;
    private final int retryMax;
}
