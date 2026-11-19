package io.mango.infra.fileproc.aspose;

import io.mango.infra.fileproc.aspose.enums.AsposeProduct;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用 Aspose 各产品自带的 {@code License#setLicense(InputStream)} 应用授权。
 */
public class DefaultAsposeLicenseApplier implements AsposeLicenseApplier {

    @Override
    public void apply(AsposeProduct product, byte[] content) {
        LicenseLoader loader = loaders().get(product);
        if (loader == null) {
            return;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            loader.load(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map<AsposeProduct, LicenseLoader> loaders() {
        Map<AsposeProduct, LicenseLoader> loaders = new LinkedHashMap<>();
        loaders.put(AsposeProduct.WORDS, stream -> new com.aspose.words.License().setLicense(stream));
        loaders.put(AsposeProduct.CELLS, stream -> new com.aspose.cells.License().setLicense(stream));
        loaders.put(AsposeProduct.SLIDES, stream -> new com.aspose.slides.License().setLicense(stream));
        loaders.put(AsposeProduct.PDF, stream -> new com.aspose.pdf.License().setLicense(stream));
        loaders.put(AsposeProduct.IMAGING, stream -> new com.aspose.imaging.License().setLicense(stream));
        return loaders;
    }

    /**
     * Aspose 各产品的 {@code setLicense(InputStream)} 声明了受检异常，用它统一签名。
     */
    @FunctionalInterface
    private interface LicenseLoader {

        void load(InputStream inputStream) throws Exception;
    }
}
