package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.ReplaceFileContentCommand;
import io.mango.file.api.command.UpdateFileRecordCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileDuplicateNameStrategy;
import io.mango.file.api.enums.FileObjectNameStrategy;
import io.mango.file.api.enums.FileObjectStatus;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.enums.FileUploadMode;
import io.mango.file.api.enums.FileUploadSessionStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.api.vo.FileUploadInitVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileHashMappingEntity;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecordEntity;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.entity.FileUploadPartEntity;
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
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.multipart.MultipartFile;

class FileServiceReplaceContentTest {

    private static final Long TENANT_ID = 1001L;

    private FileStorageRouter storageRouter;
    private IFileStorageConfigService storageConfigService;
    private FileRecordMapper fileRecordMapper;
    private FileObjectMapper fileObjectMapper;
    private FileHashMappingMapper hashMappingMapper;
    private FileUploadSessionMapper uploadSessionMapper;
    private FileUploadPartMapper uploadPartMapper;
    private FileSettingsVO runtimeSettings;
    private FileStorageConfigEntity storageConfig;
    private FileService fileService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FileObjectEntity.class);
        TableInfoHelper.initTableInfo(assistant, FileRecordEntity.class);
        TableInfoHelper.initTableInfo(assistant, FileHashMappingEntity.class);
        TableInfoHelper.initTableInfo(assistant, FileUploadSessionEntity.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(2001L, "1001", "tester", "admin", "USER", "USER", 2001L, "test"));

        storageRouter = mock(FileStorageRouter.class);
        storageConfigService = mock(IFileStorageConfigService.class);
        IFileSettingsService settingsService = mock(IFileSettingsService.class);
        IFileDirectoryService directoryService = mock(IFileDirectoryService.class);
        fileRecordMapper = mock(FileRecordMapper.class);
        fileObjectMapper = mock(FileObjectMapper.class);
        hashMappingMapper = mock(FileHashMappingMapper.class);
        uploadSessionMapper = mock(FileUploadSessionMapper.class);
        uploadPartMapper = mock(FileUploadPartMapper.class);
        FileDirectoryMapper directoryMapper = mock(FileDirectoryMapper.class);

        storageConfig = new FileStorageConfigEntity();
        storageConfig.setId(10L);
        storageConfig.setTenantId(TENANT_ID);
        storageConfig.setStorageType("LOCAL");
        storageConfig.setBucketName("local");
        storageConfig.setStoragePath("");
        storageConfig.setStatus(1);
        when(storageConfigService.activeConfig()).thenReturn(storageConfig);
        when(storageConfigService.getEnabledConfig(any())).thenReturn(storageConfig);
        runtimeSettings = settings();
        when(settingsService.current()).thenReturn(runtimeSettings);
        when(fileObjectMapper.insert(any(FileObjectEntity.class))).thenAnswer(invocation -> {
            FileObjectEntity entity = invocation.getArgument(0);
            entity.setId(77L);
            return 1;
        });
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
    void tearDown() throws Exception {
        MangoContextHolder.clear();
    }

    @Test
    void replaceContent_whenExtensionDiffers_rejectsBeforeWritingObject() throws Exception {
        FileRecordEntity record = record("report.docx", "docx", "old-content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        MultipartFile file = multipart("report.pdf", "new-content", "application/pdf");

        assertThatThrownBy(() -> fileService.replaceContent(file, replaceCommand(record.getId())))
                .isInstanceOf(RuntimeException.class);

        verify(storageRouter, never()).putObject(any(), anyString(), any(), anyLong(), any());
        verify(fileRecordMapper, never()).updateById(any(FileRecordEntity.class));
    }

    @Test
    void replaceContent_whenContentUnchanged_keepsObjectAndRewritesMetadataOnly() throws Exception {
        String content = "same-content";
        FileRecordEntity record = record("report.docx", "docx", content);
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        MultipartFile file = multipart("report.docx", content, "application/msword");
        ReplaceFileContentCommand command = replaceCommand(record.getId());
        command.setPurpose("contract");

        FileRecordVO result = fileService.replaceContent(file, command);

        assertThat(result.getPurpose()).isEqualTo("contract");
        assertThat(result.getObjectId()).isEqualTo(record.getObjectId());
        verify(storageRouter, never()).putObject(any(), anyString(), any(), anyLong(), any());
        verify(fileObjectMapper, never()).update(isNull(), any());
        verify(fileRecordMapper).updateById(record);
    }

    @Test
    void replaceContent_whenContentChanged_pointsRecordToNewObjectAndReleasesOldReference() throws Exception {
        FileRecordEntity record = record("report.docx", "docx", "old-content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        String newContent = "new-content";
        MultipartFile file = multipart("report.docx", newContent, "application/msword");

        FileRecordVO result = fileService.replaceContent(file, replaceCommand(record.getId()));

        assertThat(result.getObjectId()).isEqualTo(77L);
        assertThat(result.getFileSize()).isEqualTo((long) newContent.getBytes(StandardCharsets.UTF_8).length);
        assertThat(result.getFileHash()).isEqualTo(sha256(newContent));
        verify(storageRouter).putObject(eq(storageConfig), anyString(), any(), anyLong(), any());
        verify(fileObjectMapper, times(2)).update(isNull(), any());
    }

    @Test
    void replaceContent_whenPhysicalDeleteEnabled_removesUnreferencedOldObject() throws Exception {
        runtimeSettings.setPhysicalDeleteEnabled(true);
        FileRecordEntity record = record("report.docx", "docx", "old-content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        FileObjectEntity oldObject = new FileObjectEntity();
        oldObject.setId(44L);
        oldObject.setStorageConfigId(10L);
        oldObject.setStorageType("LOCAL");
        oldObject.setBucketName("local");
        oldObject.setObjectName("tenant-1001/old.docx");
        oldObject.setStatus(FileObjectStatus.COMPLETED.value());
        when(fileObjectMapper.selectById(44L)).thenReturn(oldObject);
        String previousObjectName = record.getObjectName();
        String newContent = "new-content";
        MultipartFile file = multipart("report.docx", newContent, "application/msword");

        fileService.replaceContent(file, replaceCommand(record.getId()));

        verify(storageRouter).removeObject(storageConfig, previousObjectName);
    }

    @Test
    void updateMetadata_whenExtensionDiffers_rejectsChange() throws Exception {
        FileRecordEntity record = record("report.docx", "docx", "content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        UpdateFileRecordCommand command = new UpdateFileRecordCommand();
        command.setId(record.getId());
        command.setFileName("report.pdf");

        assertThatThrownBy(() -> fileService.updateMetadata(command))
                .isInstanceOf(RuntimeException.class);
        verify(fileRecordMapper, never()).updateById(any(FileRecordEntity.class));
    }

    @Test
    void updateMetadata_updatesBusinessFieldsWithoutReplacingContent() throws Exception {
        FileRecordEntity record = record("report.docx", "docx", "content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        UpdateFileRecordCommand command = new UpdateFileRecordCommand();
        command.setId(record.getId());
        command.setFileName("report-final.docx");
        command.setPurpose("contract");
        command.setBizType("guarantee");
        command.setBizId("B-1");

        FileRecordVO result = fileService.updateMetadata(command);

        assertThat(result.getFileName()).isEqualTo("report-final.docx");
        assertThat(result.getPurpose()).isEqualTo("contract");
        assertThat(result.getBizType()).isEqualTo("guarantee");
        assertThat(result.getBizId()).isEqualTo("B-1");
        assertThat(result.getFileHash()).isEqualTo(record.getFileHash());
        verify(storageRouter, never()).putObject(any(), anyString(), any(), anyLong(), any());
    }

    @Test
    void createUploadSession_withReplaceTarget_keepsTargetMetadataAndRecordsReplaceId() throws Exception {
        FileRecordEntity record = record("report.docx", "docx", "content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        when(storageRouter.supportsMultipartUpload(any())).thenReturn(false);
        CreateFileUploadSessionCommand command = sessionCommand("report.docx");

        FileUploadInitVO result = fileService.createUploadSession(command);

        assertThat(result.getInstant()).isFalse();
        assertThat(result.getUploadMode()).isEqualTo(FileUploadMode.SERVER_CHUNK.name());
        verify(uploadSessionMapper).insert(org.mockito.ArgumentMatchers.argThat((FileUploadSessionEntity session) ->
                record.getId().equals(session.getReplaceFileId())
                        && record.getFileName().equals(session.getFileName())
                        && record.getPurpose().equals(session.getPurpose())
                        && record.getAccessLevel().equals(session.getAccessLevel())));
    }

    @Test
    void completeUploadSession_withReplaceTarget_updatesTargetRecordInsteadOfCreatingOne() throws Exception {
        FileRecordEntity record = record("report.docx", "docx", "old-content");
        when(fileRecordMapper.selectOne(any())).thenReturn(record);
        when(uploadSessionMapper.selectOne(any())).thenReturn(session());
        FileUploadPartEntity first = part(1, "etag-1");
        FileUploadPartEntity second = part(2, "etag-2");
        when(uploadPartMapper.selectList(any())).thenReturn(List.of(first, second));

        FileRecordVO result = fileService.completeUploadSession(99L);

        assertThat(result.getId()).isEqualTo(record.getId());
        assertThat(result.getObjectId()).isEqualTo(77L);
        verify(fileRecordMapper, never()).insert(any(FileRecordEntity.class));
        verify(fileRecordMapper).updateById(record);
        verify(storageRouter).completeMultipartUpload(any(), anyString(), anyString(), any());
    }

    private FileUploadSessionEntity session() throws Exception {
        FileUploadSessionEntity session = new FileUploadSessionEntity();
        session.setId(99L);
        session.setTenantId(TENANT_ID);
        session.setStorageConfigId(10L);
        session.setStorageType("LOCAL");
        session.setBucketName("local");
        session.setObjectName("tenant-1001/2026/01/01/new.docx");
        session.setUploadMode(FileUploadMode.S3_MULTIPART.name());
        session.setStorageUploadId("upload-1");
        session.setFileName("report.docx");
        session.setFileExt("docx");
        session.setFileHash("a".repeat(64));
        session.setFileSize(10L * 1024 * 1024);
        session.setContentType("application/msword");
        session.setChunkSize(5L * 1024 * 1024);
        session.setTotalParts(2);
        session.setUploadedParts(2);
        session.setStatus(FileUploadSessionStatus.UPLOADING.name());
        session.setPurpose("attachment");
        session.setAccessLevel(FileAccessLevel.PRIVATE.name());
        session.setDirectoryId(0L);
        session.setReplaceFileId(55L);
        return session;
    }

    private FileUploadPartEntity part(int number, String etag) {
        FileUploadPartEntity part = new FileUploadPartEntity();
        part.setSessionId(99L);
        part.setPartNumber(number);
        part.setEtag(etag);
        part.setStatus("COMPLETED");
        return part;
    }

    private FileRecordEntity record(String fileName, String fileExt, String content) {
        FileRecordEntity record = new FileRecordEntity();
        record.setId(55L);
        record.setTenantId(TENANT_ID);
        record.setFileName(fileName);
        record.setFileExt(fileExt);
        record.setFileSize((long) content.getBytes(StandardCharsets.UTF_8).length);
        record.setFileHash(sha256(content));
        record.setContentType("application/msword");
        record.setObjectId(44L);
        record.setStorageConfigId(10L);
        record.setStorageType("LOCAL");
        record.setBucketName("local");
        record.setObjectName("tenant-1001/old.docx");
        record.setStatus(FileRecordStatus.COMPLETED.value());
        record.setArchived(0);
        record.setPurpose("attachment");
        record.setAccessLevel(FileAccessLevel.PRIVATE.name());
        record.setDirectoryId(0L);
        return record;
    }

    private MultipartFile multipart(String fileName, String content, String contentType) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getSize()).thenReturn((long) bytes.length);
        when(file.getContentType()).thenReturn(contentType);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(bytes));
        return file;
    }

    private ReplaceFileContentCommand replaceCommand(Long id) {
        ReplaceFileContentCommand command = new ReplaceFileContentCommand();
        command.setId(id);
        return command;
    }

    private CreateFileUploadSessionCommand sessionCommand(String fileName) {
        CreateFileUploadSessionCommand command = new CreateFileUploadSessionCommand();
        command.setFileName(fileName);
        command.setFileSize(10L * 1024 * 1024);
        command.setContentType("application/msword");
        command.setChunkSize(5L * 1024 * 1024);
        command.setTotalParts(2);
        command.setReplaceFileId(55L);
        return command;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private FileSettingsVO settings() throws Exception {
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
        settings.setPhysicalDeleteEnabled(false);
        return settings;
    }
}
