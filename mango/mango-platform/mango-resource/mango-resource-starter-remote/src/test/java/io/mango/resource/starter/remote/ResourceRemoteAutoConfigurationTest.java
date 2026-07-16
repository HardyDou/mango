package io.mango.resource.starter.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.feign.starter.FeignAutoConfiguration;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import io.mango.resource.support.ResourceTargetDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceRemoteAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
                    FeignAutoConfiguration.class,
                    ResourceRemoteAutoConfiguration.class,
                    ResourceRegistryClientAutoConfiguration.class))
            .withBean(ResourceTargetClient.class, RecordingTargetClient::new)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(io.mango.infra.module.api.ModuleInfoResolver.class, () -> moduleName ->
                    Optional.of(new ModuleInfo(moduleName, "mango-resource-capability-app", "", "/resource", "test")));

    @Test
    void remoteClientConfiguration_exposesDispatcherAndRegistryFeignOnly() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ResourceTargetDispatcher.class);
            assertThat(context).hasSingleBean(ResourceRegistryFeignClient.class);
            assertThat(context).doesNotHaveBean("resourceTargetController");
        });
    }

    @Test
    void localRegistryApi_disablesRegistryFeignButKeepsTargetDispatcher() {
        contextRunner.withBean(ResourceRegistryApi.class, LocalResourceRegistryApi::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceTargetDispatcher.class);
                    assertThat(context).doesNotHaveBean(ResourceRegistryFeignClient.class);
                });
    }

    @Test
    void registryFeignClient_usesModulePathAndRelativeEndpoint() throws Exception {
        assertThat(ResourceRegistryFeignClient.class.getAnnotation(FeignClient.class).path())
                .isEqualTo("/resource");
        Method method = ResourceRegistryFeignClient.class.getMethod(
                "registerDeclarations", RegisterResourceDeclarationsCommand.class);
        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/declarations/register");
    }

    private static class LocalResourceRegistryApi implements ResourceRegistryApi {
        @Override
        public R<Boolean> registerDeclarations(RegisterResourceDeclarationsCommand command) {
            return R.ok(Boolean.TRUE);
        }
    }

    private static class RecordingTargetClient implements ResourceTargetClient {
        @Override
        public R<ResourceBatchResultVO> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(new ResourceBatchResultVO());
        }

        @Override
        public R<ResourceSyncResultVO> disable(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(new ResourceSyncResultVO());
        }

        @Override
        public R<ResourceSyncResultVO> delete(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(new ResourceSyncResultVO());
        }
    }
}
