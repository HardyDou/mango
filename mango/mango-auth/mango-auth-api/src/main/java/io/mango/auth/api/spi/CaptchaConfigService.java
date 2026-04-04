package io.mango.auth.api.spi;

/**
 * SPI interface for captcha configuration.
 * Implement this to customize captcha requirements per path.
 */
public interface CaptchaConfigService {

    /**
     * Check if captcha is required for a given path.
     * @param path request path
     * @return true=captcha required, false=captcha not required (fail-open)
     */
    boolean isCaptchaRequired(String path);

    /**
     * Get captcha type required for path.
     * @param path request path
     * @return captcha type key or null for default
     */
    String getCaptchaType(String path);

    /**
     * Get captcha ttl in seconds.
     * @param path request path
     * @return ttl in seconds
     */
    long getCaptchaTtl(String path);
}
