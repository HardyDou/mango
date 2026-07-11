package io.mango.file.starter.controller;

import io.mango.common.result.R;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileControllerRecordSerializationTest {

    @Test
    void detail_隐藏存储层和存储公开访问字段() throws Exception {
        IFileService fileService = mock(IFileService.class);
        FileRecordVO record = new FileRecordVO();
        record.setId(1001L);
        record.setTenantId(2001L);
        record.setObjectId(3001L);
        record.setStorageType("MINIO");
        record.setStorageConfigId(4001L);
        record.setBucketName("mango-file");
        record.setObjectName("private/1001.png");
        record.setFileName("申请材料.png");
        record.setFileSize(1024L);
        record.setContentType("image/png");
        record.setUrl("https://storage.example.com/private/1001.png");
        record.setPreviewUrl("/api/file/files/preview-content?id=1001");
        record.setDownloadUrl("/api/file/files/download?id=1001");
        record.setDirectAccess(true);
        record.setDirectPreviewUrl("https://storage.example.com/private/1001-preview.png");
        record.setDirectDownloadUrl("https://storage.example.com/private/1001-download.png");
        record.setDirectPreviewExpireSeconds(300L);
        record.setDirectDownloadExpireSeconds(300L);
        when(fileService.get(1001L)).thenReturn(R.ok(record));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileService)).build();

        mockMvc.perform(get("/file/files/detail").param("id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.fileName").value("申请材料.png"))
                .andExpect(jsonPath("$.data.previewUrl").value("/api/file/files/preview-content?id=1001"))
                .andExpect(jsonPath("$.data.downloadUrl").value("/api/file/files/download?id=1001"))
                .andExpect(jsonPath("$.data.objectId").doesNotExist())
                .andExpect(jsonPath("$.data.storageType").doesNotExist())
                .andExpect(jsonPath("$.data.storageConfigId").doesNotExist())
                .andExpect(jsonPath("$.data.bucketName").doesNotExist())
                .andExpect(jsonPath("$.data.objectName").doesNotExist())
                .andExpect(jsonPath("$.data.url").doesNotExist())
                .andExpect(jsonPath("$.data.directAccess").doesNotExist())
                .andExpect(jsonPath("$.data.directPreviewUrl").doesNotExist())
                .andExpect(jsonPath("$.data.directDownloadUrl").doesNotExist())
                .andExpect(jsonPath("$.data.directPreviewExpireSeconds").doesNotExist())
                .andExpect(jsonPath("$.data.directDownloadExpireSeconds").doesNotExist());
    }
}
