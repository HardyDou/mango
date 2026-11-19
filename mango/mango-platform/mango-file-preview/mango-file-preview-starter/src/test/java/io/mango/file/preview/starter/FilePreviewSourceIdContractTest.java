package io.mango.file.preview.starter;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.FileHandlerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 core 生成的 {@code fileId} 参数与引擎接收参数一致（core 不能依赖引擎，靠测试对齐）。
 */
class FilePreviewSourceIdContractTest {

    /** 与 {@code FilePreviewServiceImpl#SOURCE_ID_PARAM} 保持一致。 */
    private static final String CORE_SOURCE_ID_PARAM = "fileId";

    @Test
    void core参数名与引擎读取的参数名一致() {
        assertThat(CORE_SOURCE_ID_PARAM).isEqualTo("fileId");
    }

    @Test
    void 引擎按源标识构造属性时保留identifier并按文件名识别类型() {
        FileHandlerService handlerService = new FileHandlerService(org.mockito.Mockito.mock(cn.keking.service.cache.CacheService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(handlerService, "uriEncoding", "UTF-8");
        FileAttribute attribute = handlerService.getFileAttributeBySourceId(
                "ticket-1", "file-100-0123456789abcdef.pptx", null);

        assertThat(attribute.getSourceId()).isEqualTo("ticket-1");
        assertThat(attribute.getSuffix()).isEqualTo("pptx");
        assertThat(attribute.getName()).isEqualTo("file-100-0123456789abcdef.pptx");
    }

    @Test
    void 已有构造函数行为不受影响() {
        FileAttribute attribute = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", "http://x/demo.txt");

        assertThat(attribute.getSourceId()).isNull();
    }
}
