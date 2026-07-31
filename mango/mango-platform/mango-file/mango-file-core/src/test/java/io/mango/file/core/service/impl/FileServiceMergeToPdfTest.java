package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.mango.common.exception.BizException;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FileMergePdfEntryCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileObjectStatus;
import io.mango.file.api.enums.FileRecordStatus;
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
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.AsposeWordToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.ConvertRegistry;
import io.mango.infra.fileproc.convert.convert.DefaultConvertApi;
import io.mango.infra.fileproc.convert.convert.ImageToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.TiffToPdfConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertFormatPairVO;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.render.RenderApi;
import io.mango.infra.fileproc.render.command.AddPdfWatermarkCommand;
import io.mango.infra.fileproc.render.command.CompressPdfCommand;
import io.mango.infra.fileproc.render.command.CompressPdfToTargetCommand;
import io.mango.infra.fileproc.render.command.MergePdfCommand;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.service.AsposePdfRenderApi;
import io.mango.infra.fileproc.render.vo.PdfCompressionResultVO;
import io.mango.infra.fileproc.render.vo.PdfOperationResultVO;
import io.mango.infra.fileproc.render.vo.RenderFormatPairVO;
import io.mango.infra.fileproc.render.vo.RenderResultVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServiceMergeToPdfTest {

    private static final Long TENANT_ID = 1001L;
    private static final Long USER_ID = 2001L;

    private final Map<Long, FileRecordEntity> records = new HashMap<>();
    private final Map<Long, FileObjectEntity> objects = new HashMap<>();
    private final Map<String, byte[]> storage = new HashMap<>();
    private final Deque<Long> sourceLookupIds = new ArrayDeque<>();

    private FileRecordMapper fileRecordMapper;
    private FileObjectMapper fileObjectMapper;
    private FileStorageRouter fileStorageRouter;
    private IFileStorageConfigService storageConfigService;
    private IFileSettingsService settingsService;
    private IFileDirectoryService directoryService;
    private FileHashMappingMapper fileHashMappingMapper;
    private FileUploadSessionMapper fileUploadSessionMapper;
    private FileUploadPartMapper fileUploadPartMapper;
    private FileDirectoryMapper fileDirectoryMapper;
    private FileAccessUrlAssembler accessUrlAssembler;
    private FileService fileService;
    private long recordId;
    private long objectId;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FileObjectEntity.class);
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(USER_ID, String.valueOf(TENANT_ID), "tester", "admin", "USER", "USER", USER_ID, "test"));
        fileRecordMapper = mock(FileRecordMapper.class);
        fileObjectMapper = mock(FileObjectMapper.class);
        fileStorageRouter = mock(FileStorageRouter.class);
        storageConfigService = mock(IFileStorageConfigService.class);
        settingsService = mock(IFileSettingsService.class);
        directoryService = mock(IFileDirectoryService.class);
        fileHashMappingMapper = mock(FileHashMappingMapper.class);
        fileUploadSessionMapper = mock(FileUploadSessionMapper.class);
        fileUploadPartMapper = mock(FileUploadPartMapper.class);
        fileDirectoryMapper = mock(FileDirectoryMapper.class);
        accessUrlAssembler = new FileAccessUrlAssembler(new FileProperties());
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

        fileService = newFileService(List.of(new StubConvertApi()), List.of(new StubRenderApi()));
        sourceFile(11L, 101L, "source/contract.pdf", "合同正文".getBytes(StandardCharsets.UTF_8), "application/pdf", "pdf");
        sourceFile(12L, 102L, "source/photo.jpg", "照片".getBytes(StandardCharsets.UTF_8), "image/jpeg", "jpg");
        sourceFile(13L, 103L, "source/material.docx", "Word资料".getBytes(StandardCharsets.UTF_8),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        sourceFile(14L, 104L, "source/table.xlsx", "表格".getBytes(StandardCharsets.UTF_8),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
    }

    private FileService newFileService(List<ConvertApi> convertApis, List<RenderApi> renderApis) {
        return new FileService(fileStorageRouter,
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
                List.of(),
                convertApis,
                renderApis,
                new FilePackageSizeControlProcessor(List.of()));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void mergeToPdf_多个Pdf按清单顺序合并_保存新Pdf并返回文件记录() {
        sourceFile(15L, 105L, "source/license.pdf", "营业执照".getBytes(StandardCharsets.UTF_8), "application/pdf", "pdf");
        FileMergePdfCommand command = command("材料归档", entry(11L), entry(15L));
        sourceLookupIds.addAll(List.of(11L, 15L));

        FileRecordVO result = fileService.mergeToPdf(command);

        FileRecordVO vo = result;
        assertThat(vo.getFileName()).isEqualTo("材料归档.pdf");
        assertThat(vo.getContentType()).isEqualTo("application/pdf");
        assertThat(vo.getBizType()).isEqualTo("GUARANTEE_ORDER_MATERIAL_PDF");
        assertThat(vo.getBizId()).isEqualTo("123456");
        assertThat(vo.getAccessLevel()).isEqualTo(FileAccessLevel.PRIVATE.name());
        assertThat(vo.getStatus()).isEqualTo(FileRecordStatus.COMPLETED.value());
        assertThat(vo.getPreviewUrl()).isEqualTo("/file/files/preview-content?id=" + vo.getId());
        assertThat(vo.getDownloadUrl()).isEqualTo("/file/files/download?id=" + vo.getId());
        byte[] content = savedContent(vo.getId());
        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("merged:合同正文|营业执照");
    }

    @Test
    void mergeToPdf_图片和Word先转Pdf再合并_保存新Pdf并返回文件记录() {
        FileMergePdfCommand command = command("混合材料.pdf", entry(12L), entry(13L));
        sourceLookupIds.addAll(List.of(12L, 13L));

        FileRecordVO result = fileService.mergeToPdf(command);

        byte[] content = savedContent(result.getId());
        assertThat(new String(content, StandardCharsets.UTF_8))
                .isEqualTo("merged:converted:JPEG:照片|converted:DOCX:Word资料");
    }

    @Test
    void mergeToPdf_真实PdfPngJpegDocx格式_转换合并为可打开Pdf() throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.CHINA);
            ConvertApi convertApi = realConvertApi();
            byte[] sourcePdf = samplePdf("real pdf source");
            byte[] sourcePng = sampleImage("png source", "png");
            byte[] sourceJpeg = sampleImage("jpeg source", "jpeg");
            byte[] sourceDocx = sampleDocx("real word source");
            assertThatCode(() -> assertConvertsToPdf(convertApi, ConvertFormat.PNG, "real-png.png", sourcePng))
                    .as("PNG should convert to PDF")
                    .doesNotThrowAnyException();
            assertThatCode(() -> assertConvertsToPdf(convertApi, ConvertFormat.JPEG, "real-jpeg.jpg", sourceJpeg))
                    .as("JPEG should convert to PDF")
                    .doesNotThrowAnyException();
            assertThatCode(() -> assertConvertsToPdf(convertApi, ConvertFormat.DOCX, "real-word.docx", sourceDocx))
                    .as("DOCX should convert to PDF")
                    .doesNotThrowAnyException();
            fileService = newFileService(List.of(convertApi), List.of(new AsposePdfRenderApi(this::asposeLicenseContent)));
            sourceFile(21L, 201L, "source/real-pdf.pdf", sourcePdf, "application/pdf", "pdf");
            sourceFile(22L, 202L, "source/real-png.png", sourcePng, "image/png", "png");
            sourceFile(23L, 203L, "source/real-jpeg.jpg", sourceJpeg, "image/jpeg", "jpg");
            sourceFile(24L, 204L, "source/real-word.docx", sourceDocx,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
            FileMergePdfCommand command = command("真实格式材料", entry(21L), entry(22L), entry(23L), entry(24L));
            sourceLookupIds.addAll(List.of(21L, 22L, 23L, 24L));

            FileRecordVO result = fileService.mergeToPdf(command);

            byte[] content = savedContent(result.getId());
            assertThat(content).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
            com.aspose.pdf.Document document = new com.aspose.pdf.Document(new ByteArrayInputStream(content));
            try {
                assertThat(document.getPages().size()).isGreaterThanOrEqualTo(4);
                String text = extractText(document);
                assertThat(text).contains("real pdf source");
            } finally {
                document.close();
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void mergeToPdf_目标格式不是Pdf_拒绝生成文件记录() {
        FileMergePdfCommand command = command("bad.docx", entry(11L));
        command.setTargetFormat("WORD");
        long beforeRecordCount = records.size();

        assertThatThrownBy(() -> fileService.mergeToPdf(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(FileCode.FILE_EXTENSION_NOT_ALLOWED.getCode());
        assertThat(records).hasSize((int) beforeRecordCount);
    }

    @Test
    void mergeToPdf_不支持源格式_拒绝生成文件记录() {
        FileMergePdfCommand command = command("bad.pdf", entry(14L));
        long beforeRecordCount = records.size();
        sourceLookupIds.add(14L);

        assertThatThrownBy(() -> fileService.mergeToPdf(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(FileCode.FILE_EXTENSION_NOT_ALLOWED.getCode());
        assertThat(records).hasSize((int) beforeRecordCount);
    }

    @Test
    void mergeToPdf_源文件未完成_拒绝生成文件记录() {
        FileRecordEntity record = records.get(11L);
        record.setStatus(FileRecordStatus.UPLOADING.value());
        FileMergePdfCommand command = command("bad.pdf", entry(11L));
        long beforeRecordCount = records.size();
        sourceLookupIds.add(11L);

        assertThatThrownBy(() -> fileService.mergeToPdf(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(FileCode.FILE_STATUS_INVALID.getCode());
        assertThat(records).hasSize((int) beforeRecordCount);
    }

    private FileMergePdfCommand command(String fileName, FileMergePdfEntryCommand... entries) {
        FileMergePdfCommand command = new FileMergePdfCommand();
        command.setFileName(fileName);
        command.setPurpose("guarantee-order-material-pdf");
        command.setAccessLevel(FileAccessLevel.PRIVATE.name());
        command.setBizType("GUARANTEE_ORDER_MATERIAL_PDF");
        command.setBizId("123456");
        command.setEntries(List.of(entries));
        return command;
    }

    private FileMergePdfEntryCommand entry(Long fileId) {
        FileMergePdfEntryCommand entry = new FileMergePdfEntryCommand();
        entry.setFileId(fileId);
        return entry;
    }

    private ConvertApi realConvertApi() {
        return new DefaultConvertApi(new ConvertRegistry(List.of(
                new ImageToPdfConvertProvider(),
                new AsposeWordToPdfConvertProvider(this::asposeLicenseContent),
                new TiffToPdfConvertProvider())));
    }

    private void assertConvertsToPdf(ConvertApi convertApi,
                                     ConvertFormat sourceFormat,
                                     String fileName,
                                     byte[] content) {
        ConvertResultVO result = convertApi.convert(ConvertCommand.builder()
                .sourceFormat(sourceFormat)
                .targetFormat(ConvertFormat.PDF)
                .fileName(fileName)
                .inputStream(new ByteArrayInputStream(content))
                .build());
        assertThat(result.format()).isEqualTo(ConvertFormat.PDF);
        assertThat(result.content()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] savedContent(Long fileId) {
        FileRecordEntity savedRecord = records.get(fileId);
        return storage.get(objects.get(savedRecord.getObjectId()).getObjectName());
    }

    private byte[] samplePdf(String text) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            com.aspose.pdf.Document document = new com.aspose.pdf.Document();
            try {
                com.aspose.pdf.Page page = document.getPages().add();
                page.getParagraphs().add(new com.aspose.pdf.TextFragment(text));
                document.save(outputStream);
                return outputStream.toByteArray();
            } finally {
                document.close();
            }
        }
    }

    private byte[] sampleImage(String text, String format) throws Exception {
        BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(21, 92, 130));
            graphics.fillRect(0, 0, image.getWidth(), 42);
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, 32, 96);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, format, outputStream);
            assertThat(written).isTrue();
            return outputStream.toByteArray();
        }
    }

    private byte[] sampleDocx(String text) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            com.aspose.words.Document document = new com.aspose.words.Document();
            com.aspose.words.DocumentBuilder builder = new com.aspose.words.DocumentBuilder(document);
            builder.writeln(text);
            document.save(outputStream, com.aspose.words.SaveFormat.DOCX);
            return outputStream.toByteArray();
        }
    }

    private String extractText(com.aspose.pdf.Document document) {
        com.aspose.pdf.TextFragmentAbsorber absorber = new com.aspose.pdf.TextFragmentAbsorber();
        document.getPages().accept(absorber);
        return absorber.getText();
    }

    private byte[] asposeLicenseContent(AsposeProduct product) {
        return new byte[0];
    }

    private void sourceFile(Long recordIdValue,
                            Long objectIdValue,
                            String objectName,
                            byte[] content,
                            String contentType,
                            String fileExt) {
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
        record.setFileExt(fileExt);
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
        return record == null ? "application/pdf" : record.getContentType();
    }

    private static final class StubConvertApi implements ConvertApi {

        @Override
        public boolean canConvert(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
            return targetFormat == ConvertFormat.PDF
                    && (sourceFormat == ConvertFormat.JPEG
                    || sourceFormat == ConvertFormat.PNG
                    || sourceFormat == ConvertFormat.TIFF
                    || sourceFormat == ConvertFormat.DOC
                    || sourceFormat == ConvertFormat.DOCX);
        }

        @Override
        public ConvertResultVO convert(ConvertCommand command) {
            byte[] source = readAll(command);
            String content = "converted:" + command.sourceFormat() + ":" + new String(source, StandardCharsets.UTF_8);
            return ConvertResultVO.builder()
                    .format(ConvertFormat.PDF)
                    .fileName(command.fileName())
                    .contentType(ConvertFormat.PDF.contentType())
                    .content(content.getBytes(StandardCharsets.UTF_8))
                    .build();
        }

        @Override
        public Set<ConvertFormatPairVO> supportedConversions() {
            return Set.of();
        }

        private byte[] readAll(ConvertCommand command) {
            try (java.io.InputStream inputStream = command.inputStream()) {
                return inputStream.readAllBytes();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static final class StubRenderApi implements RenderApi {

        @Override
        public boolean canRender(io.mango.infra.fileproc.render.enums.RenderFormat sourceFormat,
                                 io.mango.infra.fileproc.render.enums.RenderFormat targetFormat) {
            return false;
        }

        @Override
        public RenderResultVO render(RenderCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> extractVariables(RenderCommand command) {
            return List.of();
        }

        @Override
        public Set<RenderFormatPairVO> supportedRenderings() {
            return Set.of();
        }

        @Override
        public PdfOperationResultVO mergePdf(MergePdfCommand command) {
            String content = command.sources().stream()
                    .map(source -> readSource(source.inputStream()))
                    .collect(java.util.stream.Collectors.joining("|"));
            return new PdfOperationResultVO(command.fileName(),
                    ("merged:" + content).getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public PdfOperationResultVO addPdfWatermark(AddPdfWatermarkCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PdfOperationResultVO compressPdf(CompressPdfCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PdfCompressionResultVO compressPdfToTarget(CompressPdfToTargetCommand command) {
            throw new UnsupportedOperationException();
        }

        private String readSource(java.io.InputStream inputStream) {
            try (inputStream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
