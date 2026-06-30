package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.FilePackageEntryCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileObjectStatus;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecord;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.mapper.FileDirectoryMapper;
import io.mango.file.core.mapper.FileHashMappingMapper;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileUploadPartMapper;
import io.mango.file.core.mapper.FileUploadSessionMapper;
import io.mango.file.core.service.IFileDirectoryService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.fileproc.compress.FileCompressApi;
import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServicePackageFilesTest {

    private static final Long TENANT_ID = 1001L;
    private static final Long USER_ID = 2001L;

    private final Map<Long, FileRecord> records = new HashMap<>();
    private final Map<Long, FileObjectEntity> objects = new HashMap<>();
    private final Map<String, byte[]> storage = new HashMap<>();
    private final Deque<Long> sourceLookupIds = new ArrayDeque<>();

    private FileRecordMapper fileRecordMapper;
    private FileObjectMapper fileObjectMapper;
    private FileServiceImpl fileService;
    private long recordId;
    private long objectId;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FileObjectEntity.class);
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(USER_ID, String.valueOf(TENANT_ID), "tester", "admin", "USER", "USER", USER_ID, "test"));
        fileRecordMapper = mock(FileRecordMapper.class);
        fileObjectMapper = mock(FileObjectMapper.class);
        FileStorageRouter fileStorageRouter = mock(FileStorageRouter.class);
        IFileStorageConfigService storageConfigService = mock(IFileStorageConfigService.class);
        IFileSettingsService settingsService = mock(IFileSettingsService.class);
        IFileDirectoryService directoryService = mock(IFileDirectoryService.class);
        FileHashMappingMapper fileHashMappingMapper = mock(FileHashMappingMapper.class);
        FileUploadSessionMapper fileUploadSessionMapper = mock(FileUploadSessionMapper.class);
        FileUploadPartMapper fileUploadPartMapper = mock(FileUploadPartMapper.class);
        FileDirectoryMapper fileDirectoryMapper = mock(FileDirectoryMapper.class);
        FileAccessUrlAssembler accessUrlAssembler = new FileAccessUrlAssembler(new FileProperties());
        FileStorageConfig storageConfig = storageConfig();

        when(storageConfigService.activeConfig()).thenReturn(storageConfig);
        when(storageConfigService.getEnabledConfig(any(), any(), any())).thenReturn(storageConfig);
        when(settingsService.current()).thenReturn(settings());
        when(fileRecordMapper.selectOne(any())).thenAnswer(invocation -> records.get(sourceLookupIds.removeFirst()));
        when(fileRecordMapper.selectCount(any())).thenReturn(0L);
        when(fileRecordMapper.insert(any(FileRecord.class))).thenAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(++recordId);
            records.put(record.getId(), record);
            return 1;
        });
        when(fileObjectMapper.selectById(anyLong())).thenAnswer(invocation -> objects.get(invocation.getArgument(0)));
        when(fileObjectMapper.insert(any(FileObjectEntity.class))).thenAnswer(invocation -> {
            FileObjectEntity object = invocation.getArgument(0);
            object.setId(++objectId);
            objects.put(object.getId(), object);
            return 1;
        });
        when(fileObjectMapper.update(any(), any())).thenReturn(1);
        when(fileStorageRouter.getObject(any(), any())).thenAnswer(invocation -> {
            String objectName = invocation.getArgument(1);
            byte[] content = storage.get(objectName);
            return new FileObject(new ByteArrayInputStream(content), content.length, contentType(objectName));
        });
        try {
            org.mockito.Mockito.doAnswer(invocation -> {
                String objectName = invocation.getArgument(1);
                java.io.InputStream input = invocation.getArgument(2);
                storage.put(objectName, input.readAllBytes());
                return null;
            }).when(fileStorageRouter).putObject(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        fileService = new FileServiceImpl(fileStorageRouter,
                storageConfigService,
                settingsService,
                directoryService,
                fileRecordMapper,
                fileObjectMapper,
                fileHashMappingMapper,
                fileUploadSessionMapper,
                fileUploadPartMapper,
                fileDirectoryMapper,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                accessUrlAssembler,
                List.of(new StubFileCompressApi()));
        sourceFile(11L, 101L, "source/contract.pdf", "合同正文".getBytes(StandardCharsets.UTF_8), "application/pdf");
        sourceFile(12L, 102L, "source/license.pdf", "营业执照".getBytes(StandardCharsets.UTF_8), "application/pdf");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void packageFiles_按目录结构清单打包_保存新Zip并返回文件记录() throws Exception {
        FilePackageCommand command = command("签约资料包-GO20260628000011.zip",
                entry(11L, "签约资料/合同正文.pdf"),
                entry(12L, "企业基础资料/营业执照.pdf"));
        sourceLookupIds.addAll(List.of(11L, 12L));

        R<FileRecordVO> result = fileService.packageFiles(command);

        assertThat(result.isSuccess()).isTrue();
        FileRecordVO vo = result.getData();
        assertThat(vo.getId()).isGreaterThan(12L);
        assertThat(vo.getFileName()).isEqualTo("签约资料包-GO20260628000011.zip");
        assertThat(vo.getContentType()).isEqualTo("application/zip");
        assertThat(vo.getBizType()).isEqualTo("GUARANTEE_ORDER_MATERIAL_PACKAGE");
        assertThat(vo.getBizId()).isEqualTo("123456");
        assertThat(vo.getAccessLevel()).isEqualTo(FileAccessLevel.PRIVATE.name());
        assertThat(vo.getStatus()).isEqualTo(FileRecordStatus.COMPLETED.value());
        assertThat(vo.getFileSize()).isPositive();
        FileRecord savedRecord = records.get(vo.getId());
        byte[] zipContent = storage.get(objects.get(savedRecord.getObjectId()).getObjectName());
        assertThat(unzip(zipContent)).containsEntry("签约资料/合同正文.pdf", "合同正文")
                .containsEntry("企业基础资料/营业执照.pdf", "营业执照");
    }

    @Test
    void packageFiles_Zip路径变量使用源文件名_保存新Zip并返回文件记录() throws Exception {
        FilePackageCommand command = command("项目A+被保人B+100万元.zip",
                entry(11L, "01_签约资料/${fileName}"),
                entry(12L, "01_签约资料/企业资料/${fileName}"));
        sourceLookupIds.addAll(List.of(11L, 12L));

        R<FileRecordVO> result = fileService.packageFiles(command);

        assertThat(result.isSuccess()).isTrue();
        FileRecord savedRecord = records.get(result.getData().getId());
        byte[] zipContent = storage.get(objects.get(savedRecord.getObjectId()).getObjectName());
        assertThat(unzip(zipContent)).containsEntry("01_签约资料/contract.pdf", "合同正文")
                .containsEntry("01_签约资料/企业资料/license.pdf", "营业执照");
    }

    @Test
    void packageFiles_全局压缩参数生效且Entry可覆盖为不压缩_保存新Zip并返回文件记录() throws Exception {
        FilePackageCommand command = command("compressed.zip",
                entry(11L, "资料/${fileName}"),
                entry(12L, "资料/license.pdf"));
        command.setCompression("MEDIUM");
        command.setPerFileTargetSizeBytes(10L);
        command.getEntries().get(1).setCompression("NONE");
        sourceLookupIds.addAll(List.of(11L, 12L));

        R<FileRecordVO> result = fileService.packageFiles(command);

        assertThat(result.isSuccess()).isTrue();
        FileRecord savedRecord = records.get(result.getData().getId());
        byte[] zipContent = storage.get(objects.get(savedRecord.getObjectId()).getObjectName());
        assertThat(unzip(zipContent)).containsEntry("资料/contract.pdf", "compressed:合同正文:MEDIUM:10")
                .containsEntry("资料/license.pdf", "营业执照");
    }

    @Test
    void packageFiles_压缩档位非法_拒绝生成文件记录() {
        FilePackageCommand command = command("bad-compression.zip", entry(11L, "资料/合同.pdf"));
        command.setCompression("BAD");
        long beforeRecordCount = records.size();
        sourceLookupIds.add(11L);

        assertThatThrownBy(() -> fileService.packageFiles(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(FileCode.FILE_COMPRESSION_INVALID.getCode());
        assertThat(records).hasSize((int) beforeRecordCount);
    }

    @Test
    void packageFiles_Zip非法路径_拒绝生成文件记录() {
        assertInvalidZipPath("../合同.pdf");
        assertInvalidZipPath("/合同.pdf");
        assertInvalidZipPath("C:/合同.pdf");
    }

    @Test
    void packageFiles_Zip内路径重复_拒绝生成文件记录() {
        FilePackageCommand command = command("duplicate.zip",
                entry(11L, "资料/文件.pdf"),
                entry(12L, "资料/文件.pdf"));
        long beforeRecordCount = records.size();
        sourceLookupIds.addAll(List.of(11L, 12L));

        assertThatThrownBy(() -> fileService.packageFiles(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(FileCode.FILE_NAME_DUPLICATED.getCode());
        assertThat(records).hasSize((int) beforeRecordCount);
    }

    private void assertInvalidZipPath(String path) {
        FilePackageCommand command = command("bad.zip", entry(11L, path));
        long beforeRecordCount = records.size();
        sourceLookupIds.add(11L);

        assertThatThrownBy(() -> fileService.packageFiles(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(FileCode.STORAGE_PATH_INVALID.getCode());
        assertThat(records).hasSize((int) beforeRecordCount);
    }

    private FilePackageCommand command(String fileName, FilePackageEntryCommand... entries) {
        FilePackageCommand command = new FilePackageCommand();
        command.setFileName(fileName);
        command.setPurpose("guarantee-order-material-package");
        command.setAccessLevel(FileAccessLevel.PRIVATE.name());
        command.setBizType("GUARANTEE_ORDER_MATERIAL_PACKAGE");
        command.setBizId("123456");
        command.setEntries(List.of(entries));
        return command;
    }

    private FilePackageEntryCommand entry(Long fileId, String path) {
        FilePackageEntryCommand entry = new FilePackageEntryCommand();
        entry.setFileId(fileId);
        entry.setPath(path);
        return entry;
    }

    private void sourceFile(Long recordIdValue, Long objectIdValue, String objectName, byte[] content, String contentType) {
        objectId = Math.max(objectId, objectIdValue);
        recordId = Math.max(recordId, recordIdValue);
        storage.put(objectName, content);
        FileObjectEntity object = new FileObjectEntity();
        object.setId(objectIdValue);
        object.setTenantId(TENANT_ID);
        object.setStorageConfigId(1L);
        object.setStorageType("LOCAL");
        object.setBucketName("local");
        object.setObjectName(objectName);
        object.setFileSize((long) content.length);
        object.setContentType(contentType);
        object.setStatus(FileObjectStatus.COMPLETED.value());
        objects.put(objectIdValue, object);

        FileRecord record = new FileRecord();
        record.setId(recordIdValue);
        record.setTenantId(TENANT_ID);
        record.setObjectId(objectIdValue);
        record.setStorageType("LOCAL");
        record.setStorageConfigId(1L);
        record.setBucketName("local");
        record.setObjectName(objectName);
        record.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        record.setFileExt("pdf");
        record.setFileSize((long) content.length);
        record.setContentType(contentType);
        record.setStatus(FileRecordStatus.COMPLETED.value());
        record.setArchived(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        records.put(recordIdValue, record);
    }

    private FileStorageConfig storageConfig() {
        FileStorageConfig config = new FileStorageConfig();
        config.setId(1L);
        config.setTenantId(TENANT_ID);
        config.setStorageType("LOCAL");
        config.setBucketName("local");
        config.setStoragePath("mango-file");
        config.setStatus(1);
        return config;
    }

    private FileSettingsVO settings() {
        FileSettingsVO settings = new FileSettingsVO();
        settings.setMaxSize(100L * 1024 * 1024);
        settings.setDefaultAccessLevel(FileAccessLevel.PRIVATE.name());
        settings.setDuplicateNameStrategy("ALLOW");
        settings.setDuplicateCheckDirectoryScoped(true);
        settings.setObjectNameStrategy("DATE_UUID");
        settings.setInstantUploadEnabled(false);
        settings.setContentTypeCheckEnabled(false);
        settings.setAccessMode("PROXY");
        return settings;
    }

    private String contentType(String objectName) {
        FileRecord record = records.values().stream()
                .filter(item -> objectName.equals(item.getObjectName()))
                .findFirst()
                .orElse(null);
        return record == null ? "application/zip" : record.getContentType();
    }

    private Map<String, String> unzip(byte[] content) throws Exception {
        Map<String, String> result = new HashMap<>();
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(content))) {
            java.util.zip.ZipEntry entry = zipInput.getNextEntry();
            while (entry != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                zipInput.transferTo(output);
                result.put(entry.getName(), output.toString(StandardCharsets.UTF_8));
                entry = zipInput.getNextEntry();
            }
        }
        return result;
    }

    private static final class StubFileCompressApi implements FileCompressApi {

        @Override
        public boolean supports(String fileName, String contentType) {
            return "application/pdf".equals(contentType);
        }

        @Override
        public CompressFileResultVO compress(CompressFileCommand command) {
            byte[] source = command.readAllBytes();
            String content = "compressed:" + new String(source, StandardCharsets.UTF_8)
                    + ":" + command.resolvedCompression()
                    + ":" + command.targetSizeBytes();
            byte[] compressed = content.getBytes(StandardCharsets.UTF_8);
            return new CompressFileResultVO(command.fileName(), command.contentType(), compressed,
                    source.length, compressed.length, command.targetSizeBytes(), true);
        }
    }
}
