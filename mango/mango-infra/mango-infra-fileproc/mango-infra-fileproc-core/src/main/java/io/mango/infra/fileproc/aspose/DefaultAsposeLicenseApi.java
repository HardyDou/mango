package io.mango.infra.fileproc.aspose;

import io.mango.infra.fileproc.aspose.enums.AsposeProduct;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * 默认 Aspose License 内容提供实现。
 */
public class DefaultAsposeLicenseApi implements AsposeLicenseApi {

    private final Map<AsposeProduct, byte[]> licenseContents = new EnumMap<>(AsposeProduct.class);

    public DefaultAsposeLicenseApi(Map<AsposeProduct, byte[]> licenseContents) {
        if (licenseContents != null) {
            licenseContents.forEach((product, content) -> {
                if (product != null && content != null && content.length > 0) {
                    this.licenseContents.put(product, Arrays.copyOf(content, content.length));
                }
            });
        }
    }

    @Override
    public byte[] licenseContent(AsposeProduct product) {
        byte[] content = licenseContents.get(product);
        return content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }
}
