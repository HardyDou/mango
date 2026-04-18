package io.mango.infra.web.support;

import io.mango.common.spi.request.RequestContext;
import io.mango.common.spi.request.RequestContextContributor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds servlet request variables into request context.
 */
public class WebRequestContextContributor implements RequestContextContributor {

    @Override
    public void contribute(RequestContext requestContext) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            requestContext.setAttribute("headers", Collections.emptyMap());
            requestContext.setAttribute("cookies", Collections.emptyMap());
            return;
        }

        var request = attributes.getRequest();
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        requestContext.setAttribute("request", request);
        requestContext.setAttribute("headers", headers);
        requestContext.setAttribute("cookies", cookies);
    }
}
