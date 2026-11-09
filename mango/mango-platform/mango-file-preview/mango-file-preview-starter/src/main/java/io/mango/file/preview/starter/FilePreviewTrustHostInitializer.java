package io.mango.file.preview.starter;

import cn.keking.config.ConfigConstants;
import lombok.RequiredArgsConstructor;
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
 * 把预览源文件地址的 host 自动加入引擎信任列表。
 *
 * <p>引擎的 /onlinePreview、/getCorsFile 等入口接受任意 URL，必须保留 host 白名单防止被当作
 * SSRF 代理；Mango 集成只会让引擎抓取 {@code mango.file-preview.source-base-url}，
 * 因此该 host 无需运维再重复写进 {@code trust.host}。</p>
 */
@RequiredArgsConstructor
public class FilePreviewTrustHostInitializer implements ApplicationRunner, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewTrustHostInitializer.class);

    private final String sourceBaseUrl;

    @Override
    public void run(ApplicationArguments args) {
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
