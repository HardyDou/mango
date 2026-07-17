package io.mango.link.core.service;

import lombok.Builder;
import lombok.Getter;

/** 网址 ID 跳转所需的服务层上下文。 */
@Getter
@Builder
public class LinkRedirectContext {

    private final Long id;
    private final String source;
    private final String clientIp;
    private final String userAgent;
    private final String referer;
}
