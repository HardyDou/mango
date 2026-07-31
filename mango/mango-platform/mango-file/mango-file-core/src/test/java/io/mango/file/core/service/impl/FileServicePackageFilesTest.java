package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.mango.common.exception.BizException;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.FilePackageEntryCommand;
import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileObjectStatus;
import io.mango.file.api.enums.FilePackageSizeControlMode;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FilePackageResultVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecordEntity;
import io.mango.file.core.entity.FileStorageConfigEntity;
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
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    private final Map<Long, FileRecordEntity> records = new HashMap<>();
    private final Map<Long, FileObjectEntity> objects = new HashMap<>();
    private final Map<String, byte[]> storage = new HashMap<>();
    private final Deque<Long> sourceLookupIds = new ArrayDeque<>();

    private FileRecordMapper fileRecordMapper;
    private FileObjectMapper fileObjectMapper;
    private FileService fileService;
    private RecordingTargetCompressApi targetCompressApi;
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
        FileStorageConfigEntity storageConfig = storageConfig();

        when(storageConfigService.activeConfig()).thenReturn(storageConfig);
        when(storageConfigService.getEnabledConfig(any())).thenReturn(storageConfig);
        when(settingsService.current()).thenReturn(settings());
        when(fileRecordMapper.selectOne(any())).thenAnswer(invocation -> records.get(sourceLookupIds.removeFirst()));
        when(fileRecordMapper.selectCount(any())).thenReturn(0L);
        when(fileRecordMapper.insert(any(FileRecordEntity.class))).thenAnswer(invocation -> {
            FileRecordEntity record = invocation.getArgument(0);
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

        targetCompressApi = new RecordingTargetCompressApi();
        fileService = new FileService(fileStorageRouter,
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
                List.of(new StubFileCompressApi()),
                List.of(),
                List.of(),
                new FilePackageSizeControlProcessor(List.of(targetCompressApi)));
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

        FileRecordVO result = fileService.packageFiles(command);

        FileRecordVO vo = result;
        assertThat(vo.getId()).isGreaterThan(12L);
        assertThat(vo.getFileName()).isEqualTo("签约资料包-GO20260628000011.zip");
        assertThat(vo.getContentType()).isEqualTo("application/zip");
        assertThat(vo.getBizType()).isEqualTo("GUARANTEE_ORDER_MATERIAL_PACKAGE");
        assertThat(vo.getBizId()).isEqualTo("123456");
        assertThat(vo.getAccessLevel()).isEqualTo(FileAccessLevel.PRIVATE.name());
        assertThat(vo.getStatus()).isEqualTo(FileRecordStatus.COMPLETED.value());
        assertThat(vo.getFileSize()).isPositive();
        FileRecordEntity savedRecord = records.get(vo.getId());
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

        FileRecordVO result = fileService.packageFiles(command);

        FileRecordEntity savedRecord = records.get(result.getId());
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

        FileRecordVO result = fileService.packageFiles(command);

        FileRecordEntity savedRecord = records.get(result.getId());
        byte[] zipContent = storage.get(objects.get(savedRecord.getObjectId()).getObjectName());
        assertThat(unzip(zipContent)).containsEntry("资料/contract.pdf", "compressed:合同正文:MEDIUM:10")
                .containsEntry("资料/license.pdf", "营业执照");
    }

    @Test
    void packageFilesWithSizeControl_AUTO_按文件大小比例分摊并生成单个达标Zip() throws Exception {
        byte[] largePdf = randomBytes(2400, 11);
        byte[] smallPdf = randomBytes(1200, 12);
        sourceFile(11L, 101L, "source/large.pdf", largePdf, "application/pdf");
        sourceFile(12L, 102L, "source/small.pdf", smallPdf, "application/pdf");
        FilePackageSizeControlCommand command = sizeControlCommand(
                FilePackageSizeControlMode.AUTO,
                1700L,
                entry(11L, "资料/large.pdf"),
                entry(12L, "资料/small.pdf"));
        command.setCompression("MEDIUM");
        sourceLookupIds.addAll(List.of(11L, 12L));

        FilePackageResultVO result = fileService.packageFilesWithSizeControl(command);

        assertThat(result.getFile()).isNotNull();
        assertThat(result.getFile().getFileName()).isEqualTo("size-control.zip");
        assertThat(result.getPackageTargetAchieved()).isTrue();
        assertThat(result.getActualPackageSizeBytes()).isLessThanOrEqualTo(1700L);
        assertThat(result.getEntries()).hasSize(2);
        assertThat(result.getEntries()).allMatch(item -> Boolean.TRUE.equals(item.getCompressionApplied()));
        assertThat(targetCompressApi.requests()).hasSizeGreaterThanOrEqualTo(2);
        TargetRequest largeRequest = targetCompressApi.requests().get(0);
        TargetRequest smallRequest = targetCompressApi.requests().get(1);
        long largeSaving = largeRequest.sourceSize() - largeRequest.targetSize();
        long smallSaving = smallRequest.sourceSize() - smallRequest.targetSize();
        assertThat(largeSaving).isGreaterThan(smallSaving);
        assertThat(Math.abs(largeSaving - 2L * smallSaving)).isLessThanOrEqualTo(1L);
        assertThat(savedZip(result.getFile().getId())).hasSize(2);
    }

    @Test
    void packageFilesWithSizeControl_AUTO_候选触底后将缺口重新分配给其余文件() {
        byte[] limitedPdf = randomBytes(2400, 31);
        byte[] flexiblePdf = randomBytes(1200, 32);
        sourceFile(11L, 101L, "source/limited.pdf", limitedPdf, "application/pdf");
        sourceFile(12L, 102L, "source/flexible.pdf", flexiblePdf, "application/pdf");
        targetCompressApi.setMinimumSize("limited.pdf", 1800);
        FilePackageSizeControlCommand command = sizeControlCommand(
                FilePackageSizeControlMode.AUTO,
                2500L,
                entry(11L, "资料/limited.pdf"),
                entry(12L, "资料/flexible.pdf"));
        command.setCompression("MEDIUM");
        sourceLookupIds.addAll(List.of(11L, 12L));

        FilePackageResultVO result = fileService.packageFilesWithSizeControl(command);

        List<TargetRequest> limitedRequests = targetCompressApi.requests().stream()
                .filter(request -> request.fileName().equals("limited.pdf"))
                .toList();
        List<TargetRequest> flexibleRequests = targetCompressApi.requests().stream()
                .filter(request -> request.fileName().equals("flexible.pdf"))
                .toList();
        assertThat(limitedRequests).hasSize(1);
        assertThat(flexibleRequests).hasSizeGreaterThanOrEqualTo(2);
        assertThat(flexibleRequests.get(1).targetSize()).isLessThan(flexibleRequests.get(0).targetSize());
        assertThat(result.getEntries().get(0).getOutputSizeBytes()).isEqualTo(1800L);
        assertThat(result.getEntries().get(1).getOutputSizeBytes()).isLessThan(1200L);
    }

    @Test
    void packageFilesWithSizeControl_AUTO_目标不可达时正常返回实际大小() {
        byte[] originalPdf = randomBytes(600, 41);
        byte[] excel = randomBytes(500, 42);
        sourceFile(11L, 101L, "source/original.pdf", originalPdf, "application/pdf");
        sourceFile(13L, 103L, "source/table.xlsx", excel,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        FilePackageEntryCommand noneEntry = entry(11L, "资料/original.pdf");
        noneEntry.setCompression("NONE");
        FilePackageSizeControlCommand command = sizeControlCommand(
                FilePackageSizeControlMode.AUTO,
                100L,
                noneEntry,
                entry(13L, "资料/table.xlsx"));
        command.setCompression("HIGH");
        sourceLookupIds.addAll(List.of(11L, 13L));

        FilePackageResultVO result = fileService.packageFilesWithSizeControl(command);

        assertThat(result.getPackageTargetAchieved()).isFalse();
        assertThat(result.getActualPackageSizeBytes()).isGreaterThan(100L);
        assertThat(result.getMessage())
                .contains("目标100字节")
                .contains("当前只能压缩到" + result.getActualPackageSizeBytes() + "字节");
        assertThat(targetCompressApi.requests()).isEmpty();
        assertThat(result.getEntries()).extracting("outputSizeBytes")
                .containsExactly(600L, 500L);
    }

    @Test
    void packageFilesWithSizeControl_MANUAL_只按Entry目标压缩且None和Excel保持原样() throws Exception {
        byte[] compressedPdf = randomBytes(1000, 21);
        byte[] nonePdf = randomBytes(600, 22);
        byte[] excel = randomBytes(500, 23);
        sourceFile(11L, 101L, "source/compressed.pdf", compressedPdf, "application/pdf");
        sourceFile(12L, 102L, "source/original.pdf", nonePdf, "application/pdf");
        sourceFile(13L, 103L, "source/table.xlsx", excel,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        FilePackageEntryCommand compressedEntry = entry(11L, "资料/compressed.pdf");
        compressedEntry.setTargetSizeBytes(400L);
        FilePackageEntryCommand noneEntry = entry(12L, "资料/original.pdf");
        noneEntry.setCompression("NONE");
        noneEntry.setTargetSizeBytes(100L);
        FilePackageEntryCommand excelEntry = entry(13L, "资料/table.xlsx");
        excelEntry.setTargetSizeBytes(100L);
        FilePackageSizeControlCommand command = sizeControlCommand(
                FilePackageSizeControlMode.MANUAL,
                300L,
                compressedEntry,
                noneEntry,
                excelEntry);
        command.setCompression("MEDIUM");
        sourceLookupIds.addAll(List.of(11L, 12L, 13L));

        FilePackageResultVO result = fileService.packageFilesWithSizeControl(command);

        assertThat(result.getFile()).isNotNull();
        assertThat(result.getPackageTargetAchieved()).isFalse();
        assertThat(result.getEntryTargetsAchieved()).isFalse();
        assertThat(result.getCompressionApplied()).isTrue();
        assertThat(result.getEntries()).extracting("outputSizeBytes")
                .containsExactly(400L, 600L, 500L);
        assertThat(result.getEntries()).extracting("targetAchieved")
                .containsExactly(true, false, false);
        assertThat(result.getEntries().get(1).getMessage()).contains("NONE");
        assertThat(result.getEntries().get(2).getMessage()).contains("不支持压缩");
        assertThat(targetCompressApi.requests()).hasSize(1);
        Map<String, byte[]> zipEntries = savedZip(result.getFile().getId());
        assertThat(zipEntries.get("资料/compressed.pdf")).hasSize(400);
        assertThat(zipEntries.get("资料/original.pdf")).isEqualTo(nonePdf);
        assertThat(zipEntries.get("资料/table.xlsx")).isEqualTo(excel);
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

    private FilePackageSizeControlCommand sizeControlCommand(FilePackageSizeControlMode mode,
                                                             Long maxPackageSizeBytes,
                                                             FilePackageEntryCommand... entries) {
        FilePackageSizeControlCommand command = new FilePackageSizeControlCommand();
        command.setFileName("size-control.zip");
        command.setPurpose("guarantee-order-material-package");
        command.setAccessLevel(FileAccessLevel.PRIVATE.name());
        command.setBizType("GUARANTEE_ORDER_MATERIAL_PACKAGE");
        command.setBizId("123456");
        command.setSizeControlMode(mode);
        command.setMaxPackageSizeBytes(maxPackageSizeBytes);
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

        FileRecordEntity record = new FileRecordEntity();
        record.setId(recordIdValue);
        record.setTenantId(TENANT_ID);
        record.setObjectId(objectIdValue);
        record.setStorageType("LOCAL");
        record.setStorageConfigId(1L);
        record.setBucketName("local");
        record.setObjectName(objectName);
        record.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        int extensionIndex = objectName.lastIndexOf('.');
        record.setFileExt(extensionIndex < 0 ? "" : objectName.substring(extensionIndex + 1));
        record.setFileSize((long) content.length);
        record.setContentType(contentType);
        record.setStatus(FileRecordStatus.COMPLETED.value());
        record.setArchived(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        records.put(recordIdValue, record);
    }

    private FileStorageConfigEntity storageConfig() {
        FileStorageConfigEntity config = new FileStorageConfigEntity();
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
        FileRecordEntity record = records.values().stream()
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

    private Map<String, byte[]> savedZip(Long fileId) throws Exception {
        FileRecordEntity savedRecord = records.get(fileId);
        byte[] content = storage.get(objects.get(savedRecord.getObjectId()).getObjectName());
        Map<String, byte[]> result = new HashMap<>();
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(content))) {
            java.util.zip.ZipEntry entry = zipInput.getNextEntry();
            while (entry != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                zipInput.transferTo(output);
                result.put(entry.getName(), output.toByteArray());
                entry = zipInput.getNextEntry();
            }
        }
        return result;
    }

    private byte[] randomBytes(int size, long seed) {
        byte[] content = new byte[size];
        new Random(seed).nextBytes(content);
        return content;
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

    private static final class RecordingTargetCompressApi implements FileCompressApi {

        private final List<TargetRequest> requests = new java.util.ArrayList<>();
        private final Map<String, Integer> minimumSizes = new HashMap<>();

        @Override
        public boolean supports(String fileName, String contentType) {
            return "application/pdf".equals(contentType) || (contentType != null && contentType.startsWith("image/"));
        }

        @Override
        public CompressFileResultVO compress(CompressFileCommand command) {
            byte[] source = command.readAllBytes();
            long requestedTarget = command.targetSizeBytes() == null ? source.length : command.targetSizeBytes();
            long minimumSize = minimumSizes.getOrDefault(command.fileName(), 1);
            int outputSize = (int) Math.max(minimumSize, Math.min(source.length, requestedTarget));
            requests.add(new TargetRequest(command.fileName(), source.length, requestedTarget));
            byte[] compressed = Arrays.copyOf(source, outputSize);
            return new CompressFileResultVO(command.fileName(), command.contentType(), compressed,
                    source.length, compressed.length, command.targetSizeBytes(), compressed.length <= requestedTarget);
        }

        private void setMinimumSize(String fileName, int minimumSize) {
            minimumSizes.put(fileName, minimumSize);
        }

        private List<TargetRequest> requests() {
            return List.copyOf(requests);
        }
    }

    private record TargetRequest(String fileName, long sourceSize, long targetSize) {
    }
}
