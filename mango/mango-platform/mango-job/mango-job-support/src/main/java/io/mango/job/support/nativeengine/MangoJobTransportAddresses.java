package io.mango.job.support.nativeengine;

/**
 * Mango 原生 Job Worker 地址约定。
 */
public final class MangoJobTransportAddresses {

    /**
     * 内嵌 Worker 内存通信地址前缀。
     */
    public static final String IN_MEMORY_PREFIX = "in-memory://";

    /**
     * HTTP 内部通信地址前缀。
     */
    public static final String HTTP_PREFIX = "http://";

    /**
     * HTTPS 内部通信地址前缀。
     */
    public static final String HTTPS_PREFIX = "https://";

    private MangoJobTransportAddresses() {
    }

    /**
     * 判断是否为内存 Worker 地址。
     *
     * @param address Worker 地址
     * @return true 表示内存 Worker 地址
     */
    public static boolean isInMemory(String address) {
        return address != null && address.startsWith(IN_MEMORY_PREFIX);
    }

    /**
     * 判断是否为 HTTP 内部 Worker 地址。
     *
     * @param address Worker 地址
     * @return true 表示 HTTP 或 HTTPS Worker 地址
     */
    public static boolean isHttpInternal(String address) {
        return address != null && (address.startsWith(HTTP_PREFIX) || address.startsWith(HTTPS_PREFIX));
    }
}
