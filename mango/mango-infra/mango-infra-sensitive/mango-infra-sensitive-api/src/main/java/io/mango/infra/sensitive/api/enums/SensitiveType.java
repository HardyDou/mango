package io.mango.infra.sensitive.api.enums;

/**
 * Built-in output masking strategies.
 */
public enum SensitiveType {

    /**
     * Custom prefix and suffix masking.
     */
    CUSTOM,

    /**
     * Compatibility alias for Pigx CUSTOMER.
     */
    CUSTOMER,

    /**
     * Chinese name masking.
     */
    CHINESE_NAME,

    /**
     * Identity card number masking.
     */
    ID_CARD,

    /**
     * Fixed-line phone masking.
     */
    FIXED_PHONE,

    /**
     * Mobile phone masking.
     */
    MOBILE_PHONE,

    /**
     * Address masking.
     */
    ADDRESS,

    /**
     * Email masking.
     */
    EMAIL,

    /**
     * Bank card masking.
     */
    BANK_CARD,

    /**
     * Password masking.
     */
    PASSWORD,

    /**
     * Secret key masking.
     */
    KEY,

    /**
     * IPv4 host segment masking.
     */
    IPV4,

    /**
     * Mainland China vehicle plate masking.
     */
    CAR_LICENSE,

    /**
     * Query parameter value masking.
     */
    QUERY_PARAM,

    /**
     * JSON string field masking by configured keys.
     */
    JSON
}
