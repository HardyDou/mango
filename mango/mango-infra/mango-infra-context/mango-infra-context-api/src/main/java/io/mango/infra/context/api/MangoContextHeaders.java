package io.mango.infra.context.api;

import java.util.List;

/**
 * Mango 运行时上下文请求头常量。
 *
 * @author Mango
 */
public final class MangoContextHeaders {

    public static final String REQUEST_ID = "X-Mango-Request-Id";
    public static final String TRACE_ID = "X-Mango-Trace-Id";
    public static final String TENANT_ID = "X-Mango-Tenant-Id";
    public static final String USER_ID = "X-Mango-User-Id";
    public static final String MEMBER_ID = "X-Mango-Member-Id";
    public static final String PRINCIPAL_NAME = "X-Mango-Principal-Name";
    public static final String REALM = "X-Mango-Realm";
    public static final String ACTOR_TYPE = "X-Mango-Actor-Type";
    public static final String PARTY_TYPE = "X-Mango-Party-Type";
    public static final String PARTY_ID = "X-Mango-Party-Id";
    public static final String APP_CODE = "X-Mango-App-Code";
    public static final String CLIENT_IP = "X-Mango-Client-Ip";

    public static final List<String> ALL = List.of(
            REQUEST_ID,
            TRACE_ID,
            TENANT_ID,
            USER_ID,
            MEMBER_ID,
            PRINCIPAL_NAME,
            REALM,
            ACTOR_TYPE,
            PARTY_TYPE,
            PARTY_ID,
            APP_CODE,
            CLIENT_IP
    );

    private MangoContextHeaders() {
    }
}
