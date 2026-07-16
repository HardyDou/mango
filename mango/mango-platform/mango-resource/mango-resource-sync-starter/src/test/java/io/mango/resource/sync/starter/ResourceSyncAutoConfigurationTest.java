package io.mango.resource.sync.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.execution.ResourceTargetExecutor;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceSyncResult;
import io.mango.resource.sync.starter.controller.ResourceTargetController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceSyncAutoConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceSyncAutoConfiguration.class))
            .withBean(ObjectMapper.class, () -> objectMapper)
            .withBean(ResourceHandler.class, RecordingHandler::new);

    @Test
    void autoConfiguration_exposesTargetExecutorAndController() throws Exception {
        ExecuteResourceTargetCommand command = command();

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ResourceTargetExecutor.class);
            assertThat(context).hasSingleBean(ResourceTargetController.class);
            assertThat(context.getBean(ResourceTargetController.class).upsertBatch(command).isSuccess()).isTrue();
        });
    }

    @Test
    void targetController_preservesPublishedHttpPaths() throws Exception {
        assertThat(ResourceTargetController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/resource/targets");
        assertPostPath("upsertBatch", "/upsert-batch");
        assertPostPath("disable", "/disable");
        assertPostPath("delete", "/delete");
    }

    @Test
    void autoConfigurationImports_referenceLoadableClasses() throws Exception {
        String imports = new String(ResourceSyncAutoConfigurationTest.class.getResourceAsStream(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(imports.lines().filter(line -> !line.isBlank()))
                .allSatisfy(className -> assertThatCodeLoads(className.trim()));
    }

    private void assertPostPath(String methodName, String expectedPath) throws Exception {
        Method method = ResourceTargetController.class.getMethod(
                methodName, ExecuteResourceTargetCommand.class);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(expectedPath);
    }

    private void assertThatCodeLoads(String className) {
        try {
            assertThat(Class.forName(className)).isNotNull();
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("自动配置引用了未打包的类: " + className, exception);
        }
    }

    private ExecuteResourceTargetCommand command() throws Exception {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("1");
        declaration.setResourceType("AUTH_MENU");
        String declarations = objectMapper.writeValueAsString(List.of(declaration));
        ExecuteResourceTargetCommand command = new ExecuteResourceTargetCommand();
        command.setDeclarations(declarations);
        command.setCompleteBatch(declarations);
        return command;
    }

    private static class RecordingHandler implements ResourceHandler {

        @Override
        public String resourceType() {
            return "AUTH_MENU";
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            return ResourceSyncResult.of(1L, "authorization_app_module", "ok");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(1L, "authorization_app_module", "disabled");
        }
    }
}
