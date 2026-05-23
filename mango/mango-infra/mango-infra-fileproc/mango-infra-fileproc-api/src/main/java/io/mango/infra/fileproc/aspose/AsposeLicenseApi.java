package io.mango.infra.fileproc.aspose;

import io.mango.infra.fileproc.aspose.enums.AsposeProduct;

/**
 * Aspose License 内容提供接口。
 */
public interface AsposeLicenseApi {

    /**
     * 获取指定 Aspose 产品线的 License 内容。
     *
     * @param product Aspose 产品线。
     * @return License 内容；未配置时返回空数组。
     */
    byte[] licenseContent(AsposeProduct product);
}
