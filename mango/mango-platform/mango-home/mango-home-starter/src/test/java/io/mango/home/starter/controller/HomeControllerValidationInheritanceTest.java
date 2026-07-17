package io.mango.home.starter.controller;

import io.mango.home.api.command.CreateHomePageCommand;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** API 与 Controller 方法校验继承合同测试。 */
class HomeControllerValidationInheritanceTest {

    @Test
    void controllerValidationMetadataInheritsApiConstraintsWithoutConflicts() {
        Set<Class<?>> controllerTypes = controllerTypes("io.mango.home.starter.controller");

        assertThat(controllerTypes).isNotEmpty();
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(controllerTypes).allSatisfy(controllerType ->
                    assertThatCode(() -> validatorFactory.getValidator().getConstraintsForClass(controllerType))
                            .as(controllerType.getName())
                            .doesNotThrowAnyException());

            var createMetadata = validatorFactory.getValidator()
                    .getConstraintsForClass(HomePageController.class)
                    .getConstraintsForMethod("create", CreateHomePageCommand.class);
            assertThat(createMetadata).isNotNull();
            assertThat(createMetadata.getParameterDescriptors().getFirst().isCascaded()).isTrue();
        }
    }

    private static Set<Class<?>> controllerTypes(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents(basePackage).stream()
                .map(definition -> loadClass(definition.getBeanClassName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Home Controller class must be loadable: " + className, exception);
        }
    }
}
