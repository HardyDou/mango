package io.mango.common.valid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 身份证校验器。
 *
 * @author Mango
 */
public class IdCardValidator implements ConstraintValidator<IdCard, String> {

    /** 中国大陆身份证号码正则。 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}$|^\\d{17}[\\dXx]$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ID_CARD_PATTERN.matcher(value).matches();
    }
}
