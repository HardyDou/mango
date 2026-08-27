package io.mango.file.core.resource;

import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecordEntity;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileStorageConfigMapper;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorage;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.builder.ResourceDeclarationBuilder;
import io.mango.resource.support.declaration.FileAssetContentLocations;
import io.mango.resource.support.model.ResourceDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAssetResourceHandlerTest {

    private static final Long FILE_ID = 900000000000001L;
    private static final Long STORAGE_CONFIG_ID = 1L;
    private static final String OBJECT_NAME = "mango-assets/test/sample.txt";
    private static final String SHA256 = "5ca4a7a6c1faf50702ba8a8f2c164bf420ec40a8a795f435e52b35857682bbdb";

    private final FileStorageConfigMapper storageConfigMapper = mock(FileStorageConfigMapper.class);
    private final FileObjectMapper fileObjectMapper = mock(FileObjectMapper.class);
    private final FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
    private final AtomicReference<FileObjectEntity> objectState = new AtomicReference<>();
    private final AtomicReference<FileRecordEntity> recordState = new AtomicReference<>();
    private final InMemoryFileStorage storage = new InMemoryFileStorage();
    private final FileProperties fileProperties = new FileProperties();
    private FileAssetResourceHandler handler;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        FileStorageConfigEntity config = new FileStorageConfigEntity();
        config.setId(STORAGE_CONFIG_ID);
        config.setTenantId(1L);
        config.setStorageType("MEMORY");
        config.setBucketName("test-bucket");
        config.setStatus(1);
        when(storageConfigMapper.selectById(STORAGE_CONFIG_ID)).thenReturn(config);
        when(fileRecordMapper.selectById(FILE_ID)).thenAnswer(ignored -> recordState.get());
        when(fileObjectMapper.selectById(any())).thenAnswer(ignored -> objectState.get());
        when(fileObjectMapper.selectOne(any())).thenAnswer(ignored -> objectState.get());
        doAnswer(invocation -> {
            FileObjectEntity entity = invocation.getArgument(0);
            entity.setId(7001L);
            objectState.set(entity);
            return 1;
        }).when(fileObjectMapper).insert(any(FileObjectEntity.class));
        doAnswer(invocation -> {
            objectState.set(invocation.getArgument(0));
            return 1;
        }).when(fileObjectMapper).updateById(any(FileObjectEntity.class));
        doAnswer(invocation -> {
            recordState.set(invocation.getArgument(0));
            return 1;
        }).when(fileRecordMapper).insert(any(FileRecordEntity.class));
        doAnswer(invocation -> {
            recordState.set(invocation.getArgument(0));
            return 1;
        }).when(fileRecordMapper).updateById(any(FileRecordEntity.class));
        handler = new FileAssetResourceHandler(storageConfigMapper, fileObjectMapper, fileRecordMapper,
                new FileStorageRouter(java.util.List.of(storage)), new DefaultResourceLoader(), fileProperties);
    }

    @Test
    void publishesOnceAndKeepsStableFileIdentity() {
        ResourceDeclaration declaration = declaration(OBJECT_NAME, SHA256);

        handler.upsert(declaration);
        handler.upsert(declaration);

        assertThat(storage.putCount).isOne();
        assertThat(storage.objects).containsKey(OBJECT_NAME);
        assertThat(storage.objects.keySet()).noneMatch(key -> key.startsWith(".mango-staging/"));
        assertThat(recordState.get()).satisfies(record -> {
            assertThat(record.getId()).isEqualTo(FILE_ID);
            assertThat(record.getObjectName()).isEqualTo(OBJECT_NAME);
            assertThat(record.getFileHash()).isEqualTo(SHA256);
            assertThat(record.getObjectId()).isEqualTo(7001L);
        });
    }

    @Test
    void publishFailureRemovesStagingObjectBeforePropagatingFailure() {
        storage.failNextPublish();

        assertThatThrownBy(() -> handler.upsert(declaration(OBJECT_NAME, SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Publish FILE_ASSET failed");

        assertThat(storage.objects).doesNotContainKey(OBJECT_NAME);
        assertThat(storage.objects.keySet()).noneMatch(key -> key.startsWith(".mango-staging/"));
        assertThat(recordState.get()).isNull();
        assertThat(objectState.get()).isNull();
    }

    @Test
    void rejectsStableObjectLocationDrift() {
        handler.upsert(declaration(OBJECT_NAME, SHA256));

        assertThatThrownBy(() -> handler.upsert(declaration("mango-assets/test/other.txt", SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stable identity drift");
        assertThat(storage.putCount).isOne();
    }

    @Test
    void rejectsArtifactChecksumMismatchBeforeUpload() {
        assertThatThrownBy(() -> handler.upsert(declaration(OBJECT_NAME, "0".repeat(64))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sha256 mismatch");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void republishesWhenStoredObjectChecksumDrifts() {
        ResourceDeclaration declaration = declaration(OBJECT_NAME, SHA256);
        handler.upsert(declaration);
        storage.objects.put(OBJECT_NAME, "corrupted".getBytes(StandardCharsets.UTF_8));

        handler.upsert(declaration);

        assertThat(storage.putCount).isEqualTo(2);
        assertThat(storage.objects.get(OBJECT_NAME))
                .isEqualTo("mango-file-asset\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void logicalDeleteArchivesRecordAndRetainsObject() {
        ResourceDeclaration declaration = declaration(OBJECT_NAME, SHA256);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(recordState.get().getArchived()).isOne();
        assertThat(storage.objects).containsKey(OBJECT_NAME);
    }

    @Test
    void publishesExternalAssetFromConfiguredRoot() throws Exception {
        Path asset = temporaryDirectory.resolve("documents/sample.txt");
        Files.createDirectories(asset.getParent());
        Files.writeString(asset, "mango-file-asset\n", StandardCharsets.UTF_8);
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        handler.upsert(externalDeclaration("asset:documents/sample.txt", SHA256));

        assertThat(storage.objects.get(OBJECT_NAME))
                .isEqualTo("mango-file-asset\n".getBytes(StandardCharsets.UTF_8));
        assertThat(recordState.get().getFileHash()).isEqualTo(SHA256);
    }

    @Test
    void keepsClasspathAssetCompatibility() {
        handler.upsert(declaration(OBJECT_NAME, SHA256));

        assertThat(storage.objects).containsKey(OBJECT_NAME);
        assertThat(storage.putCount).isOne();
    }

    @Test
    void publishesContentAddressedObjectFromPackagedClasspath() {
        handler.upsert(declaration(OBJECT_NAME, SHA256,
                FileAssetContentLocations.packagedObject(SHA256)));

        assertThat(storage.objects.get(OBJECT_NAME))
                .isEqualTo("mango-file-asset\n".getBytes(StandardCharsets.UTF_8));
        assertThat(recordState.get().getFileHash()).isEqualTo(SHA256);
    }

    @Test
    void rejectsPackagedObjectWhoseLocationDoesNotMatchDeclaredChecksum() {
        String mismatchedLocation = FileAssetContentLocations.packagedObject("0".repeat(64));

        assertThatThrownBy(() -> handler.upsert(declaration(OBJECT_NAME, SHA256, mismatchedLocation)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must match declared sha256");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsExternalAssetWhenRootIsNotConfigured() {
        assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:documents/sample.txt", SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asset-root is not configured");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsAbsoluteExternalAssetPath() {
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:/etc/passwd", SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("safe relative path");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsWindowsAbsoluteExternalAssetPath() {
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:C:/bootstrap-assets/sample.txt",
                SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("safe relative path");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsParentTraversalExternalAssetPath() {
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:documents/../sample.txt", SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("safe relative path");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsMissingExternalAsset() {
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:documents/missing.txt", SHA256)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external content is not readable");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsExternalAssetChecksumMismatchBeforeUpload() throws Exception {
        Path asset = temporaryDirectory.resolve("documents/sample.txt");
        Files.createDirectories(asset.getParent());
        Files.writeString(asset, "mango-file-asset\n", StandardCharsets.UTF_8);
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:documents/sample.txt",
                "0".repeat(64))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sha256 mismatch");
        assertThat(storage.putCount).isZero();
    }

    @Test
    void rejectsSymbolicLinkEscapingExternalAssetRoot() throws Exception {
        Path outside = Files.createTempFile("mango-file-asset-outside", ".txt");
        Path link = temporaryDirectory.resolve("outside.txt");
        Files.createSymbolicLink(link, outside);
        fileProperties.setAssetRoot(temporaryDirectory.toString());

        try {
            assertThatThrownBy(() -> handler.upsert(externalDeclaration("asset:outside.txt", SHA256)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("escapes asset-root");
            assertThat(storage.putCount).isZero();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    private ResourceDeclaration declaration(String objectName, String sha256) {
        return declaration(objectName, sha256, "classpath:META-INF/mango/assets/test/sample.txt");
    }

    private ResourceDeclaration externalDeclaration(String location, String sha256) {
        return declaration(OBJECT_NAME, sha256, location);
    }

    private ResourceDeclaration declaration(String objectName, String sha256, String location) {
        return ResourceDeclarationBuilder.create(ResourceTypes.FILE_ASSET)
                .id("service-a.sample-file")
                .version(1)
                .bizKey("service-a.sample-file")
                .longValue("tenantId", 1L)
                .longValue("fileId", FILE_ID)
                .longValue("storageConfigId", STORAGE_CONFIG_ID)
                .string("objectName", objectName)
                .string("fileName", "sample.txt")
                .string("sha256", sha256)
                .file("content", location, null, "text/plain")
                .build();
    }

    private static final class InMemoryFileStorage implements FileStorage {

        private final Map<String, byte[]> objects = new HashMap<>();
        private int putCount;
        private boolean failPublish;

        private void failNextPublish() {
            failPublish = true;
        }

        @Override
        public boolean supports(String storageType) {
            return "MEMORY".equals(storageType);
        }

        @Override
        public void putObject(FileStorageConfigEntity config, String objectName, InputStream inputStream,
                              long contentLength, String contentType) throws Exception {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            inputStream.transferTo(output);
            objects.put(objectName, output.toByteArray());
            putCount++;
        }

        @Override
        public FileObject getObject(FileStorageConfigEntity config, String objectName) {
            byte[] content = objects.get(objectName);
            if (content == null) {
                throw new IllegalStateException("Object not found");
            }
            return new FileObject(new ByteArrayInputStream(content), content.length, "text/plain");
        }

        @Override
        public void removeObject(FileStorageConfigEntity config, String objectName) {
            objects.remove(objectName);
        }

        @Override
        public void publishObject(FileStorageConfigEntity config, String stagingObjectName,
                                  String targetObjectName) {
            if (failPublish) {
                failPublish = false;
                throw new IllegalStateException("Injected publish failure");
            }
            byte[] content = objects.remove(stagingObjectName);
            if (content == null) {
                throw new IllegalStateException("Staging object not found");
            }
            objects.put(targetObjectName, content);
        }

        @Override
        public void test(FileStorageConfigEntity config) {
        }
    }
}
