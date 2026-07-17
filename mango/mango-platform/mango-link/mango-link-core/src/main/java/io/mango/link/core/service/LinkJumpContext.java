package io.mango.link.core.service;

import lombok.Builder;
import lombok.Getter;

/** 原始 URL 跳转所需的服务层上下文。 */
@Getter
@Builder
public class LinkJumpContext {

    private final String url;
    private final String visitorId;
    private final String source;
    private final String extraParams;
    private final String clientIp;
    private final String userAgent;
    private final String referer;
}
