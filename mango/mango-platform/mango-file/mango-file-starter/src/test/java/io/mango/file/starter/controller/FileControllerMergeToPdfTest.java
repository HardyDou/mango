package io.mango.file.starter.controller;

import io.mango.common.result.R;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileControllerMergeToPdfTest {

    @Test
    void mergeToPdf_postJson_委托文件服务并返回新文件记录() throws Exception {
        IFileService fileService = mock(IFileService.class);
        FileRecordVO record = new FileRecordVO();
        record.setId(9001L);
        record.setFileName("材料合集.pdf");
        record.setContentType("application/pdf");
        when(fileService.mergeToPdf(any(FileMergePdfCommand.class))).thenReturn(R.ok(record));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileService)).build();

        mockMvc.perform(post("/file/files/merge-pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "材料合集",
                                  "targetFormat": "PDF",
                                  "purpose": "archive",
                                  "bizType": "case",
                                  "bizId": "C-1001",
                                  "directoryId": 12,
                                  "entries": [
                                    {"fileId": 101, "title": "身份证正面"},
                                    {"fileId": 102, "title": "申请书"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(9001))
                .andExpect(jsonPath("$.data.fileName").value("材料合集.pdf"))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"));

        ArgumentCaptor<FileMergePdfCommand> captor = ArgumentCaptor.forClass(FileMergePdfCommand.class);
        verify(fileService).mergeToPdf(captor.capture());
        FileMergePdfCommand command = captor.getValue();
        assertThat(command.getFileName()).isEqualTo("材料合集");
        assertThat(command.getTargetFormat()).isEqualTo("PDF");
        assertThat(command.getPurpose()).isEqualTo("archive");
        assertThat(command.getBizType()).isEqualTo("case");
        assertThat(command.getBizId()).isEqualTo("C-1001");
        assertThat(command.getDirectoryId()).isEqualTo(12L);
        assertThat(command.getEntries()).hasSize(2);
        assertThat(command.getEntries().get(0).getFileId()).isEqualTo(101L);
        assertThat(command.getEntries().get(0).getTitle()).isEqualTo("身份证正面");
        assertThat(command.getEntries().get(1).getFileId()).isEqualTo(102L);
        assertThat(command.getEntries().get(1).getTitle()).isEqualTo("申请书");
    }
}
