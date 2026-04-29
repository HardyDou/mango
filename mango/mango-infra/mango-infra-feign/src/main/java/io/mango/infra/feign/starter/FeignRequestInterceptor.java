package io.mango.infra.feign.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.security.core.TokenContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign 请求拦截器，用于跨服务传递 Mango 运行时上下文。
 *
 * @author Mango
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        // 透传当前登录令牌。
        String token = TokenContextHolder.getToken();
        if (token != null && !token.isEmpty()) {
            template.header(AUTHORIZATION_HEADER, token);
            log.debug("透传 JWT token");
        }

        MangoContextSnapshot context = MangoContextHolder.get();
        put(template, MangoContextHeaders.REQUEST_ID, context.requestId());
        put(template, MangoContextHeaders.TRACE_ID, context.traceId());
        put(template, MangoContextHeaders.TENANT_ID, context.tenantId());
        put(template, MangoContextHeaders.USER_ID, context.userId());
        put(template, MangoContextHeaders.PRINCIPAL_NAME, context.principalName());
        put(template, MangoContextHeaders.REALM, context.realm());
        put(template, MangoContextHeaders.ACTOR_TYPE, context.actorType());
        put(template, MangoContextHeaders.PARTY_TYPE, context.partyType());
        put(template, MangoContextHeaders.PARTY_ID, context.partyId());
        put(template, MangoContextHeaders.APP_CODE, context.appCode());
        put(template, MangoContextHeaders.CLIENT_IP, context.clientIp());
    }

    private void put(RequestTemplate template, String name, Object value) {
        if (value != null && !value.toString().isBlank()) {
            template.header(name, value.toString());
            log.debug("透传 Mango 上下文请求头: {}", name);
        }
    }
}
