package io.mango.infra.fileproc.aspose;

import io.mango.infra.fileproc.aspose.enums.AsposeProduct;

/**
 * Aspose 授权可加载性校验。
 *
 * <p>抽成接口是为了让启动自检可测：单元测试提供桩实现，不需要真实的授权文件，
 * 也不能让测试把进程级的 Aspose 授权状态改掉。</p>
 */
@FunctionalInterface
public interface AsposeLicenseVerifier {

    /**
     * 校验授权内容能否被指定产品加载。
     *
     * @param product 产品
     * @param content 授权文件内容
     * @throws IllegalStateException 授权无法加载时
     */
    void verify(AsposeProduct product, byte[] content);
}
