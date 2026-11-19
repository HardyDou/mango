package io.mango.infra.fileproc.aspose;

import io.mango.infra.fileproc.aspose.enums.AsposeProduct;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用 Aspose 各产品自带的 {@code License#setLicense(InputStream)} 校验授权。
 */
public class DefaultAsposeLicenseVerifier implements AsposeLicenseVerifier {

    @Override
    public void verify(AsposeProduct product, byte[] content) {
        LicenseApplier applier = appliers().get(product);
        if (applier == null) {
            return;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            applier.apply(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map<AsposeProduct, LicenseApplier> appliers() {
        Map<AsposeProduct, LicenseApplier> appliers = new LinkedHashMap<>();
        appliers.put(AsposeProduct.WORDS, stream -> new com.aspose.words.License().setLicense(stream));
        appliers.put(AsposeProduct.CELLS, stream -> new com.aspose.cells.License().setLicense(stream));
        appliers.put(AsposeProduct.SLIDES, stream -> new com.aspose.slides.License().setLicense(stream));
        appliers.put(AsposeProduct.PDF, stream -> new com.aspose.pdf.License().setLicense(stream));
        appliers.put(AsposeProduct.IMAGING, stream -> new com.aspose.imaging.License().setLicense(stream));
        return appliers;
    }

    /**
     * Aspose 各产品的 {@code setLicense(InputStream)} 声明了受检异常，用它统一签名。
     */
    @FunctionalInterface
    private interface LicenseApplier {

        void apply(InputStream inputStream) throws Exception;
    }
}
