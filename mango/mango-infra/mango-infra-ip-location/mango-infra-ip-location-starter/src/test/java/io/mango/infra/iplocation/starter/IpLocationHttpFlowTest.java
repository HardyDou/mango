package io.mango.infra.iplocation.starter;

import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = IpLocationHttpFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.ip-location.cache.enabled=false"
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Tag("flow")
@Tag("infra-ip-location")
class IpLocationHttpFlowTest {

    private static final Path XDB_FILE = createXdbFile();

    private final TestRestTemplate restTemplate;

    IpLocationHttpFlowTest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @DynamicPropertySource
    static void xdbLocation(DynamicPropertyRegistry registry) {
        registry.add("mango.ip-location.ip2region.xdb-location", () -> XDB_FILE.toUri().toString());
    }

    @AfterAll
    static void deleteXdbFile() throws IOException {
        Files.deleteIfExists(XDB_FILE);
    }

    @Test
    void shouldResolveThroughRealTomcatHttpBoundary() {
        ResponseEntity<IpLocation> response = restTemplate.getForEntity(
                "/test/ip-location?ip={ip}", IpLocation.class, "8.8.8.8");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIp()).isEqualTo("8.8.8.8");
        assertThat(response.getBody().getCity()).isEqualTo("杭州市");
        assertThat(response.getBody().isPrivateAddress()).isFalse();
        assertThat(response.getBody().isResolved()).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        TestController testController(IpLocationResolver resolver) {
            return new TestController(resolver);
        }
    }

    @RestController
    static class TestController {
        private final IpLocationResolver resolver;

        TestController(IpLocationResolver resolver) {
            this.resolver = resolver;
        }

        @GetMapping("/test/ip-location")
        IpLocation resolve(@RequestParam String ip) {
            return resolver.resolve(ip);
        }
    }

    private static Path createXdbFile() {
        try {
            Path path = Files.createTempFile("mango-ip-location-", ".xdb");
            return Files.write(path, XdbFixture.xdb("中国|0|浙江省|杭州市|电信"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
