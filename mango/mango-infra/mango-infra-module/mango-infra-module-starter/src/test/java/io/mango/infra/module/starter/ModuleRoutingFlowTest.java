package io.mango.infra.module.starter;

import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.RequestLine;
import feign.Target;
import feign.codec.StringDecoder;
import io.mango.infra.feign.starter.ModuleTargetFeignInterceptor;
import io.mango.infra.module.api.ModuleInfoResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("flow")
@Tag("infra-module")
class ModuleRoutingFlowTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void configuredDeployment_overridesClasspathAndRoutesRealHttpCall() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/configured/probe", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawPath());
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ModuleAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=127.0.0.1:1",
                        "mango.module.module-service.modules.mango-probe.service-name=127.0.0.1:"
                                + server.getAddress().getPort(),
                        "mango.module.module-service.modules.mango-probe.context-path=/configured",
                        "mango.module.module-service.modules.mango-probe.module-path=/probe")
                .withBean(ModuleMetadataLoader.class, () -> new ModuleMetadataLoader() {
                    @Override
                    public java.util.List<ModuleMetadata> load() {
                        return java.util.List.of(new ModuleMetadata("mango-probe", "/legacy", "classpath"));
                    }
                })
                .run(context -> {
                    ProbeClient client = Feign.builder()
                            .requestInterceptor(new ModuleTargetFeignInterceptor(
                                    context.getBeanProvider(ModuleInfoResolver.class)))
                            .decoder(new StringDecoder())
                            .target(new Target.HardCodedTarget<>(
                                    ProbeClient.class, "mango-probe", "http://127.0.0.1:1"));

                    assertThat(client.probe()).isEqualTo("ok");
                    assertThat(requestPath.get()).isEqualTo("/configured/probe");
                });
    }

    interface ProbeClient {

        @RequestLine("GET /probe")
        String probe();
    }
}
