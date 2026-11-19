package io.mango.file.preview.starter;

import cn.keking.config.ConfigConstants;
import cn.keking.spi.PreviewSourceProviders;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.config.FilePreviewSourceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 把预览源文件地址的 host 自动加入引擎信任列表，并在进程内直取模式下跳过这一步。
 *
 * <p>引擎的 /onlinePreview、/getCorsFile 等入口接受任意 URL，必须保留 host 白名单防止被当作
 * SSRF 代理；Mango 集成只会让引擎抓取 {@code mango.file-preview.source-base-url}，
 * 因此该 host 无需运维再重复写进 {@code trust.host}。</p>
 */
public class FilePreviewTrustHostInitializer implements ApplicationRunner, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewTrustHostInitializer.class);

    private final String sourceBaseUrl;

    private final FilePreviewProperties properties;

    public FilePreviewTrustHostInitializer(String sourceBaseUrl) {
        this(sourceBaseUrl, null);
    }

    public FilePreviewTrustHostInitializer(FilePreviewProperties properties) {
        this(properties == null ? null : properties.getSourceBaseUrl(), properties);
    }

    private FilePreviewTrustHostInitializer(String sourceBaseUrl, FilePreviewProperties properties) {
        this.sourceBaseUrl = sourceBaseUrl;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (skipInternalSourceMode()) {
            return;
        }
        String baseUrl = sourceBaseUrl;
        if (!StringUtils.hasText(baseUrl)) {
            return;
        }
        String host;
        try {
            host = URI.create(baseUrl.trim()).getHost();
        } catch (IllegalArgumentException ex) {
            host = null;
        }
        if (!StringUtils.hasText(host)) {
            LOGGER.warn("忽略无法解析的 mango.file-preview.source-base-url: {}", baseUrl);
            return;
        }
        Set<String> trustedHosts = new LinkedHashSet<>();
        Set<String> configuredHosts = ConfigConstants.getTrustHostSet();
        if (configuredHosts != null) {
            trustedHosts.addAll(configuredHosts);
        }
        trustedHosts.add(host.toLowerCase(Locale.ROOT));
        ConfigConstants.setTrustHostValue(String.join(",", trustedHosts));
    }

    /**
     * 进程内直取时不把对外域名写进引擎信任列表；显式要求直取却没有实现时直接启动失败。
     *
     * @return 已按直取模式跳过信任列表注册时返回 true
     */
    private boolean skipInternalSourceMode() {
        if (properties == null) {
            return false;
        }
        FilePreviewSourceMode sourceMode = properties.getSourceMode();
        if (sourceMode == FilePreviewSourceMode.FILE_ID && !PreviewSourceProviders.hasProvider()) {
            throw new IllegalStateException(
                    "mango.file-preview.source-mode=fileId 需要同进程内注册预览源文件直取实现，"
                            + "当前进程没有实现；独立部署引擎请使用 url 或 auto");
        }
        boolean internalSource = sourceMode == FilePreviewSourceMode.FILE_ID
                || (sourceMode == FilePreviewSourceMode.AUTO && PreviewSourceProviders.hasProvider());
        if (internalSource) {
            LOGGER.info("预览源文件按 fileId 进程内直取，跳过把对外域名写入引擎信任列表");
        }
        return internalSource;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
