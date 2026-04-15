package io.mango.common.valid;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 身份证校验注解。
 *
 * @author Mango
 */
@Documented
@Constraint(validatedBy = IdCardValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdCard {

    /** 默认错误消息。 */
    String message() default "身份证格式不正确";

    /** 校验分组。 */
    Class<?>[] groups() default {};

    /** 负载信息。 */
    Class<? extends Payload>[] payload() default {};
}
