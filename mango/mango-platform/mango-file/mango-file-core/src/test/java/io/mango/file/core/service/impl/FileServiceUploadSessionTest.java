package io.mango.file.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileDuplicateNameStrategy;
import io.mango.file.api.enums.FileObjectNameStrategy;
import io.mango.file.api.enums.FileUploadMode;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.api.vo.FileUploadInitVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileHashMappingEntity;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.entity.FileUploadSessionEntity;
import io.mango.file.core.mapper.FileDirectoryMapper;
import io.mango.file.core.mapper.FileHashMappingMapper;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileUploadPartMapper;
import io.mango.file.core.mapper.FileUploadSessionMapper;
import io.mango.file.core.service.IFileDirectoryService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.file.core.storage.MultipartUpload;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceUploadSessionTest {

    private FileUploadSessionMapper uploadSessionMapper;
    private FileStorageRouter storageRouter;
    private FileObjectMapper fileObjectMapper;
    private FileHashMappingMapper hashMappingMapper;
    private FileSettingsVO runtimeSettings;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(2001L, "1001", "tester", "admin", "USER", "USER", 2001L, "test"));

        storageRouter = mock(FileStorageRouter.class);
        IFileStorageConfigService storageConfigService = mock(IFileStorageConfigService.class);
        IFileSettingsService settingsService = mock(IFileSettingsService.class);
        IFileDirectoryService directoryService = mock(IFileDirectoryService.class);
        FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
        fileObjectMapper = mock(FileObjectMapper.class);
        hashMappingMapper = mock(FileHashMappingMapper.class);
        uploadSessionMapper = mock(FileUploadSessionMapper.class);
        FileUploadPartMapper uploadPartMapper = mock(FileUploadPartMapper.class);
        FileDirectoryMapper directoryMapper = mock(FileDirectoryMapper.class);

        FileStorageConfigEntity storageConfig = new FileStorageConfigEntity();
        storageConfig.setId(10L);
        storageConfig.setTenantId(1001L);
        storageConfig.setStorageType("MINIO");
        storageConfig.setBucketName("files");
        storageConfig.setStoragePath("");
        storageConfig.setStatus(1);
        when(storageConfigService.activeConfig()).thenReturn(storageConfig);
        runtimeSettings = settings();
        when(settingsService.current()).thenReturn(runtimeSettings);
        when(storageRouter.supportsMultipartUpload(storageConfig)).thenReturn(true);
        when(uploadSessionMapper.insert(any(FileUploadSessionEntity.class))).thenAnswer(invocation -> {
            FileUploadSessionEntity session = invocation.getArgument(0);
            session.setId(99L);
            return 1;
        });

        fileService = new FileService(
                storageRouter,
                storageConfigService,
                settingsService,
                directoryService,
                fileRecordMapper,
                fileObjectMapper,
                hashMappingMapper,
                uploadSessionMapper,
                uploadPartMapper,
                directoryMapper,
                new ObjectMapper(),
                new FileAccessUrlAssembler(new FileProperties()),
                List.of(),
                List.of(),
                List.of(),
                new FilePackageSizeControlProcessor(List.of()));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createUploadSession_withoutClientHash_fallsBackToServerChunks() {
        CreateFileUploadSessionCommand command = new CreateFileUploadSessionCommand();
        command.setFileName("large.bin");
        command.setFileSize(30L * 1024 * 1024);
        command.setContentType("application/octet-stream");
        command.setChunkSize(10L * 1024 * 1024);
        command.setTotalParts(3);

        FileUploadInitVO result = fileService.createUploadSession(command);

        assertThat(result.getSessionId()).isEqualTo(99L);
        assertThat(result.getUploadMode()).isEqualTo(FileUploadMode.SERVER_CHUNK.name());
    }

    @Test
    void createUploadSession_withClientHash_keepsNativeMultipartUpload() {
        when(storageRouter.initiateMultipartUpload(any(), any(), any()))
                .thenReturn(new MultipartUpload("storage-upload-1"));
        CreateFileUploadSessionCommand command = uploadCommand();
        command.setFileHash("a".repeat(64));

        FileUploadInitVO result = fileService.createUploadSession(command);

        assertThat(result.getUploadMode()).isEqualTo(FileUploadMode.S3_MULTIPART.name());
        assertThat(result.getStorageUploadId()).isEqualTo("storage-upload-1");
        verify(storageRouter).initiateMultipartUpload(any(), any(), any());
    }

    @Test
    void createUploadSession_withMissingInstantObject_fallsBackToNormalUpload() {
        runtimeSettings.setInstantUploadEnabled(true);
        FileHashMappingEntity mapping = new FileHashMappingEntity();
        mapping.setObjectId(88L);
        mapping.setStatus(1);
        when(hashMappingMapper.selectOne(any())).thenReturn(mapping);
        when(storageRouter.supportsMultipartUpload(any())).thenReturn(false);
        CreateFileUploadSessionCommand command = uploadCommand();
        command.setFileHash("a".repeat(64));

        FileUploadInitVO result = fileService.createUploadSession(command);

        assertThat(result.getInstant()).isFalse();
        assertThat(result.getUploadMode()).isEqualTo(FileUploadMode.SERVER_CHUNK.name());
        verify(hashMappingMapper).updateById(mapping);
        verify(fileObjectMapper, never()).update(any(), any());
    }

    @Test
    void createUploadSession_whenMultipartIsDisabled_rejectsTheSession() {
        runtimeSettings.setMultipartEnabled(false);

        assertThatThrownBy(() -> fileService.createUploadSession(uploadCommand()))
                .isInstanceOf(RuntimeException.class);
        verify(uploadSessionMapper, never()).insert(any(FileUploadSessionEntity.class));
    }

    private CreateFileUploadSessionCommand uploadCommand() {
        CreateFileUploadSessionCommand command = new CreateFileUploadSessionCommand();
        command.setFileName("large.bin");
        command.setFileSize(30L * 1024 * 1024);
        command.setContentType("application/octet-stream");
        command.setChunkSize(10L * 1024 * 1024);
        command.setTotalParts(3);
        return command;
    }

    private FileSettingsVO settings() {
        FileSettingsVO settings = new FileSettingsVO();
        settings.setMaxSize(100L * 1024 * 1024);
        settings.setAllowedExtensions(List.of());
        settings.setBlockedExtensions(List.of());
        settings.setDefaultAccessLevel(FileAccessLevel.PRIVATE.name());
        settings.setDuplicateNameStrategy(FileDuplicateNameStrategy.ALLOW.name());
        settings.setDuplicateCheckDirectoryScoped(true);
        settings.setObjectNameStrategy(FileObjectNameStrategy.DATE_UUID.name());
        settings.setInstantUploadEnabled(false);
        settings.setMultipartEnabled(true);
        settings.setMultipartThreshold(20L * 1024 * 1024);
        settings.setContentTypeCheckEnabled(false);
        settings.setAllowedContentTypes(List.of());
        settings.setBlockedContentTypes(List.of());
        return settings;
    }
}
