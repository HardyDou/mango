package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.config.FileProperties;
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
import io.mango.file.core.storage.FileStorage;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        FileServiceConcurrentSaveIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:file_concurrent;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class FileServiceConcurrentSaveIntegrationTest {

    private static final int CONCURRENCY = 5;
    private static final long TENANT_ID = 1001L;
    private static final long USER_ID = 2001L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileServiceImpl fileService;

    @Autowired
    private ConcurrentFileStorage fileStorage;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        rebuildTables();
        fileStorage.reset(CONCURRENCY);
        executor = Executors.newFixedThreadPool(CONCURRENCY);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        MangoContextHolder.clear();
    }

    @Test
    void saveGenerated_五线程并发保存相同内容_复用一个物理对象并全部成功() throws Exception {
        byte[] content = "IT_453_same_content".getBytes(StandardCharsets.UTF_8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<R<FileRecordVO>>> futures = new ArrayList<>();

        for (int index = 0; index < CONCURRENCY; index++) {
            int fileIndex = index;
            futures.add(executor.submit(() -> saveGenerated(start, content, fileIndex)));
        }
        start.countDown();

        List<R<FileRecordVO>> results = new ArrayList<>();
        for (Future<R<FileRecordVO>> future : futures) {
            results.add(future.get(15, TimeUnit.SECONDS));
        }

        assertThat(results).allSatisfy(result -> {
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
        });
        assertThat(results).extracting(result -> result.getData().getId()).doesNotHaveDuplicates();
        assertThat(count("file_object")).isOne();
        assertThat(count("file_hash_mapping")).isOne();
        assertThat(count("file_record")).isEqualTo(CONCURRENCY);
        assertThat(countDistinct("file_record", "object_id")).isOne();
        assertThat(longValue("file_object", "ref_count")).isEqualTo(CONCURRENCY);
        assertThat(longValue("file_hash_mapping", "object_id"))
                .isEqualTo(longValue("file_object", "id"));
        assertThat(fileStorage.objectCount()).isOne();
    }

    private R<FileRecordVO> saveGenerated(CountDownLatch start, byte[] content, int fileIndex) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(USER_ID, String.valueOf(TENANT_ID), "IT_453", "admin",
                        "USER", "USER", USER_ID, "IT_453"));
        try {
            return fileService.saveGenerated(
                    content,
                    "IT_453_" + fileIndex + ".zip",
                    "application/zip",
                    "IT_453",
                    "IT_453",
                    String.valueOf(fileIndex));
        } finally {
            MangoContextHolder.clear();
        }
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists file_record");
        jdbcTemplate.execute("drop table if exists file_hash_mapping");
        jdbcTemplate.execute("drop table if exists file_object");
        jdbcTemplate.execute("""
                create table file_object (
                    id bigint not null,
                    tenant_id bigint not null,
                    storage_config_id bigint,
                    storage_type varchar(32) not null,
                    bucket_name varchar(128) not null,
                    object_name varchar(500) not null,
                    file_hash varchar(128) not null,
                    file_size bigint not null,
                    content_type varchar(128),
                    status tinyint not null,
                    ref_count bigint not null,
                    created_by bigint,
                    created_time timestamp not null,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null,
                    updated_at timestamp default current_timestamp,
                    primary key (id),
                    unique key uk_file_object_hash_storage
                        (storage_config_id, bucket_name, file_hash, file_size)
                )
                """);
        jdbcTemplate.execute("""
                create table file_hash_mapping (
                    id bigint not null,
                    scope_type varchar(32) not null,
                    tenant_id bigint not null,
                    storage_config_id bigint,
                    file_hash varchar(128) not null,
                    file_size bigint not null,
                    object_id bigint not null,
                    status tinyint not null,
                    created_by bigint,
                    created_time timestamp not null,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null,
                    updated_at timestamp default current_timestamp,
                    primary key (id),
                    unique key uk_file_hash_mapping_target
                        (scope_type, tenant_id, storage_config_id, file_hash, file_size)
                )
                """);
        jdbcTemplate.execute("""
                create table file_record (
                    id bigint not null,
                    tenant_id bigint not null,
                    biz_type varchar(64),
                    biz_id varchar(128),
                    purpose varchar(64),
                    biz_meta varchar(4000),
                    directory_id bigint not null,
                    access_level varchar(32) not null,
                    object_id bigint not null,
                    storage_type varchar(32) not null,
                    storage_config_id bigint,
                    bucket_name varchar(128) not null,
                    object_name varchar(500) not null,
                    file_name varchar(255) not null,
                    file_ext varchar(32),
                    file_size bigint not null,
                    content_type varchar(128),
                    file_hash varchar(128) not null,
                    status tinyint not null,
                    archived tinyint not null,
                    created_by bigint,
                    created_time timestamp not null,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null,
                    updated_at timestamp default current_timestamp,
                    primary key (id)
                )
                """);
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private long countDistinct(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                "select count(distinct " + columnName + ") from " + tableName,
                Long.class);
    }

    private long longValue(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                "select " + columnName + " from " + tableName,
                Long.class);
    }

    private static FileStorageConfig storageConfig() {
        FileStorageConfig config = new FileStorageConfig();
        config.setId(1L);
        config.setTenantId(TENANT_ID);
        config.setStorageType("LOCAL");
        config.setBucketName("IT_453_BUCKET");
        config.setStoragePath("IT_453");
        config.setStatus(1);
        return config;
    }

    private static FileSettingsVO settings() {
        FileSettingsVO settings = new FileSettingsVO();
        settings.setMaxSize(1024L * 1024L);
        settings.setDefaultAccessLevel(FileAccessLevel.PRIVATE.name());
        settings.setDuplicateNameStrategy("ALLOW");
        settings.setDuplicateCheckDirectoryScoped(true);
        settings.setObjectNameStrategy("DATE_UUID");
        settings.setInstantUploadEnabled(true);
        settings.setInstantUploadScope("TENANT");
        settings.setContentTypeCheckEnabled(false);
        settings.setAccessMode("PROXY");
        return settings;
    }

    @Configuration
    @MapperScan(basePackageClasses = FileObjectMapper.class)
    static class TestConfig {

        @Bean
        ConcurrentFileStorage concurrentFileStorage() {
            return new ConcurrentFileStorage();
        }

        @Bean
        FileStorageRouter fileStorageRouter(ConcurrentFileStorage storage) {
            return new FileStorageRouter(List.of(storage));
        }

        @Bean
        FileServiceImpl fileService(FileStorageRouter fileStorageRouter,
                                    FileRecordMapper fileRecordMapper,
                                    FileObjectMapper fileObjectMapper,
                                    FileHashMappingMapper fileHashMappingMapper,
                                    FileUploadSessionMapper fileUploadSessionMapper,
                                    FileUploadPartMapper fileUploadPartMapper,
                                    FileDirectoryMapper fileDirectoryMapper) {
            FileStorageConfig config = storageConfig();
            IFileStorageConfigService storageConfigService = mock(IFileStorageConfigService.class);
            IFileSettingsService settingsService = mock(IFileSettingsService.class);
            IFileDirectoryService directoryService = mock(IFileDirectoryService.class);
            when(storageConfigService.activeConfig()).thenReturn(config);
            when(storageConfigService.getEnabledConfig(1L, "LOCAL", "IT_453_BUCKET")).thenReturn(config);
            when(settingsService.current()).thenReturn(settings());
            return new FileServiceImpl(
                    fileStorageRouter,
                    storageConfigService,
                    settingsService,
                    directoryService,
                    fileRecordMapper,
                    fileObjectMapper,
                    fileHashMappingMapper,
                    fileUploadSessionMapper,
                    fileUploadPartMapper,
                    fileDirectoryMapper,
                    new ObjectMapper(),
                    new FileAccessUrlAssembler(new FileProperties()),
                    List.of(),
                    List.of(),
                    List.of());
        }
    }

    static final class ConcurrentFileStorage implements FileStorage {

        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
        private volatile CyclicBarrier uploadBarrier = new CyclicBarrier(CONCURRENCY);

        void reset(int parties) {
            objects.clear();
            uploadBarrier = new CyclicBarrier(parties);
        }

        int objectCount() {
            return objects.size();
        }

        @Override
        public boolean supports(String storageType) {
            return "LOCAL".equals(storageType);
        }

        @Override
        public void putObject(FileStorageConfig config,
                              String objectName,
                              InputStream inputStream,
                              long contentLength,
                              String contentType) throws Exception {
            objects.put(objectName, inputStream.readAllBytes());
            uploadBarrier.await(10, TimeUnit.SECONDS);
        }

        @Override
        public FileObject getObject(FileStorageConfig config, String objectName) {
            byte[] content = objects.get(objectName);
            return new FileObject(new ByteArrayInputStream(content), content.length, "application/zip");
        }

        @Override
        public void removeObject(FileStorageConfig config, String objectName) {
            objects.remove(objectName);
        }

        @Override
        public void test(FileStorageConfig config) {
            Objects.requireNonNull(config, "config");
        }
    }
}
