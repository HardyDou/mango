package io.mango.auth.core.anti;

/**
 * 应用签名密钥提供者。
 */
public interface AppSecretProvider {

    /**
     * 根据应用标识查询签名密钥。
     *
     * @param appKey 应用标识
     * @return 签名密钥；未配置时返回 null
     */
    String findSecret(String appKey);
}
