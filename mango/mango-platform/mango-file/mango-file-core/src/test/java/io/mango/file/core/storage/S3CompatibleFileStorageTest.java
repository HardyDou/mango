package io.mango.file.core.storage;

import io.mango.file.core.entity.FileStorageConfigEntity;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class S3CompatibleFileStorageTest {

    @Test
    void publicGetUrl_withPublicEndpointAndPathStyle_usesConfiguredStorageEndpoint() {
        S3CompatibleFileStorage storage = new S3CompatibleFileStorage();
        FileStorageConfigEntity config = minioConfig();
        config.setPublicEndpoint("http://file.mango.io:9000");
        config.setPathStyleAccess(1);

        String url = storage.publicGetUrl(config, "tenant-1/2026/06/test file.txt", "test file.txt").orElseThrow();

        assertThat(url).isEqualTo("http://file.mango.io:9000/mango-file/tenant-1/2026/06/test%20file.txt");
    }

    @Test
    void publicGetUrl_withoutPublicEndpoint_usesEndpointAndSslConfig() {
        S3CompatibleFileStorage storage = new S3CompatibleFileStorage();
        FileStorageConfigEntity config = minioConfig();
        config.setEndpoint("minio.example.com:9000");
        config.setPathStyleAccess(1);
        config.setSslEnabled(1);

        String url = storage.publicGetUrl(config, "tenant-1/2026/06/test.txt", "test.txt").orElseThrow();

        assertThat(url).isEqualTo("https://minio.example.com:9000/mango-file/tenant-1/2026/06/test.txt");
    }

    @Test
    void publicGetUrl_withoutPathStyle_keepsObjectNameUnderConfiguredEndpoint() {
        S3CompatibleFileStorage storage = new S3CompatibleFileStorage();
        FileStorageConfigEntity config = minioConfig();
        config.setPublicEndpoint("https://cdn.example.com");
        config.setPathStyleAccess(0);

        String url = storage.publicGetUrl(config, "tenant-1/2026/06/test.txt", "test.txt").orElseThrow();

        assertThat(url).isEqualTo("https://cdn.example.com/tenant-1/2026/06/test.txt");
    }

    @Test
    void presignedGetUrl_withPublicEndpoint_signsAndReturnsConfiguredHostAndPort() {
        S3CompatibleFileStorage storage = new S3CompatibleFileStorage();
        FileStorageConfigEntity config = minioConfig();
        config.setPublicEndpoint("http://file.mango.io:9000");
        config.setPathStyleAccess(1);

        URI url = URI.create(storage.presignedGetUrl(
                config,
                "tenant-1/2026/06/test.txt",
                "test.txt",
                Duration.ofMinutes(10)
        ).orElseThrow());

        assertThat(url.getScheme()).isEqualTo("http");
        assertThat(url.getHost()).isEqualTo("file.mango.io");
        assertThat(url.getPort()).isEqualTo(9000);
        assertThat(url.getPath()).isEqualTo("/mango-file/tenant-1/2026/06/test.txt");
        assertThat(url.getRawQuery())
                .contains("X-Amz-Signature=")
                .contains("X-Amz-Expires=600");
    }

    private FileStorageConfigEntity minioConfig() {
        FileStorageConfigEntity config = new FileStorageConfigEntity();
        config.setStorageType("MINIO");
        config.setEndpoint("http://127.0.0.1:9000");
        config.setBucketName("mango-file");
        config.setAccessKey("minioadmin");
        config.setSecretKey("minioadmin");
        config.setSslEnabled(0);
        return config;
    }
}
