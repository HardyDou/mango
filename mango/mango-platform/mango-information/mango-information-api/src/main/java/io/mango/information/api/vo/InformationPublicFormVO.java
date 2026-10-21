package io.mango.information.api.vo;

import lombok.Data;

/** 托管表单公开定义。 */
@Data
public class InformationPublicFormVO {
    private String formCode;
    private String formName;
    private String description;
    private String definitionJson;
    private String successMessage;
}
