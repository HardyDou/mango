package io.mango.authorization.data.grant;

/**
 * Grant 校验结果。
 *
 * @param status  校验状态。
 * @param message 对外提示文案。
 */
public record DataGrantVerification(Status status, String message) {

    /**
     * 构造通过结果。
     *
     * @return 通过结果。
     */
    public static DataGrantVerification passed() {
        return new DataGrantVerification(Status.PASS, null);
    }

    /**
     * 构造指定状态的结果。
     *
     * @param status  状态。
     * @param message 提示文案。
     * @return 校验结果。
     */
    public static DataGrantVerification of(Status status, String message) {
        return new DataGrantVerification(status, message);
    }

    /**
     * 是否通过。
     *
     * @return 状态为 {@link Status#PASS} 时返回 true。
     */
    public boolean isPassed() {
        return status == Status.PASS;
    }

    /**
     * 校验状态。
     */
    public enum Status {

        /** 令牌有效且记录命中集合。 */
        PASS,

        /** 未携带 Grant。 */
        ABSENT,

        /** 令牌无效：格式、签名、密钥标识、数据域、租户或账号不匹配。 */
        INVALID,

        /** 令牌已过期。 */
        EXPIRED,

        /** 数据范围配置已变更（指纹不一致）。 */
        SCOPE_CHANGED,

        /** 记录不在 Grant 集合内，对外统一表现为「资源不存在」。 */
        ID_NOT_FOUND
    }
}
