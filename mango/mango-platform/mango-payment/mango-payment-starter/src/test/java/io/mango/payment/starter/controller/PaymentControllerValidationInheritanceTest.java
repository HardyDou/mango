package io.mango.payment.starter.controller;

import io.mango.payment.api.command.CreatePaymentApplicationCommand;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PaymentControllerValidationInheritanceTest {

    @Test
    void controllerValidationMetadata_inheritsApiConstraintsWithoutConflicts() {
        Set<Class<?>> controllerTypes = controllerTypes("io.mango.payment.starter.controller");

        assertThat(controllerTypes).isNotEmpty();
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(controllerTypes).allSatisfy(controllerType ->
                    assertThatCode(() -> validatorFactory.getValidator().getConstraintsForClass(controllerType))
                            .as(controllerType.getName())
                            .doesNotThrowAnyException());

            var applicationMetadata = validatorFactory.getValidator()
                    .getConstraintsForClass(PaymentApplicationController.class)
                    .getConstraintsForMethod("createApplication", CreatePaymentApplicationCommand.class);
            assertThat(applicationMetadata).isNotNull();
            assertThat(applicationMetadata.getParameterDescriptors().getFirst().isCascaded()).isTrue();

            var cashierMetadata = validatorFactory.getValidator()
                    .getConstraintsForClass(PaymentCashierController.class)
                    .getConstraintsForMethod("payResult", String.class);
            assertThat(cashierMetadata).isNotNull();
            assertThat(cashierMetadata.getParameterDescriptors().getFirst().getConstraintDescriptors())
                    .anySatisfy(descriptor -> assertThat(descriptor.getAnnotation().annotationType())
                            .isEqualTo(NotBlank.class));
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
            throw new IllegalStateException("Payment Controller class must be loadable: " + className, exception);
        }
    }
}
