package io.mango.infra.realtime.api.dto;

/**
 * 实时协议端点请求头名称。
 */
public final class RealtimeHeaders {

    public static final String AUTHORIZATION = "Authorization";

    public static final String TENANT_ID = "X-Mango-Tenant-Id";

    public static final String USER_ID = "X-Mango-User-Id";

    public static final String CLIENT_ID = "X-Mango-Client-Id";

    private RealtimeHeaders() {
    }
}
