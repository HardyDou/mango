package io.mango.file.starter.controller;

import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IRemoteFileImportService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileImportControllerTest {

    @Test
    void importImage_acceptsOnlySourceAndBusinessOwnershipContract() throws Exception {
        IRemoteFileImportService service = mock(IRemoteFileImportService.class);
        FileRecordVO record = new FileRecordVO();
        record.setId(1001L);
        record.setFileName("remote-image.png");
        record.setContentType("image/png");
        record.setPurpose("image");
        record.setAccessLevel("PRIVATE");
        when(service.importImage(org.mockito.ArgumentMatchers.any(ImportRemoteImageCommand.class)))
                .thenReturn(record);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FileImportController(service)).build();

        mockMvc.perform(post("/file/files/import-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceUrl": "https://public.example/image.png",
                                  "bizType": "EDITOR",
                                  "bizId": "ARTICLE-1",
                                  "purpose": "attachment",
                                  "accessLevel": "PUBLIC_READ"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.purpose").value("image"))
                .andExpect(jsonPath("$.data.accessLevel").value("PRIVATE"));

        ArgumentCaptor<ImportRemoteImageCommand> captor = ArgumentCaptor.forClass(ImportRemoteImageCommand.class);
        verify(service).importImage(captor.capture());
        assertThat(captor.getValue().getSourceUrl()).isEqualTo("https://public.example/image.png");
        assertThat(captor.getValue().getBizType()).isEqualTo("EDITOR");
        assertThat(captor.getValue().getBizId()).isEqualTo("ARTICLE-1");
    }
}
