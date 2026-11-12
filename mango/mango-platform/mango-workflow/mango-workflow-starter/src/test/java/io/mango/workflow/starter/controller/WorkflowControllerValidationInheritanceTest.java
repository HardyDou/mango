package io.mango.workflow.starter.controller;

import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.request.WorkflowTaskBusinessPageRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WorkflowControllerValidationInheritanceTest {

    @Test
    void controllerValidationMetadata_inheritsApiConstraintsWithoutConflicts() {
        Set<Class<?>> controllerTypes = controllerTypes("io.mango.workflow.starter.controller");

        assertThat(controllerTypes).isNotEmpty();
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(controllerTypes).allSatisfy(controllerType ->
                    assertThatCode(() -> validatorFactory.getValidator().getConstraintsForClass(controllerType))
                            .as(controllerType.getName())
                            .doesNotThrowAnyException());

            var definitionMetadata = validatorFactory.getValidator()
                    .getConstraintsForClass(WorkflowDefinitionController.class)
                    .getConstraintsForMethod("create", SaveWorkflowDefinitionCommand.class);
            assertThat(definitionMetadata).isNotNull();
            assertThat(definitionMetadata.getParameterDescriptors().getFirst().isCascaded()).isTrue();
        }
    }

    @Test
    void businessTaskPageRequest_allowsMissingBusinessKeys() {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            WorkflowTaskBusinessPageRequest request = new WorkflowTaskBusinessPageRequest();
            request.setBusinessType("BAOHAN_RC_PROJECT_REVIEW");

            assertThat(validator.validate(request)).isEmpty();

            request.setBusinessKeys(List.of());
            assertThat(validator.validate(request)).isEmpty();

            request.setBusinessKeys(List.of("RC1"));
            assertThat(validator.validate(request)).isEmpty();

            request.setBusinessType(null);
            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getMessage())
                    .contains("业务类型不能为空");
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
            throw new IllegalStateException("Workflow Controller class must be loadable: " + className, exception);
        }
    }
}
