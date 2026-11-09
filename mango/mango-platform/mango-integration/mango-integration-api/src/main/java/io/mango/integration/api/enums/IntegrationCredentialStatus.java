package io.mango.integration.api.enums;

/**
 * 凭证状态。
 */
public enum IntegrationCredentialStatus {

    /** 可用。 */
    ACTIVE,

    /** 轮换中。 */
    ROTATING,

    /** 已到期。 */
    EXPIRED
}
