package io.mango.file.preview.starter;

import cn.keking.utils.PreviewSourceUrl;
import cn.keking.utils.WebUtils;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.config.FilePreviewSourceMode;
import io.mango.file.preview.core.service.IFilePreviewService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 core 生成的 {@code fileid://} 地址与引擎解析规则一致（两模块不能互相依赖，靠测试对齐）。
 */
class FilePreviewSourceUrlContractTest {

    /** 与 {@code FilePreviewServiceImpl#FILE_ID_SOURCE_PREFIX} 保持一致的字面量。 */
    private static final String CORE_PREFIX = "fileid://";

    @Test
    void core前缀与引擎协议一致() {
        assertThat(CORE_PREFIX).isEqualTo(PreviewSourceUrl.build("x").substring(0, CORE_PREFIX.length()));
    }

    @Test
    void 引擎能从core地址中取出令牌并读到文件名() {
        String coreAddress = CORE_PREFIX + "token-1?fullfilename=file-100-0123456789abcdef.pptx";

        assertThat(PreviewSourceUrl.isSourceUrl(coreAddress)).isTrue();
        assertThat(WebUtils.isValidUrl(coreAddress)).isTrue();
        assertThat(PreviewSourceUrl.sourceId(coreAddress)).isEqualTo("token-1");
        assertThat(WebUtils.getUrlParameterReg(coreAddress, "fullfilename"))
                .isEqualTo("file-100-0123456789abcdef.pptx");
    }

    @Test
    void 直取模式默认是auto() {
        assertThat(new FilePreviewProperties().getSourceMode()).isEqualTo(FilePreviewSourceMode.AUTO);
    }

    @Test
    void 直取实现由starter装配成引擎可用的provider() {
        IFilePreviewService service = org.mockito.Mockito.mock(IFilePreviewService.class);
        FilePreviewEngineSourceProvider provider = new FilePreviewEngineSourceProvider(service);
        try {
            provider.afterPropertiesSet();
            assertThat(cn.keking.spi.PreviewSourceProviders.hasProvider()).isTrue();
        } finally {
            provider.destroy();
        }
    }
}
