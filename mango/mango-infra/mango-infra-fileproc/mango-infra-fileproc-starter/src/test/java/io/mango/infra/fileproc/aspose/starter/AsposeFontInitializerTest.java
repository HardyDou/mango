package io.mango.infra.fileproc.aspose.starter;

import io.mango.infra.fileproc.aspose.AsposeFontAvailability;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.AsposeLicenseApplier;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.starter.ConvertProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aspose 字体与授权启动自检。
 *
 * <p>字体来源与授权校验都用桩实现：构建机没有中文字体，也不允许测试改掉进程级 Aspose 状态。</p>
 */
class AsposeFontInitializerTest {

    private static final Set<String> REQUIRED_FONTS =
            new LinkedHashSet<>(AsposeFontInitializer.REQUIRED_FONT_NAMES);

    private static final byte[] LICENSE = "license".getBytes(StandardCharsets.UTF_8);

    private final ConvertProperties convertProperties = new ConvertProperties();

    private final AsposeProperties asposeProperties = new AsposeProperties();

    private final List<AsposeProduct> verifiedProducts = new ArrayList<>();

    @Test
    void check_passesWhenRequiredFontsAndLicensesAreAvailable() {
        assertThatCode(() -> initializer(REQUIRED_FONTS, true, false).check()).doesNotThrowAnyException();
        assertThat(verifiedProducts).containsExactly(AsposeProduct.WORDS, AsposeProduct.CELLS,
                AsposeProduct.SLIDES, AsposeProduct.PDF, AsposeProduct.IMAGING);
    }

    @Test
    void check_warnsButKeepsStartingWhenFontsMissingByDefault() {
        Set<String> available = new LinkedHashSet<>(REQUIRED_FONTS);
        available.remove("SimSun");
        available.remove("KaiTi");
        assertThatCode(() -> initializer(available, true, false).check()).doesNotThrowAnyException();
        // 授权仍会被校验：字体不致命不代表整段跳过。
        assertThat(verifiedProducts).isNotEmpty();
    }

    @Test
    void check_failsOnMissingFontsWhenFatalEnabled() {
        asposeProperties.setFontCheckFatal(true);
        Set<String> available = new LinkedHashSet<>(REQUIRED_FONTS);
        available.remove("SimSun");
        available.remove("KaiTi");
        assertThatThrownBy(() -> initializer(available, true, false).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SimSun")
                .hasMessageContaining("KaiTi")
                .hasMessageContaining("font-check-fatal=true");
    }

    @Test
    void check_warnsWhenNoFontIsVisibleAtAll() {
        assertThatCode(() -> initializer(Set.of(), true, false).check()).doesNotThrowAnyException();
    }

    @Test
    void check_failsWhenNoFontIsVisibleAtAllAndFatalEnabled() {
        asposeProperties.setFontCheckFatal(true);
        assertThatThrownBy(() -> initializer(Set.of(), true, false).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法枚举任何字体来源");
    }

    @Test
    void check_skipsWhenFontCheckDisabled() {
        convertProperties.setAsposeFontCheckEnabled(false);
        assertThatCode(() -> initializer(Set.of(), false, false).check()).doesNotThrowAnyException();
        assertThat(verifiedProducts).isEmpty();
    }

    @Test
    void check_skipsWhenNoAsposeConverterEnabled() {
        disableAllAsposeConverters();
        assertThatCode(() -> initializer(Set.of(), false, false).check()).doesNotThrowAnyException();
        assertThat(verifiedProducts).isEmpty();
    }

    @Test
    void check_reportsProductWhoseLicenseIsMissing() {
        assertThatThrownBy(() -> initializer(REQUIRED_FONTS, false, false).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aspose 授权自检失败")
                .hasMessageContaining(AsposeProduct.WORDS.name());
    }

    @Test
    void check_wrapsLicenseLoadFailureWithProductName() {
        assertThatThrownBy(() -> initializer(REQUIRED_FONTS, true, true).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aspose 授权自检失败")
                .hasMessageContaining(AsposeProduct.WORDS.name())
                .hasMessageContaining("授权文件损坏");
    }

    private void disableAllAsposeConverters() {
        convertProperties.setAsposeWordToPdfEnabled(false);
        convertProperties.setAsposeExcelToPdfEnabled(false);
        convertProperties.setAsposeSlideToPdfEnabled(false);
        convertProperties.setAsposePdfToImageEnabled(false);
        convertProperties.setAsposeImagingEnabled(false);
    }

    private AsposeFontInitializer initializer(Set<String> fonts, boolean licenseAvailable, boolean licenseBroken) {
        AsposeFontAvailability availability = () -> fonts;
        AsposeLicenseApi licenseApi = product -> licenseAvailable ? LICENSE : null;
        AsposeLicenseApplier applier = (product, content) -> {
            verifiedProducts.add(product);
            if (licenseBroken) {
                throw new IllegalStateException("授权文件损坏");
            }
        };
        // 字体目录留空：不触碰 Aspose 进程级全局字体设置。
        asposeProperties.setFontsFolders(List.of());
        return new AsposeFontInitializer(convertProperties, asposeProperties, licenseApi, availability, applier);
    }
}
