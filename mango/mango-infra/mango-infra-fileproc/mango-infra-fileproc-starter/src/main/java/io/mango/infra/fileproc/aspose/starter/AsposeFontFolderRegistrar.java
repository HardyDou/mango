package io.mango.infra.fileproc.aspose.starter;

import io.mango.infra.fileproc.aspose.AsposeFontRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 把 {@code mango.fileproc.aspose.fonts-folders} 注册到 Aspose.Words。
 *
 * <p>与字体自检解耦：只要 Aspose 模块启用且配置了目录就会注册，不受
 * {@code mango.fileproc.convert.aspose-font-check-enabled} 影响。否则「先关掉自检止血」
 * 会连带把字体目录注册一起关掉，转换依旧出方块且日志里没有任何线索。</p>
 */
public class AsposeFontFolderRegistrar implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsposeFontFolderRegistrar.class);

    private final AsposeProperties asposeProperties;

    public AsposeFontFolderRegistrar(AsposeProperties asposeProperties) {
        this.asposeProperties = asposeProperties;
    }

    @Override
    public void afterPropertiesSet() {
        int appended = AsposeFontRegistrar.appendFontFolders(asposeProperties.getFontsFolders());
        if (appended > 0) {
            LOGGER.info("已注册 Aspose 额外字体目录 {} 个: {}", appended, asposeProperties.getFontsFolders());
        }
    }
}
