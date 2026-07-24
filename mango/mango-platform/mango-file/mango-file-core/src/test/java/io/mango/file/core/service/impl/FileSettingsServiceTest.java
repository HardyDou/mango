package io.mango.file.core.service.impl;

import io.mango.file.api.command.SaveFileSettingsCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileAccessMode;
import io.mango.file.api.enums.FileDuplicateNameStrategy;
import io.mango.file.api.enums.FileInstantUploadScope;
import io.mango.file.api.enums.FileObjectNameStrategy;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileSettingsEntity;
import io.mango.file.core.mapper.FileSettingsMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileSettingsServiceTest {

    @Test
    void save_空列表_清空历史限制配置() {
        FileSettingsMapper mapper = mock(FileSettingsMapper.class);
        FileSettingsService service = new FileSettingsService(mapper, new FileProperties());
        FileSettingsEntity existing = existingSettings();
        when(mapper.selectOne(any())).thenReturn(existing);

        service.save(emptyListSettings());

        verify(mapper).updateById(existing);
        assertThat(existing.getAllowedExtensions()).isEmpty();
        assertThat(existing.getBlockedExtensions()).isEmpty();
        assertThat(existing.getAllowedContentTypes()).isEmpty();
        assertThat(existing.getBlockedContentTypes()).isEmpty();
        assertThat(existing.getPreviewExternalExtensions()).isEmpty();
        assertThat(existing.getDuplicateCheckDirectoryScoped()).isOne();
        assertThat(existing.getInstantUploadEnabled()).isOne();
        assertThat(existing.getMultipartEnabled()).isOne();
        assertThat(existing.getMultipartThreshold()).isEqualTo(20L * 1024 * 1024);
        assertThat(existing.getContentTypeCheckEnabled()).isOne();
        assertThat(existing.getDirectUploadEnabled()).isZero();
        assertThat(existing.getDirectUploadExpireSeconds()).isEqualTo(900L);
        assertThat(existing.getAccessTokenEnabled()).isOne();
        assertThat(existing.getPublicReadRequiresToken()).isOne();
        assertThat(existing.getAccessMode()).isEqualTo(FileAccessMode.PROXY.name());
        assertThat(existing.getAccessTokenExpireSeconds()).isEqualTo(86400L);
        assertThat(existing.getPreviewProviderUrl()).isEqualTo("/file-preview/files/preview");
        assertThat(existing.getPreviewExpireSeconds()).isEqualTo(86400L);
        assertThat(existing.getArchiveRetainEnabled()).isOne();
        assertThat(existing.getArchiveRetainDays()).isEqualTo(180);
        assertThat(existing.getArchiveRestoreEnabled()).isZero();
        assertThat(existing.getPhysicalDeleteEnabled()).isZero();

        FileSettingsVO current = service.current();
        assertThat(current.getAllowedExtensions()).isEmpty();
        assertThat(current.getBlockedExtensions()).isEmpty();
        assertThat(current.getAllowedContentTypes()).isEmpty();
        assertThat(current.getBlockedContentTypes()).isEmpty();
        assertThat(current.getPreviewExternalExtensions()).isEmpty();
    }

    private FileSettingsEntity existingSettings() {
        FileSettingsEntity entity = new FileSettingsEntity();
        entity.setId(1L);
        entity.setTenantId(1L);
        entity.setAllowedExtensions("pdf,docx");
        entity.setBlockedExtensions("exe");
        entity.setAllowedContentTypes("application/pdf");
        entity.setBlockedContentTypes("application/x-msdownload");
        entity.setPreviewExternalExtensions("pdf,docx");
        return entity;
    }

    private SaveFileSettingsCommand emptyListSettings() {
        SaveFileSettingsCommand command = new SaveFileSettingsCommand();
        command.setMaxSize(1024L);
        command.setAllowedExtensions(List.of());
        command.setBlockedExtensions(List.of());
        command.setDefaultAccessLevel(FileAccessLevel.PRIVATE.name());
        command.setDuplicateNameStrategy(FileDuplicateNameStrategy.AUTO_RENAME.name());
        command.setDuplicateCheckDirectoryScoped(true);
        command.setObjectNameStrategy(FileObjectNameStrategy.DATE_UUID.name());
        command.setInstantUploadEnabled(true);
        command.setMultipartEnabled(true);
        command.setMultipartThreshold(20L * 1024 * 1024);
        command.setInstantUploadScope(FileInstantUploadScope.TENANT.name());
        command.setContentTypeCheckEnabled(true);
        command.setAllowedContentTypes(List.of());
        command.setBlockedContentTypes(List.of());
        command.setDirectUploadEnabled(false);
        command.setDirectUploadExpireSeconds(900L);
        command.setAccessTokenEnabled(true);
        command.setPublicReadRequiresToken(true);
        command.setAccessMode(FileAccessMode.PROXY.name());
        command.setAccessTokenExpireSeconds(86400L);
        command.setPreviewProviderUrl("/file-preview/files/preview");
        command.setPreviewExpireSeconds(86400L);
        command.setPreviewExternalExtensions(List.of());
        command.setArchiveRetainEnabled(true);
        command.setArchiveRetainDays(180);
        command.setArchiveRestoreEnabled(false);
        command.setPhysicalDeleteEnabled(false);
        return command;
    }
}
