package io.mango.gridlayout.starter.controller;

import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GridLayoutPersonalControllerValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void savePersonal_apiOwnedValidation_isInheritedWithoutDeclarationConflict() throws Exception {
        GridLayoutPersonalController controller = new GridLayoutPersonalController(null);
        SaveGridLayoutPersonalCommand command = new SaveGridLayoutPersonalCommand();
        Method method = GridLayoutPersonalController.class.getMethod(
                "savePersonal", SaveGridLayoutPersonalCommand.class);

        Set<ConstraintViolation<GridLayoutPersonalController>> violations = validator
                .forExecutables()
                .validateParameters(controller, method, new Object[]{command});

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("pageCode不能为空", "layoutJson不能为空");
    }

    @Test
    void queryMethods_apiOwnedValidation_isInheritedForGetAndDelete() throws Exception {
        GridLayoutPersonalController controller = new GridLayoutPersonalController(null);
        GridLayoutPersonalQuery query = new GridLayoutPersonalQuery();

        assertThat(validateQuery(controller, "getPersonal", query))
                .extracting(ConstraintViolation::getMessage)
                .contains("pageCode不能为空");
        assertThat(validateQuery(controller, "deletePersonal", query))
                .extracting(ConstraintViolation::getMessage)
                .contains("pageCode不能为空");
    }

    private Set<ConstraintViolation<GridLayoutPersonalController>> validateQuery(
            GridLayoutPersonalController controller,
            String methodName,
            GridLayoutPersonalQuery query) throws Exception {
        Method method = GridLayoutPersonalController.class.getMethod(methodName, GridLayoutPersonalQuery.class);
        return validator.forExecutables().validateParameters(controller, method, new Object[]{query});
    }
}
