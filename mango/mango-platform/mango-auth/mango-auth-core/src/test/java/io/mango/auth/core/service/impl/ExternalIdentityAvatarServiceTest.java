package io.mango.auth.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalIdentityAvatarServiceTest {

    @Test
    void importsAvatarAsManagedExternalIdentityFile() {
        FileImportApi fileImportApi = mock(FileImportApi.class);
        FileApi fileApi = mock(FileApi.class);
        FileRecordVO file = new FileRecordVO();
        file.setId(22L);
        when(fileImportApi.importImage(any())).thenReturn(R.ok(file));
        ExternalIdentityAvatarService service = new ExternalIdentityAvatarService(fileImportApi, fileApi);

        assertThat(service.importAvatar(7L, "https://wework.qpic.cn/avatar.png")).isEqualTo(22L);

        ArgumentCaptor<ImportRemoteImageCommand> commandCaptor =
                ArgumentCaptor.forClass(ImportRemoteImageCommand.class);
        verify(fileImportApi).importImage(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getSourceUrl()).isEqualTo("https://wework.qpic.cn/avatar.png");
        assertThat(commandCaptor.getValue().getBizType()).isEqualTo("identity-external-avatar");
        assertThat(commandCaptor.getValue().getBizId()).isEqualTo("7");
    }

    @Test
    void rejectsAnEmptyFileImportResult() {
        FileImportApi fileImportApi = mock(FileImportApi.class);
        FileApi fileApi = mock(FileApi.class);
        when(fileImportApi.importImage(any())).thenReturn(R.ok());
        ExternalIdentityAvatarService service = new ExternalIdentityAvatarService(fileImportApi, fileApi);

        assertThatThrownBy(() -> service.importAvatar(7L, "https://wework.qpic.cn/avatar.png"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("头像导入失败");
    }

    @Test
    void deletesReplacedManagedAvatar() {
        FileImportApi fileImportApi = mock(FileImportApi.class);
        FileApi fileApi = mock(FileApi.class);
        when(fileApi.delete(any())).thenReturn(R.ok(true));
        ExternalIdentityAvatarService service = new ExternalIdentityAvatarService(fileImportApi, fileApi);

        service.deleteAvatar(11L);

        ArgumentCaptor<FileDeleteCommand> commandCaptor = ArgumentCaptor.forClass(FileDeleteCommand.class);
        verify(fileApi).delete(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getIds()).containsExactly(11L);
    }
}
