package io.mango.authorization.data.grant;

import java.util.List;

/**
 * Grant 相关响应头与提示文案常量。
 * <p>
 * 三个响应头都必须出现在 CORS {@code exposedHeaders} 中，否则跨域前端读不到它们，
 * 也就无法判断「这一页能不能点详情」；{@link #GRANT} 作为请求头回传时还必须在 {@code allowedHeaders} 中，
 * 否则跨源部署下浏览器会直接拦掉该请求头。追加动作由 {@link DataGrantAutoConfiguration} 完成。
 */
public final class DataGrantHeaders {

    /** 签发成功的 Grant 值，或未携带 Grant 时的 {@link #ABSENT} 标记。 */
    public static final String GRANT = "X-Mango-Data-Grant";

    /** 强制模式下缺少 Grant 的标记，值为 {@code 1}。 */
    public static final String REQUIRED = "X-Mango-Data-Grant-Required";

    /** 未签发 Grant 的原因，见 {@link #SKIPPED_OVERSIZE} 与 {@link #SKIPPED_SCOPE_UNRESOLVED}。 */
    public static final String SKIPPED = "X-Mango-Data-Grant-Skipped";

    /** {@link #GRANT} 的「未携带」标记值。 */
    public static final String ABSENT = "absent";

    /** {@link #SKIPPED} 的「ID 集合超过 max-ids」标记值。 */
    public static final String SKIPPED_OVERSIZE = "oversize";

    /** 跳过签发原因：数据范围无法解析。 */
    public static final String SKIPPED_SCOPE_UNRESOLVED = "scope-unresolved";

    /** 需要跨域暴露的响应头列表，顺序固定便于测试断言。 */
    public static final List<String> EXPOSED_HEADERS = List.of(GRANT, REQUIRED, SKIPPED);

    /** 需要允许跨域携带的请求头：只有 {@link #GRANT} 会被前端回传。 */
    public static final List<String> ALLOWED_REQUEST_HEADERS = List.of(GRANT);

    /** CORS 通配符，配置为它时无需逐个追加。 */
    private static final String WILDCARD = "*";

    /** 详情记录不存在或不在 Grant 集合内时对外统一使用的文案。 */
    public static final String MESSAGE_RESOURCE_NOT_FOUND = "资源不存在";

    /** 缺少 Grant 时的提示文案。 */
    public static final String MESSAGE_GRANT_REQUIRED = "缺少数据权限凭证，请刷新列表后重试";

    /** Grant 无效（签名、密钥标识、租户、账号或数据域不匹配）时的提示文案。 */
    public static final String MESSAGE_GRANT_INVALID = "数据权限凭证无效，请刷新列表后重试";

    /** Grant 过期时的提示文案。 */
    public static final String MESSAGE_GRANT_EXPIRED = "数据权限凭证已过期，请刷新列表后重试";

    /** 数据范围配置变更导致 Grant 失效时的提示文案。 */
    public static final String MESSAGE_GRANT_SCOPE_CHANGED = "数据权限已变更，请刷新列表后重试";

    private DataGrantHeaders() {
    }

    /**
     * 幂等追加 Grant 响应头到 CORS 暴露列表。
     *
     * @param exposedHeaders 目标可变列表，通常来自 {@code mango.web.cors.exposed-headers}。
     */
    public static void expose(List<String> exposedHeaders) {
        appendMissing(exposedHeaders, EXPOSED_HEADERS);
    }

    /**
     * 把 {@link #GRANT} 追加进 CORS 允许的请求头。
     *
     * <p>响应侧 {@code exposedHeaders} 只决定「前端能不能读」，请求侧 {@code allowedHeaders} 才决定
     * 「浏览器允不允许带」。显式配置了允许头但漏掉它的部署，跨域下详情请求会在浏览器层被拦掉，
     * 表现为「列表正常、详情永远 401/403」。配置为 {@code *} 时已覆盖，不再追加。</p>
     *
     * @param allowedHeaders CORS 允许的请求头列表，允许为 null。
     */
    public static void allowRequestHeader(List<String> allowedHeaders) {
        if (allowedHeaders == null) {
            return;
        }
        boolean wildcard = allowedHeaders.stream().anyMatch(existing -> WILDCARD.equals(existing));
        if (wildcard) {
            return;
        }
        appendMissing(allowedHeaders, ALLOWED_REQUEST_HEADERS);
    }

    private static void appendMissing(List<String> target, List<String> headers) {
        for (String header : headers) {
            boolean present = target.stream().anyMatch(existing -> header.equalsIgnoreCase(existing));
            if (!present) {
                target.add(header);
            }
        }
    }
}
