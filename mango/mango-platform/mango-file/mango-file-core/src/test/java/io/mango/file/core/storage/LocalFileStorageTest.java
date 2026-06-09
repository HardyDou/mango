package io.mango.file.core.storage;

import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileStorageConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @Test
    void publicGetUrl_withoutPublicEndpoint_returnsLocalObjectAccessPath() {
        LocalFileStorage storage = new LocalFileStorage(properties());
        FileStorageConfig config = localConfig();

        String url = storage.publicGetUrl(config, "2026/05/test file.txt", "test file.txt").orElseThrow();

        assertThat(url).isEqualTo("/file/local-objects/local/2026/05/test%20file.txt");
        assertThat(url).doesNotContain("/file/files/download");
    }

    @Test
    void presignedGetUrl_withoutPublicEndpoint_usesLocalObjectAccessPath() {
        LocalFileStorage storage = new LocalFileStorage(properties());
        FileStorageConfig config = localConfig();

        String url = storage.presignedGetUrl(config, "2026/05/test.txt", "test.txt", Duration.ofMinutes(10))
                .orElseThrow();

        assertThat(url).isEqualTo("/file/local-objects/local/2026/05/test.txt");
        assertThat(url).doesNotContain("/file/files/download");
    }

    @Test
    void publicDownloadUrl_withoutPublicEndpoint_usesLocalObjectDownloadPath() {
        LocalFileStorage storage = new LocalFileStorage(properties());
        FileStorageConfig config = localConfig();

        String url = storage.publicDownloadUrl(config, "2026/05/test.txt", "test.txt").orElseThrow();

        assertThat(url).isEqualTo("/file/local-objects/local/2026/05/test.txt?download=1");
        assertThat(url).doesNotContain("/file/files/download");
    }

    @Test
    void publicGetUrl_withPublicEndpoint_usesConfiguredLocalAccessEndpoint() {
        LocalFileStorage storage = new LocalFileStorage(properties());
        FileStorageConfig config = localConfig();
        config.setPublicEndpoint("local-files.example.com/static");
        config.setSslEnabled(1);

        String url = storage.publicGetUrl(config, "2026/05/test file.txt", "test file.txt").orElseThrow();

        assertThat(url).isEqualTo("https://local-files.example.com/static/local/2026/05/test%20file.txt");
    }

    @Test
    void publicDownloadUrl_withPublicEndpoint_keepsConfiguredLocalAccessEndpointAndDownloadFlag() {
        LocalFileStorage storage = new LocalFileStorage(properties());
        FileStorageConfig config = localConfig();
        config.setPublicEndpoint("local-files.example.com/static");
        config.setSslEnabled(1);

        String url = storage.publicDownloadUrl(config, "2026/05/test file.txt", "test file.txt").orElseThrow();

        assertThat(url).isEqualTo("https://local-files.example.com/static/local/2026/05/test%20file.txt?download=1");
    }

    private FileProperties properties() {
        FileProperties properties = new FileProperties();
        properties.getLocal().setPublicPath("/file/local-objects");
        return properties;
    }

    private FileStorageConfig localConfig() {
        FileStorageConfig config = new FileStorageConfig();
        config.setStorageType("LOCAL");
        config.setBucketName("local");
        return config;
    }
}
