package cn.keking.utils;

/**
 * {@code fileid://} 内部直取地址。
 *
 * <p>预览引擎与文件服务同进程时，源文件不需要再绕一次 HTTP：Mango 把源文件令牌编成
 * {@code fileid://<token>} 交给引擎，{@link DownloadUtils} 识别该协议后直接调用
 * {@link cn.keking.spi.PreviewSourceProviders} 打开流并落盘，因此不产生出口流量、不依赖
 * 对外域名，也不需要把业务域名写进 {@code trust.host}。</p>
 *
 * <p>协议只在进程内有效：独立部署的引擎没有注册直取实现，会拒绝该类地址。</p>
 */
public final class PreviewSourceUrl {

    /** 内部直取协议名。 */
    public static final String SCHEME = "fileid";

    private static final String PREFIX = SCHEME + "://";

    private PreviewSourceUrl() {
    }

    /**
     * 拼出内部直取地址。
     *
     * @param sourceId 源标识，Mango 集成里是源文件临时令牌
     * @return {@code fileid://} 开头的地址
     */
    public static String build(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("内部直取地址缺少源标识");
        }
        return PREFIX + sourceId;
    }

    /**
     * 判断是否内部直取地址；大小写不敏感，允许带查询串与片段。
     *
     * @param url 待判断地址
     * @return 是内部直取地址时返回 true
     */
    public static boolean isSourceUrl(String url) {
        return url != null && url.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
    }

    /**
     * 取出源标识，忽略查询串与片段。
     *
     * @param url 内部直取地址
     * @return 源标识
     */
    public static String sourceId(String url) {
        if (!isSourceUrl(url)) {
            throw new IllegalArgumentException("不是内部直取地址: " + url);
        }
        String sourceId = url.substring(PREFIX.length());
        int separator = indexOfSeparator(sourceId);
        if (separator >= 0) {
            sourceId = sourceId.substring(0, separator);
        }
        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("内部直取地址缺少源标识: " + url);
        }
        return sourceId;
    }

    private static int indexOfSeparator(String sourceId) {
        int query = sourceId.indexOf('?');
        int fragment = sourceId.indexOf('#');
        if (query < 0) {
            return fragment;
        }
        if (fragment < 0) {
            return query;
        }
        return Math.min(query, fragment);
    }
}
