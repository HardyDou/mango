package io.mango.authorization.data.grant;

/**
 * Grant 签发结果。
 * <p>
 * 未签发时区分 {@code skipReason}：只有 {@code oversize} 会在响应头标记
 * {@code X-Mango-Data-Grant-Skipped}，其余原因（未配置密钥、缺少上下文、空集合）不对外暴露细节，
 * 避免把部署配置问题泄漏给前端。
 *
 * @param token      签发成功的令牌，未签发时为 null。
 * @param skipReason 未签发原因，签发成功时为 null。
 */
public record DataGrantIssueResult(String token, String skipReason) {

    /** 数据权限 Grant 能力已关闭。 */
    public static final String SKIP_DISABLED = "disabled";

    /** 未配置签名密钥。 */
    public static final String SKIP_NO_SECRET = "no-secret";

    /** 缺少租户或账号上下文。 */
    public static final String SKIP_NO_CONTEXT = "no-context";

    /** 本页没有记录。 */
    public static final String SKIP_EMPTY = "empty";

    /** 记录数超过 {@code max-ids}。 */
    public static final String SKIP_OVERSIZE = "oversize";

    /**
     * 构造签发成功结果。
     *
     * @param token 令牌。
     * @return 签发结果。
     */
    public static DataGrantIssueResult issued(String token) {
        return new DataGrantIssueResult(token, null);
    }

    /**
     * 构造未签发结果。
     *
     * @param skipReason 未签发原因。
     * @return 签发结果。
     */
    public static DataGrantIssueResult skipped(String skipReason) {
        return new DataGrantIssueResult(null, skipReason);
    }

    /**
     * 是否签发成功。
     *
     * @return 令牌非空时返回 true。
     */
    public boolean isIssued() {
        return token != null && !token.isBlank();
    }
}
