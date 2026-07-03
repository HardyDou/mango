package io.mango.file.starter.controller;

import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.core.service.IFileService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileControllerDownloadResponseTest {

    @Test
    void downloadResponse_中文文件名_响应头只做一次协议编码() throws Exception {
        String fileName = "基准履约保函项目NOAF-1782780291894-已编辑+基准申请企业NOAF-1782780291894+288000.zip";
        IFileService fileService = mock(IFileService.class);
        when(fileService.download(1001L, null, null)).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)),
                fileName,
                "application/zip",
                2L));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileService)).build();

        MvcResult result = mockMvc.perform(get("/file/files/download").param("id", "1001"))
                .andExpect(status().isOk())
                .andReturn();

        String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).isNotBlank();
        assertThat(disposition).doesNotContain("%25E5");
        assertThat(ContentDisposition.parse(disposition).getFilename()).isEqualTo(fileName);
    }
}
