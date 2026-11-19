package io.mango.infra.fileproc.aspose.starter;

import io.mango.infra.fileproc.aspose.AsposeFontAvailability;
import io.mango.infra.fileproc.aspose.AsposeFontRegistrar;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.AsposeLicenseVerifier;
import io.mango.infra.fileproc.aspose.DefaultAsposeLicenseVerifier;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.starter.ConvertProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aspose 启动自检：字体可见性与授权可用性。
 *
 * <p>默认转换引擎是 Aspose，缺字体或缺授权不会让转换失败，而是静默产出方块字或评估水印，
 * 所以放在启动期 fail-fast，并在错误里直接点名缺哪个字体、哪个产品授权有问题。</p>
 *
 * <p>只有当前应用启用了至少一个 Aspose 转换器时才校验；全部关闭时跳过，避免只装配
 * license bean 而没有用 Aspose 的应用被误杀。字体断言只使用拉丁名：中文族名（如「宋体」）
 * Aspose 本身不可见，由内置映射表解析到 {@code SimSun}。</p>
 */
public class AsposeFontInitializer implements InitializingBean {

    /**
     * 中文文档渲染依赖的字体族（拉丁名）。
     */
    public static final List<String> REQUIRED_FONT_NAMES =
            List.of("SimSun", "SimHei", "KaiTi", "FangSong", "Microsoft YaHei");

    private static final Logger LOGGER = LoggerFactory.getLogger(AsposeFontInitializer.class);

    private final ConvertProperties convertProperties;

    private final AsposeProperties asposeProperties;

    private final AsposeLicenseApi licenseApi;

    private final AsposeFontAvailability fontAvailability;

    private final AsposeLicenseVerifier licenseVerifier;

    public AsposeFontInitializer(ConvertProperties convertProperties, AsposeProperties asposeProperties,
                                 AsposeLicenseApi licenseApi, AsposeFontAvailability fontAvailability) {
        this(convertProperties, asposeProperties, licenseApi, fontAvailability, new DefaultAsposeLicenseVerifier());
    }

    public AsposeFontInitializer(ConvertProperties convertProperties, AsposeProperties asposeProperties,
                                 AsposeLicenseApi licenseApi, AsposeFontAvailability fontAvailability,
                                 AsposeLicenseVerifier licenseVerifier) {
        this.convertProperties = convertProperties;
        this.asposeProperties = asposeProperties;
        this.licenseApi = licenseApi;
        this.fontAvailability = fontAvailability;
        this.licenseVerifier = licenseVerifier;
    }

    @Override
    public void afterPropertiesSet() {
        check();
    }

    /**
     * 执行自检。
     *
     * @throws IllegalStateException 字体缺失或授权不可用时
     */
    public void check() {
        if (!convertProperties.isAsposeFontCheckEnabled()) {
            LOGGER.info("Aspose 字体与授权自检已关闭（mango.fileproc.convert.aspose-font-check-enabled=false）");
            return;
        }
        Map<AsposeProduct, Boolean> conversions = enabledConversions();
        List<AsposeProduct> enabledProducts = new ArrayList<>();
        for (Map.Entry<AsposeProduct, Boolean> entry : conversions.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                enabledProducts.add(entry.getKey());
            }
        }
        if (enabledProducts.isEmpty()) {
            LOGGER.info("没有启用任何 Aspose 转换器，跳过 Aspose 字体与授权自检");
            return;
        }
        int appended = AsposeFontRegistrar.appendFontFolders(asposeProperties.getFontsFolders());
        if (appended > 0) {
            LOGGER.info("已向 Aspose 追加 {} 个字体目录", appended);
        }
        checkFonts(enabledProducts);
        checkLicenses(enabledProducts);
    }

    private Map<AsposeProduct, Boolean> enabledConversions() {
        Map<AsposeProduct, Boolean> conversions = new LinkedHashMap<>();
        conversions.put(AsposeProduct.WORDS, convertProperties.isAsposeWordToPdfEnabled());
        conversions.put(AsposeProduct.CELLS, convertProperties.isAsposeExcelToPdfEnabled());
        conversions.put(AsposeProduct.SLIDES, convertProperties.isAsposeSlideToPdfEnabled());
        conversions.put(AsposeProduct.PDF, convertProperties.isAsposePdfToImageEnabled());
        conversions.put(AsposeProduct.IMAGING, convertProperties.isAsposeImagingEnabled());
        return conversions;
    }

    private void checkFonts(List<AsposeProduct> enabledProducts) {
        Set<String> available = fontAvailability.availableFontNames();
        if (available == null || available.isEmpty()) {
            throw new IllegalStateException("Aspose 字体自检失败：无法枚举任何字体来源（已启用 " + enabledProducts
                    + "）。请确认字体服务可用，或在 mango.fileproc.aspose.fonts-folders 配置字体目录。");
        }
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_FONT_NAMES) {
            if (!available.contains(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Aspose 字体自检失败：缺少字体 " + missing + "（已启用 " + enabledProducts
                    + "）。中文族名（如「宋体」）由 Aspose 内置映射解析到 SimSun，不会出现在字体枚举里；"
                    + "请安装对应字体，或用 mango.fileproc.aspose.fonts-folders 追加字体目录。");
        }
        LOGGER.info("Aspose 字体自检通过：已启用 {} 的转换器，所需中文字体齐全（额外字体目录 {} 个）",
                enabledProducts, asposeProperties.getFontsFolders() == null ? 0 : asposeProperties.getFontsFolders().size());
    }

    private void checkLicenses(List<AsposeProduct> enabledProducts) {
        for (AsposeProduct product : enabledProducts) {
            byte[] content = licenseApi.licenseContent(product);
            if (content == null || content.length == 0) {
                throw new IllegalStateException("Aspose 授权自检失败：没有为 " + product
                        + " 配置 License（mango.fileproc.aspose.license-location 或对应的 *-license-location）。"
                        + "评估模式会输出水印，请先配置授权文件，或关闭对应的 aspose-*-enabled。");
            }
            try {
                licenseVerifier.verify(product, content);
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Aspose 授权自检失败：" + product + " 的 License 无法加载（"
                        + ex.getMessage() + "）。请检查授权文件是否与 " + product + " 20.3 匹配、是否已过期。", ex);
            }
        }
        LOGGER.info("Aspose 授权自检通过：{}", enabledProducts);
    }
}
