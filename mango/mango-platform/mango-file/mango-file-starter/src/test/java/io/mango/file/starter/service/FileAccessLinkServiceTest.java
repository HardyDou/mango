package io.mango.file.starter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.enums.FileAccessAction;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAccessLinkServiceTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void issuedLinkRestoresTenantAndKeepsAction() {
        IFileService files = mock(IFileService.class);
        IFileSettingsService settings = mock(IFileSettingsService.class);
        MemoryTokenStore tokens = new MemoryTokenStore();
        FileRecordVO record = new FileRecordVO();
        record.setId(9L);
        when(files.get(9L)).thenReturn(R.ok(record));
        FileSettingsVO configuration = new FileSettingsVO();
        configuration.setAccessTokenExpireSeconds(600L);
        when(settings.current()).thenReturn(configuration);
        FileDownloadVO download = new FileDownloadVO(new ByteArrayInputStream(new byte[]{1}), "a.pdf",
                "application/pdf", 1L);
        when(files.downloadForService(9L)).thenAnswer(ignored -> {
            assertThat(MangoContextHolder.get().tenantId()).isEqualTo("42");
            return download;
        });
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("42"));
        FileAccessLinkService service = new FileAccessLinkService(files, settings, tokens, new ObjectMapper());

        String token = service.create(9L, FileAccessAction.PREVIEW).getUrl().substring("/file/files/access?token=".length());
        FileAccessLinkService.AccessContent opened = service.open(token);

        assertThat(opened.action()).isEqualTo(FileAccessAction.PREVIEW);
        assertThat(opened.download()).isSameAs(download);
    }

    @Test
    void expiredLinkIsRejected() {
        MemoryTokenStore tokens = new MemoryTokenStore();
        tokens.store("file:access:expired",
                "{\"fileId\":9,\"action\":\"DOWNLOAD\",\"tenantId\":\"42\",\"expiresAt\":0}", 600);
        FileAccessLinkService service = new FileAccessLinkService(mock(IFileService.class),
                mock(IFileSettingsService.class), tokens, new ObjectMapper());

        assertThatThrownBy(() -> service.open("expired")).isInstanceOf(BizException.class);
    }

    private static class MemoryTokenStore implements ITokenStore {
        private final Map<String, String> values = new HashMap<>();
        public void store(String token, String value, long ttlSeconds) { values.put(token, value); }
        public String get(String token) { return values.get(token); }
        public void remove(String token) { values.remove(token); }
    }
}
