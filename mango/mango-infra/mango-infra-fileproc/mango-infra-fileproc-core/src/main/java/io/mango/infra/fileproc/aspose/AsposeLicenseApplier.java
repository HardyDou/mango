package io.mango.infra.fileproc.aspose;

import io.mango.infra.fileproc.aspose.enums.AsposeProduct;

/**
 * Aspose 授权加载器：把授权内容应用到指定产品上。
 *
 * <p>注意语义是「应用」而不是「校验」：Aspose 的 {@code License#setLicense(InputStream)}
 * 会改进程级的授权状态，重复应用同一份授权是幂等的。因此本接口的实现有副作用，
 * 只能在启动期调用，单元测试请提供桩实现，不要用真实实现去动全局状态。</p>
 *
 * <p>Aspose 对「订阅期已过但在该版本发布日之后仍可用」的情况不会抛异常，所以本接口
 * 只能发现文件结构不合法或产品不匹配，覆盖不到订阅期过期。</p>
 */
@FunctionalInterface
public interface AsposeLicenseApplier {

    /**
     * 把授权内容应用到指定产品。
     *
     * @param product 产品
     * @param content 授权文件内容
     * @throws IllegalStateException 授权内容无法被该产品加载时
     */
    void apply(AsposeProduct product, byte[] content);
}
