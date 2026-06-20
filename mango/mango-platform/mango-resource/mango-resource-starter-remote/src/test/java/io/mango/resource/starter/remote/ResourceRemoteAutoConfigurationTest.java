package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.infra.feign.starter.FeignAutoConfiguration;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.api.ResourceTargetDispatcher;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceRemoteAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
                    org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration.class,
                    org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration.class,
                    org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration.class,
                    org.springframework.cloud.loadbalancer.config.LoadBalancerCacheAutoConfiguration.class,
                    FeignAutoConfiguration.class,
                    ResourceRemoteAutoConfiguration.class,
                    ResourceRegistryClientAutoConfiguration.class,
                    ResourceTargetClientAutoConfiguration.class))
            .withBean(ResourceTargetFeignClient.class, RecordingTargetFeignClient::new)
            .withBean(io.mango.infra.module.api.ModuleInfoResolver.class, () -> moduleName ->
                    Optional.of(new ModuleInfo(moduleName, "mango-resource-capability-app", "", "/resource", "test")));

    @Test
    void remoteTargetApp_withHandler_exposesTargetControllerAndRegistryFeign() {
        contextRunner
                .withBean(ResourceHandler.class, TestResourceHandler::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceTargetController.class);
                    assertThat(context).hasSingleBean(ResourceTargetDispatcher.class);
                    assertThat(context).hasSingleBean(ResourceRegistryFeignClient.class);
                });
    }

    @Test
    void resourceRegistryApp_withLocalRegistryApi_keepsTargetDispatcherWithoutRegistryFeign() {
        contextRunner
                .withBean(ResourceRegistryApi.class, LocalResourceRegistryApi::new)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ResourceTargetController.class);
                    assertThat(context).hasSingleBean(ResourceTargetDispatcher.class);
                    assertThat(context).doesNotHaveBean(ResourceRegistryFeignClient.class);
                });
    }

    @Test
    void registryFeignClient_usesFullResourceRegistryPathOnMethod() throws Exception {
        assertThat(ResourceRegistryFeignClient.class.getAnnotation(FeignClient.class).path()).isEmpty();
        Method method = ResourceRegistryFeignClient.class.getMethod(
                "registerDeclarations", RegisterResourceDeclarationsCommand.class);
        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/resource/declarations/register");
    }

    @Test
    void targetFeignClient_usesLoadBalancerAndFullReverseTargetPathOnMethod() throws Exception {
        FeignClient feignClient = ResourceTargetFeignClient.class.getAnnotation(FeignClient.class);
        assertThat(feignClient.path()).isEmpty();
        assertThat(feignClient.url()).isEmpty();

        Method method = ResourceTargetFeignClient.class.getMethod(
                "upsertBatch", URI.class, ExecuteResourceTargetCommand.class);
        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/_resource/targets/upsert-batch");
    }

    private static class TestResourceHandler implements ResourceHandler {

        @Override
        public String resourceType() {
            return "TEST";
        }

        @Override
        public ResourceHandlerSpec spec() {
            return ResourceHandler.super.spec();
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            return ResourceSyncResult.of(1L, "test", "ok");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(1L, "test", "disabled");
        }
    }

    private static class LocalResourceRegistryApi implements ResourceRegistryApi {

        @Override
        public R<Boolean> registerDeclarations(RegisterResourceDeclarationsCommand command) {
            return R.ok(Boolean.TRUE);
        }
    }

    private static class RecordingTargetFeignClient implements ResourceTargetFeignClient {

        @Override
        public R<Map<String, ResourceSyncResult>> upsertBatch(ExecuteResourceTargetCommand command) {
            return R.ok(Map.of());
        }

        @Override
        public R<Map<String, ResourceSyncResult>> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(Map.of());
        }

        @Override
        public R<ResourceSyncResult> disable(ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(1L, "test", "disabled"));
        }

        @Override
        public R<ResourceSyncResult> disable(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(1L, "test", "disabled"));
        }

        @Override
        public R<ResourceSyncResult> delete(ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(1L, "test", "deleted"));
        }

        @Override
        public R<ResourceSyncResult> delete(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(1L, "test", "deleted"));
        }
    }
}
