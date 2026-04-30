package io.mango.infra.crypto.impl.sm;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * BouncyCastle Provider 全局初始化器。
 */
public final class BouncyCastleLoader {

    /** BouncyCastle Provider 名称。 */
    static final String PROVIDER_NAME = "BC";

    static {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private BouncyCastleLoader() {
    }

    /** 确保 Provider 已注册。 */
    public static void ensure() {
    }
}
