package io.mango.authorization.data.grant;

import java.util.List;

/**
 * Grant 相关响应头与提示文案常量。
 * <p>
 * 三个响应头都必须出现在 CORS {@code exposedHeaders} 中，否则跨域前端读不到它们，
 * 也就无法判断「这一页能不能点详情」。追加动作由 {@link DataGrantAutoConfiguration} 完成。
 */
public final class DataGrantHeaders {

    /** 签发成功的 Grant 值，或未携带 Grant 时的 {@link #ABSENT} 标记。 */
    public static final String GRANT = "X-Mango-Data-Grant";

    /** 强制模式下缺少 Grant 的标记，值为 {@code 1}。 */
    public static final String REQUIRED = "X-Mango-Data-Grant-Required";

    /** 未签发 Grant 的原因，当前只有 {@link #SKIPPED_OVERSIZE}。 */
    public static final String SKIPPED = "X-Mango-Data-Grant-Skipped";

    /** {@link #GRANT} 的「未携带」标记值。 */
    public static final String ABSENT = "absent";

    /** {@link #SKIPPED} 的「ID 集合超过 max-ids」标记值。 */
    public static final String SKIPPED_OVERSIZE = "oversize";

    /** 需要跨域暴露的响应头列表，顺序固定便于测试断言。 */
    public static final List<String> EXPOSED_HEADERS = List.of(GRANT, REQUIRED, SKIPPED);

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
        for (String header : EXPOSED_HEADERS) {
            boolean present = exposedHeaders.stream().anyMatch(existing -> header.equalsIgnoreCase(existing));
            if (!present) {
                exposedHeaders.add(header);
            }
        }
    }
}
