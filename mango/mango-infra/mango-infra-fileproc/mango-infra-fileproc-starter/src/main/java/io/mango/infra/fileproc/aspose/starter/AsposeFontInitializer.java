package io.mango.infra.fileproc.aspose.starter;

import io.mango.infra.fileproc.aspose.AsposeFontAvailability;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.AsposeLicenseApplier;
import io.mango.infra.fileproc.aspose.DefaultAsposeLicenseApplier;
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
 * <p>默认转换引擎是 Aspose。缺字体不会让转换失败，只会静默产出方块字——那是渲染质量问题，
 * 所以默认只打 ERROR（点名缺哪个字体）并继续启动，只有 {@code font-check-fatal=true} 才抛异常。
 * 缺授权或授权无法加载则一定 fail-fast：那是配置错误，且评估模式会给每份产出打水印。</p>
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

    private final AsposeLicenseApplier licenseApplier;

    public AsposeFontInitializer(ConvertProperties convertProperties, AsposeProperties asposeProperties,
                                 AsposeLicenseApi licenseApi, AsposeFontAvailability fontAvailability) {
        this(convertProperties, asposeProperties, licenseApi, fontAvailability, new DefaultAsposeLicenseApplier());
    }

    public AsposeFontInitializer(ConvertProperties convertProperties, AsposeProperties asposeProperties,
                                 AsposeLicenseApi licenseApi, AsposeFontAvailability fontAvailability,
                                 AsposeLicenseApplier licenseApplier) {
        this.convertProperties = convertProperties;
        this.asposeProperties = asposeProperties;
        this.licenseApi = licenseApi;
        this.fontAvailability = fontAvailability;
        this.licenseApplier = licenseApplier;
    }

    @Override
    public void afterPropertiesSet() {
        check();
    }

    /**
     * 执行自检。
     *
     * @throws IllegalStateException 授权不可用，或 {@code font-check-fatal=true} 且字体缺失时
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
            reportFontProblem("Aspose 字体自检：无法枚举任何字体来源（已启用 " + enabledProducts
                    + "）。请确认字体服务可用，或在 mango.fileproc.aspose.fonts-folders 配置字体目录。", enabledProducts);
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_FONT_NAMES) {
            if (!available.contains(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            reportFontProblem("Aspose 字体自检：缺少字体 " + missing + "（已启用 " + enabledProducts
                    + "）。中文族名（如「宋体」）由 Aspose 内置映射解析到 SimSun，不会出现在字体枚举里；"
                    + "请安装对应字体，或用 mango.fileproc.aspose.fonts-folders 追加字体目录。", enabledProducts);
            return;
        }
        LOGGER.info("Aspose 字体自检通过：已启用 {} 的转换器，所需中文字体齐全（额外字体目录 {} 个）",
                enabledProducts, asposeProperties.getFontsFolders() == null ? 0 : asposeProperties.getFontsFolders().size());
    }

    /**
     * 字体问题默认只告警：缺字体是渲染质量问题，不该让应用起不来。
     *
     * <p>只有 {@code mango.fileproc.aspose.font-check-fatal=true} 时才升级为启动失败。</p>
     */
    private void reportFontProblem(String message, List<AsposeProduct> enabledProducts) {
        if (asposeProperties.isFontCheckFatal()) {
            throw new IllegalStateException(message + "（font-check-fatal=true，启动中止）");
        }
        LOGGER.error("{}。转换仍会继续，但中文可能回退为其他字体或显示方块。"
                + "如需在启动期拦住，设置 mango.fileproc.aspose.font-check-fatal=true。", message);
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
                licenseApplier.apply(product, content);
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Aspose 授权自检失败：" + product + " 的 License 无法加载（"
                        + ex.getMessage() + "）。授权文件随 mango-infra-fileproc-core 发布，请确认文件完整且与 "
                        + product + " 匹配；也可以关闭对应的 aspose-*-enabled 改用 LibreOffice 兜底。", ex);
            }
        }
        LOGGER.info("Aspose 授权自检通过：{}", enabledProducts);
    }
}
