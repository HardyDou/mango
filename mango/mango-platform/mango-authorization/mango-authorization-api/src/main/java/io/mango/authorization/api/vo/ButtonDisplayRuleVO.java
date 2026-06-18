package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Button display rule granted to the current subject.
 */
@Data
public class ButtonDisplayRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String buttonType;
    private String displayRule;
}
