package io.mango.file.starter.controller;

import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.enums.FilePackageSizeControlMode;
import io.mango.file.api.vo.FilePackageResultVO;
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

class FileControllerPackageSizeControlTest {

    @Test
    void packageFilesWithSizeControl_postJson_委托文件服务并返回大小控制结果() throws Exception {
        IFileService fileService = mock(IFileService.class);
        FileRecordVO record = new FileRecordVO();
        record.setId(9101L);
        record.setFileName("材料包.zip");
        record.setFileSize(4800L);
        FilePackageResultVO result = new FilePackageResultVO();
        result.setFile(record);
        result.setSizeControlMode(FilePackageSizeControlMode.AUTO);
        result.setMaxPackageSizeBytes(5000L);
        result.setActualPackageSizeBytes(4800L);
        result.setPackageTargetAchieved(true);
        when(fileService.packageFilesWithSizeControl(any(FilePackageSizeControlCommand.class))).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileService)).build();

        mockMvc.perform(post("/file/files/package-size-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "材料包.zip",
                                  "sizeControlMode": "AUTO",
                                  "maxPackageSizeBytes": 5000,
                                  "compression": "MEDIUM",
                                  "entries": [
                                    {"fileId": 101, "path": "资料/合同.pdf"},
                                    {"fileId": 102, "path": "资料/现场照片.jpg", "compression": "NONE"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.file.id").value(9101))
                .andExpect(jsonPath("$.data.sizeControlMode").value("AUTO"))
                .andExpect(jsonPath("$.data.maxPackageSizeBytes").value(5000))
                .andExpect(jsonPath("$.data.actualPackageSizeBytes").value(4800))
                .andExpect(jsonPath("$.data.packageTargetAchieved").value(true));

        ArgumentCaptor<FilePackageSizeControlCommand> captor =
                ArgumentCaptor.forClass(FilePackageSizeControlCommand.class);
        verify(fileService).packageFilesWithSizeControl(captor.capture());
        FilePackageSizeControlCommand command = captor.getValue();
        assertThat(command.getSizeControlMode()).isEqualTo(FilePackageSizeControlMode.AUTO);
        assertThat(command.getMaxPackageSizeBytes()).isEqualTo(5000L);
        assertThat(command.getCompression()).isEqualTo("MEDIUM");
        assertThat(command.getEntries()).hasSize(2);
        assertThat(command.getEntries().get(1).getCompression()).isEqualTo("NONE");
    }
}
