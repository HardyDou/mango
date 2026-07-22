package io.mango.file.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.remote.IRemoteImageFetcher;
import io.mango.file.core.service.remote.RemoteImageContent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RemoteFileImportServiceTest {

    @Test
    void importImage_reusesFileServiceAndForcesManagedImageSemantics() throws Exception {
        IRemoteImageFetcher fetcher = mock(IRemoteImageFetcher.class);
        IFileService fileService = mock(IFileService.class);
        byte[] imageBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        when(fetcher.fetch("https://public.example/image.png"))
                .thenReturn(new RemoteImageContent(imageBytes, "image/png", "png"));
        FileRecordVO expected = mock(FileRecordVO.class);
        when(fileService.save(org.mockito.ArgumentMatchers.any(SaveFileCommand.class))).thenReturn(expected);
        RemoteFileImportService service = new RemoteFileImportService(fetcher, fileService);
        ImportRemoteImageCommand command = new ImportRemoteImageCommand();
        command.setSourceUrl("https://public.example/image.png");
        command.setBizType("EDITOR");
        command.setBizId("ARTICLE-1");
        command.setBizMeta("{\"scene\":\"rich-text\"}");
        command.setDirectoryId(12L);

        FileRecordVO actual = service.importImage(command);

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<SaveFileCommand> captor = ArgumentCaptor.forClass(SaveFileCommand.class);
        verify(fileService).save(captor.capture());
        SaveFileCommand saved = captor.getValue();
        assertThat(saved.getInputStream().readAllBytes()).containsExactly(imageBytes);
        assertThat(saved.getFileName()).isEqualTo("remote-image.png");
        assertThat(saved.getFileSize()).isEqualTo((long) imageBytes.length);
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getPurpose()).isEqualTo("image");
        assertThat(saved.getAccessLevel()).isEqualTo(FileAccessLevel.PRIVATE.name());
        assertThat(saved.getBizType()).isEqualTo("EDITOR");
        assertThat(saved.getBizId()).isEqualTo("ARTICLE-1");
        assertThat(saved.getBizMeta()).isEqualTo("{\"scene\":\"rich-text\"}");
        assertThat(saved.getDirectoryId()).isEqualTo(12L);
    }

    @Test
    void importImage_rejectsPersistedUrlInBizMetaBeforeFetching() {
        IRemoteImageFetcher fetcher = mock(IRemoteImageFetcher.class);
        IFileService fileService = mock(IFileService.class);
        RemoteFileImportService service = new RemoteFileImportService(fetcher, fileService);
        ImportRemoteImageCommand command = new ImportRemoteImageCommand();
        command.setSourceUrl("https://public.example/image.png");
        command.setBizMeta("{\"source\":\"https://third-party.example/image.png\"}");

        assertThatThrownBy(() -> service.importImage(command))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getCode()).isEqualTo(FileCode.FILE_REMOTE_URL_INVALID.getCode()));
        verifyNoInteractions(fetcher, fileService);
    }
}
